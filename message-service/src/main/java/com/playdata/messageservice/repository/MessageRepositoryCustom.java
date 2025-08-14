package com.playdata.messageservice.repository;

import com.playdata.messageservice.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepositoryCustom {
    Page<Message> searchReceivedMessages(Long receiverId, String searchType, String searchValue, List<Long> employeeNosForSearch, LocalDateTime startDate, LocalDateTime endDate, Boolean unreadOnly, Pageable pageable);
    Page<Message> searchSentMessages(Long senderId, String searchType, String searchValue, List<Long> employeeNosForSearch, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}