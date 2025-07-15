package com.playdata.messageservice.service;

import com.playdata.messageservice.client.HrServiceClient;
import com.playdata.messageservice.dto.MessageRequest;
import com.playdata.messageservice.dto.MessageResponse;
import com.playdata.messageservice.dto.AttachmentResponse;
import com.playdata.messageservice.dto.UserFeignResDto;
import com.playdata.messageservice.entity.Message;
import com.playdata.messageservice.repository.MessageRepository;
import com.playdata.messageservice.common.configs.AwsS3Config;
import com.playdata.messageservice.type.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.playdata.messageservice.entity.Attachment;
import com.playdata.messageservice.repository.AttachmentRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final NotificationService notificationService;
    private final AwsS3Config awsS3Config;
    private final HrServiceClient hrServiceClient;
    private final AttachmentRepository attachmentRepository;

    @Transactional
    public MessageResponse sendMessage(Long senderId, MessageRequest request, MultipartFile[] attachments) {
        Message message = Message.builder()
                .senderId(senderId)
                .receiverId(request.getReceiverId())
                .subject(request.getSubject())
                .content(request.getContent())
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();

        Message savedMessage = messageRepository.save(message);

        if (attachments != null && attachments.length > 0) {
            Arrays.stream(attachments).forEach(attachmentFile -> {
                if (!attachmentFile.isEmpty()) {
                    try {
                        String originalFilename = attachmentFile.getOriginalFilename();
                        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFilename;
                        String attachmentUrl = awsS3Config.uploadToS3Bucket(attachmentFile.getBytes(), uniqueFileName);

                        Attachment attachment = Attachment.builder()
                                .message(savedMessage)
                                .attachmentUrl(attachmentUrl)
                                .originalFileName(originalFilename)
                                .build();
                        attachmentRepository.save(attachment);
                        savedMessage.addAttachment(attachment);
                    } catch (IOException e) {
                        log.error("Failed to upload attachment to S3", e);
                        // 파일 업로드 실패 시 예외 처리 또는 null로 진행
                    }
                }
            });
        }

        // 쪽지 발송 시 알림 생성
        notificationService.createNotification(
                String.valueOf(request.getReceiverId()), // employeeNo는 String
                NotificationType.MESSAGE, // "NEW_MESSAGE" -> NotificationType.MESSAGE로 변경
                "새 쪽지가 도착했습니다: " + request.getSubject(),
                savedMessage.getMessageId()
        );

        return convertToDto(savedMessage);
    }

    // 받은 쪽지함 검색/필터/페이징
    public Page<MessageResponse> getReceivedMessages(Long receiverId, String searchType, String searchValue, String period, Boolean unreadOnly, Pageable pageable) {
        LocalDateTime[] dateRange = getDateRange(period);
        List<Long> searchEmployeeNos = getEmployeeNosFromSearch(searchType, searchValue, "sender");

        if (searchEmployeeNos != null && searchEmployeeNos.isEmpty()) {
            return Page.empty();
        }

        Page<Message> messages = messageRepository.searchReceivedMessages(receiverId, searchType, searchValue, searchEmployeeNos, dateRange[0], dateRange[1], unreadOnly, pageable);
        return messages.map(this::convertToDto);
    }

    // 보낸 쪽지함 검색/필터/페이징
    public Page<MessageResponse> getSentMessages(Long senderId, String searchType, String searchValue, String period, Pageable pageable) {
        LocalDateTime[] dateRange = getDateRange(period);
        List<Long> searchEmployeeNos = getEmployeeNosFromSearch(searchType, searchValue, "receiver");

        if (searchEmployeeNos != null && searchEmployeeNos.isEmpty()) {
            return Page.empty();
        }

        Page<Message> messages = messageRepository.searchSentMessages(senderId, searchType, searchValue, searchEmployeeNos, dateRange[0], dateRange[1], pageable);
        return messages.map(this::convertToDto);
    }

    @Transactional
    public MessageResponse readMessage(Long messageId, Long authenticatedEmployeeNo) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        // 메시지 수신자 또는 발신자가 맞는지 확인
        if (!message.getReceiverId().equals(authenticatedEmployeeNo) && !message.getSenderId().equals(authenticatedEmployeeNo)) {
            throw new IllegalArgumentException("Unauthorized access to message");
        }

        // 현재 로그인한 사용자가 쪽지의 수신자이고, 아직 읽지 않은 경우에만 읽음 처리
        if (message.getReceiverId().equals(authenticatedEmployeeNo) && !message.getIsRead()) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
            messageRepository.save(message);

            // 알림 서비스에서 해당 쪽지 관련 알림을 읽음 처리
            notificationService.markNotificationAsReadByMessageId(messageId);
        }
        return convertToDto(message);
    }

    public long getUnreadMessageCount(Long receiverId) {
        return messageRepository.countByReceiverIdAndIsReadFalse(receiverId);
    }

    @Transactional
    public void deleteMessage(Long messageId, Long employeeNo) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found with id: " + messageId));

        // 메시지를 보낸 사람 또는 받은 사람만 삭제 가능하도록 검증
        if (!message.getSenderId().equals(employeeNo) && !message.getReceiverId().equals(employeeNo)) {
            throw new IllegalArgumentException("Unauthorized to delete this message.");
        }

        // S3에 첨부된 파일이 있다면 삭제
        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            message.getAttachments().forEach(attachment -> {
                try {
                    awsS3Config.deleteFromS3Bucket(attachment.getAttachmentUrl());
                } catch (IOException e) {
                    log.error("Failed to delete attachment from S3: {}", attachment.getAttachmentUrl(), e);
                    // 파일 삭제 실패 시에도 메시지 삭제는 진행
                }
            });
        }

        messageRepository.delete(message);
        // 알림 서비스에서 해당 쪽지 관련 알림도 삭제
        notificationService.deleteNotificationsByMessageId(messageId);
    }

    @Transactional
    public void recallMessage(Long messageId, Long senderEmployeeNo) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found with id: " + messageId));

        // 발신자만 발신 취소 가능
        if (!message.getSenderId().equals(senderEmployeeNo)) {
            throw new IllegalArgumentException("Only the sender can recall this message.");
        }

        // 쪽지가 읽지 않은 상태인지 확인
        if (message.getIsRead()) {
            throw new IllegalArgumentException("Cannot recall a message that has already been read.");
        }

        // 첨부파일 및 알림 삭제는 deleteMessage 로직을 재활용
        deleteMessage(messageId, senderEmployeeNo);
    }

    

    private List<Long> getEmployeeNosFromSearch(String searchType, String searchValue, String targetType) {
        if (StringUtils.hasText(searchValue) && targetType.equals(searchType)) {
            List<UserFeignResDto> users = hrServiceClient.getUserByUserName(searchValue);
            if (users.isEmpty()) {
                return java.util.Collections.emptyList();
            }
            return users.stream().map(UserFeignResDto::getEmployeeNo).collect(Collectors.toList());
        }
        return null;
    }

    private MessageResponse convertToDto(Message message) {
        List<AttachmentResponse> attachmentResponses;
        if (message.getAttachments() != null) {
            attachmentResponses = message.getAttachments().stream()
                    .map(attachment -> AttachmentResponse.builder()
                            .attachmentId(attachment.getAttachmentId())
                            .attachmentUrl(attachment.getAttachmentUrl())
                            .originalFileName(attachment.getOriginalFileName())
                            .build())
                    .collect(Collectors.toList());
        } else {
            attachmentResponses = Collections.emptyList();
        }

        return MessageResponse.builder()
                .messageId(message.getMessageId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .subject(message.getSubject())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .isRead(message.getIsRead())
                .readAt(message.getReadAt())
                .attachments(attachmentResponses)
                .build();
    }

    // 기간 필터링을 위한 날짜 범위 계산 유틸리티 메서드
    private LocalDateTime[] getDateRange(String period) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = null;

        switch (period) {
            case "1w":
                startDate = endDate.minusWeeks(1);
                break;
            case "1m":
                startDate = endDate.minusMonths(1);
                break;
            case "3m":
                startDate = endDate.minusMonths(3);
                break;
            case "all":
            default:
                // startDate는 null로 유지하여 전체 기간 조회
                break;
        }
        return new LocalDateTime[]{startDate, endDate};
    }
}
