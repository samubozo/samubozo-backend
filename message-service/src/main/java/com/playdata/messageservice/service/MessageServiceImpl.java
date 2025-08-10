package com.playdata.messageservice.service;

import com.playdata.messageservice.client.HrServiceClient;
import com.playdata.messageservice.client.NotificationServiceClient;
import com.playdata.messageservice.client.NotificationServiceClient.NotificationCreateRequest;
import com.playdata.messageservice.common.auth.TokenUserInfo;
import com.playdata.messageservice.common.configs.AwsS3Config;
import com.playdata.messageservice.dto.AttachmentResponse;
import com.playdata.messageservice.dto.MessageRequest;
import com.playdata.messageservice.dto.MessageResponse;
import com.playdata.messageservice.dto.UserFeignResDto;
import com.playdata.messageservice.entity.Attachment;
import com.playdata.messageservice.entity.Message;
import com.playdata.messageservice.repository.AttachmentRepository;
import com.playdata.messageservice.repository.MessageRepository;
import com.playdata.messageservice.type.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final NotificationServiceClient notificationServiceClient;
    private final AwsS3Config awsS3Config;
    private final HrServiceClient hrServiceClient;
    private final AttachmentRepository attachmentRepository;

    @Transactional
    @Override
    public MessageResponse sendMessage(Long senderId, MessageRequest request, MultipartFile[] attachments) {
        Message.MessageBuilder messageBuilder = Message.builder()
                .senderId(senderId)
                .subject(request.getSubject())
                .content(request.getContent())
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .isNotice(request.getIsNotice() != null && request.getIsNotice());

        if (request.getIsNotice() != null && request.getIsNotice()) {
            messageBuilder.receiverId(null); // 공지사항일 경우 receiverId를 null로 설정
        } else {
            if (request.getReceiverId() == null) {
                throw new IllegalArgumentException("일반 쪽지는 수신자 ID가 필수입니다.");
            }
            messageBuilder.receiverId(request.getReceiverId());
        }

        Message message = messageBuilder.build();

        Message savedMessage = messageRepository.save(message);

        if (attachments != null && attachments.length > 0) {
            Arrays.stream(attachments).forEach(attachmentFile -> {
                if (!attachmentFile.isEmpty()) {
                    try {
                        String originalFilename = attachmentFile.getOriginalFilename();
                        String uniqueFileName = UUID.randomUUID() + "_" + originalFilename;
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

        // 쪽지 발송 시 알림 생성 (공지사항이 아닌 경우에만)
        if ((request.getIsNotice() == null || !request.getIsNotice()) && request.getReceiverId() != null) {
            TokenUserInfo userInfo = getAuthenticatedUserInfo();
            if (userInfo != null) {
                notificationServiceClient.createNotification(
                        String.valueOf(senderId),
                        userInfo.getEmail(), // 수정: getUserEmail() -> getEmail()
                        userInfo.getHrRole(), // 수정: getUserRole() -> getHrRole()
                        NotificationCreateRequest.builder()
                                .employeeNo(String.valueOf(request.getReceiverId()))
                                .type(NotificationType.MESSAGE)
                                .message("새 쪽지가 도착했습니다: " + request.getSubject())
                                .messageId(savedMessage.getMessageId())
                                .build()
                );
            } else {
                log.warn("인증된 사용자 정보가 없어 알림을 생성할 수 없습니다. senderId: {}", senderId);
            }
        }

        return convertToDto(savedMessage);
    }

    @Override
    public Page<MessageResponse> getReceivedMessages(Long receiverId, String searchType, String searchValue, String period, Boolean unreadOnly, Pageable pageable) {
        LocalDateTime[] dateRange = getDateRange(period);
        List<Long> searchEmployeeNos = getEmployeeNosFromSearch(searchType, searchValue, "sender");

        if (searchEmployeeNos != null && searchEmployeeNos.isEmpty()) {
            return Page.empty();
        }

        Page<Message> messages = messageRepository.searchReceivedMessages(receiverId, searchType, searchValue, searchEmployeeNos, dateRange[0], dateRange[1], unreadOnly, pageable);
        return messages.map(this::convertToDto);
    }

    @Override
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
    @Override
    public MessageResponse readMessage(Long messageId, Long authenticatedEmployeeNo) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        // 공지사항이 아니고, 수신자나 발신자가 아니면 접근 불가
        if (!(message.getIsNotice() != null && message.getIsNotice()) && !message.getReceiverId().equals(authenticatedEmployeeNo) && !message.getSenderId().equals(authenticatedEmployeeNo)) {
            throw new IllegalArgumentException("Unauthorized access to message");
        }

        // 수신자가 본인이고 아직 읽지 않은 경우에만 읽음 처리
        if (message.getReceiverId() != null && message.getReceiverId().equals(authenticatedEmployeeNo) && !message.getIsRead()) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
            messageRepository.save(message);

            TokenUserInfo userInfo = getAuthenticatedUserInfo();
            if (userInfo != null) {
                notificationServiceClient.markNotificationAsReadByMessageId(
                        String.valueOf(authenticatedEmployeeNo),
                        userInfo.getEmail(), // 수정: getUserEmail() -> getEmail()
                        userInfo.getHrRole(), // 수정: getUserRole() -> getHrRole()
                        messageId
                );
            } else {
                log.warn("인증된 사용자 정보가 없어 알림을 읽음 처리할 수 없습니다. employeeNo: {}", authenticatedEmployeeNo);
            }
        }
        MessageResponse messageResponse = convertToDto(message);
        HrServiceClient hrServiceClient = this.hrServiceClient; // HR 서비스 클라이언트
        UserFeignResDto senderInfo = hrServiceClient.getUserByEmployeeNo(messageResponse.getSenderId());

        messageResponse.setSenderDepartmentName(senderInfo.getDepartment().getName());
        return messageResponse;
    }

    @Override
    public long getUnreadMessageCount(Long receiverId) {
        return messageRepository.countByReceiverIdAndIsReadFalse(receiverId);
    }

    @Transactional
    @Override
    public void deleteMessage(Long messageId, Long employeeNo) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found with id: " + messageId));

        // 공지사항인 경우 작성자만 삭제 가능
        if (message.getIsNotice() != null && message.getIsNotice()) {
            if (!message.getSenderId().equals(employeeNo)) {
                throw new IllegalArgumentException("공지사항은 작성자만 삭제할 수 있습니다.");
            }
        } else {
            // 일반 쪽지는 수신자 또는 발신자가 삭제 가능
            if (!message.getSenderId().equals(employeeNo) && !message.getReceiverId().equals(employeeNo)) {
                throw new IllegalArgumentException("Unauthorized to delete this message.");
            }
        }

        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            message.getAttachments().forEach(attachment -> {
                try {
                    awsS3Config.deleteFromS3Bucket(attachment.getAttachmentUrl());
                } catch (IOException e) {
                    log.error("Failed to delete attachment from S3: {}", attachment.getAttachmentUrl(), e);
                }
            });
        }

        messageRepository.delete(message);

        TokenUserInfo userInfo = getAuthenticatedUserInfo();
        if (userInfo != null) {
            notificationServiceClient.deleteNotificationsByMessageId(
                    String.valueOf(employeeNo),
                    userInfo.getEmail(), // 수정: getUserEmail() -> getEmail()
                    userInfo.getHrRole(), // 수정: getUserRole() -> getHrRole()
                    messageId
            );
        } else {
            log.warn("인증된 사용자 정보가 없어 알림을 삭제할 수 없습니다. employeeNo: {}", employeeNo);
        }
    }

    @Transactional
    @Override
    public void recallMessage(Long messageId, Long senderEmployeeNo) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found with id: " + messageId));

        if (!message.getSenderId().equals(senderEmployeeNo)) {
            throw new IllegalArgumentException("Only the sender can recall this message.");
        }

        if (message.getIsRead()) {
            throw new IllegalArgumentException("Cannot recall a message that has already been read.");
        }

        deleteMessage(messageId, senderEmployeeNo);
    }

    private TokenUserInfo getAuthenticatedUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof TokenUserInfo) {
            return (TokenUserInfo) authentication.getPrincipal();
        }
        return null;
    }

    @Override
    public List<Long> getEmployeeNosFromSearch(String searchType, String searchValue, String targetType) {
        if (StringUtils.hasText(searchValue) && targetType.equals(searchType)) {
            List<UserFeignResDto> users = hrServiceClient.getUserByUserName(searchValue);
            if (users.isEmpty()) {
                return java.util.Collections.emptyList();
            }
            return users.stream().map(UserFeignResDto::getEmployeeNo).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public MessageResponse convertToDto(Message message) {
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

        UserFeignResDto sender = hrServiceClient.getUserByEmployeeNo(message.getSenderId());
        UserFeignResDto receiver = null;
        if (message.getReceiverId() != null) {
            receiver = hrServiceClient.getUserByEmployeeNo(message.getReceiverId());
        }

        return MessageResponse.builder()
                .messageId(message.getMessageId())
                .senderId(message.getSenderId())
                .senderName(sender != null ? sender.getUserName() : "알 수 없음")
                .receiverId(message.getReceiverId())
                .receiverName(receiver != null ? receiver.getUserName() : "전사공지")
                .subject(message.getSubject())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .isRead(message.getIsRead())
                .readAt(message.getReadAt())
                .isNotice(message.getIsNotice())
                .attachments(attachmentResponses)
                .build();
    }

    @Override
    public LocalDateTime[] getDateRange(String period) {
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
                break;
        }
        return new LocalDateTime[]{startDate, endDate};
    }
}