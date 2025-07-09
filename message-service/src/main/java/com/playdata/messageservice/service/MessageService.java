package com.playdata.messageservice.service;

import com.playdata.messageservice.dto.MessageRequest;
import com.playdata.messageservice.dto.MessageResponse;
import com.playdata.messageservice.entity.Message;
import com.playdata.messageservice.repository.MessageRepository;
import com.playdata.messageservice.common.configs.AwsS3Config;
import com.playdata.messageservice.type.NotificationType; // NotificationType import 추가
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final NotificationService notificationService; // 알림 서비스 주입
    private final AwsS3Config awsS3Config;

    @Transactional
    public MessageResponse sendMessage(Long senderId, MessageRequest request, MultipartFile attachment) {
        String attachmentUrl = null;
        if (attachment != null && !attachment.isEmpty()) {
            try {
                String originalFilename = attachment.getOriginalFilename();
                String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFilename;
                attachmentUrl = awsS3Config.uploadToS3Bucket(attachment.getBytes(), uniqueFileName);
            } catch (IOException e) {
                log.error("Failed to upload attachment to S3", e);
                // 파일 업로드 실패 시 예외 처리 또는 null로 진행
            }
        }

        Message message = Message.builder()
                .senderId(senderId)
                .receiverId(request.getReceiverId())
                .subject(request.getSubject())
                .content(request.getContent())
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .attachmentUrl(attachmentUrl) // 첨부 파일 URL 저장
                .build();

        Message savedMessage = messageRepository.save(message);

        // 쪽지 발송 시 알림 생성
        notificationService.createNotification(
                String.valueOf(request.getReceiverId()), // employeeNo는 String
                NotificationType.MESSAGE, // "NEW_MESSAGE" -> NotificationType.MESSAGE로 변경
                "새 쪽지가 도착했습니다: " + request.getSubject(),
                savedMessage.getMessageId()
        );

        return convertToDto(savedMessage);
    }

    public List<MessageResponse> getReceivedMessages(Long receiverId) {
        List<Message> messages = messageRepository.findByReceiverIdOrderBySentAtDesc(receiverId);
        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<MessageResponse> getSentMessages(Long senderId) {
        List<Message> messages = messageRepository.findBySenderIdOrderBySentAtDesc(senderId);
        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse readMessage(Long messageId, Long authenticatedEmployeeNo) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        // 메시지 수신자 또는 발신자가 맞는지 확인
        if (!message.getReceiverId().equals(authenticatedEmployeeNo) && !message.getSenderId().equals(authenticatedEmployeeNo)) {
            throw new IllegalArgumentException("Unauthorized access to message");
        }

        if (!message.getIsRead()) {
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
        if (message.getAttachmentUrl() != null && !message.getAttachmentUrl().isEmpty()) {
            try {
                awsS3Config.deleteFromS3Bucket(message.getAttachmentUrl());
            } catch (IOException e) {
                log.error("Failed to delete attachment from S3: {}", message.getAttachmentUrl(), e);
                // 파일 삭제 실패 시에도 메시지 삭제는 진행
            }
        }

        messageRepository.delete(message);
        // 알림 서비스에서 해당 쪽지 관련 알림도 삭제 (필요하다면)
        // notificationService.deleteNotificationsByMessageId(messageId);
    }

    @Transactional
    public MessageResponse modifyMessage(Long messageId, Long senderId, MessageRequest request, MultipartFile attachment) {
        Message existingMessage = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found with id: " + messageId));

        // 메시지 보낸 사람만 수정 가능하도록 검증
        if (!existingMessage.getSenderId().equals(senderId)) {
            throw new IllegalArgumentException("Unauthorized to modify this message.");
        }

        // 기존 첨부 파일 삭제
        if (existingMessage.getAttachmentUrl() != null && !existingMessage.getAttachmentUrl().isEmpty()) {
            try {
                awsS3Config.deleteFromS3Bucket(existingMessage.getAttachmentUrl());
            } catch (IOException e) {
                log.error("Failed to delete old attachment from S3: {}", existingMessage.getAttachmentUrl(), e);
                // 파일 삭제 실패 시에도 메시지 수정은 진행
            }
        }

        // 기존 메시지 삭제 (새로운 메시지로 대체)
        messageRepository.delete(existingMessage);

        // 새로운 메시지 생성 (sendMessage 로직 재활용)
        return sendMessage(senderId, request, attachment);
    }

    private MessageResponse convertToDto(Message message) {
        return MessageResponse.builder()
                .messageId(message.getMessageId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .subject(message.getSubject())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .isRead(message.getIsRead())
                .readAt(message.getReadAt())
                .attachmentUrl(message.getAttachmentUrl())
                .build();
    }
}
