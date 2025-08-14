package com.playdata.messageservice.service;

import com.playdata.messageservice.dto.MessageRequest;
import com.playdata.messageservice.dto.MessageResponse;
import com.playdata.messageservice.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageService {
    @Transactional
    List<MessageResponse> sendMessage(Long senderId, MessageRequest request, MultipartFile[] attachments);

    // 받은 쪽지함 검색/필터/페이징
    Page<MessageResponse> getReceivedMessages(Long receiverId, String searchType, String searchValue, String period, Boolean unreadOnly, Pageable pageable);

    // 보낸 쪽지함 검색/필터/페이징
    Page<MessageResponse> getSentMessages(Long senderId, String searchType, String searchValue, String period, Pageable pageable);

    @Transactional
    MessageResponse readMessage(Long messageId, Long authenticatedEmployeeNo);

    long getUnreadMessageCount(Long receiverId);

    @Transactional
    void deleteMessage(Long messageId, Long employeeNo);

    @Transactional
    void recallMessage(Long messageId, Long senderEmployeeNo);

    List<Long> getEmployeeNosFromSearch(String searchType, String searchValue, String targetType);

    MessageResponse convertToDto(Message message);

    // 기간 필터링을 위한 날짜 범위 계산 유틸리티 메서드
    LocalDateTime[] getDateRange(String period);
}
