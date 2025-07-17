package com.playdata.messageservice.controller;

import com.playdata.messageservice.dto.MessageRequest;
import com.playdata.messageservice.dto.MessageResponse;
import com.playdata.messageservice.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.playdata.messageservice.common.auth.TokenUserInfo;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    // 쪽지 보내기
    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo,
            @RequestPart("request") MessageRequest request,
            @RequestPart(value = "attachments", required = false) MultipartFile[] attachments) {
        // 실제 senderId는 인증된 사용자 정보에서 가져옴
        Long senderId = tokenUserInfo.getEmployeeNo();
        MessageResponse response = messageService.sendMessage(senderId, request, attachments);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 받은 쪽지함 검색/필터/페이징
    @GetMapping("/received")
    public ResponseEntity<Page<MessageResponse>> getReceivedMessages(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo,
            @RequestParam(required = false, defaultValue = "all") String period,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) Boolean unreadOnly,
            @PageableDefault(sort = "sentAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Long receiverId = tokenUserInfo.getEmployeeNo();
        Page<MessageResponse> messages = messageService.getReceivedMessages(receiverId, searchType, searchValue, period, unreadOnly, pageable);
        return ResponseEntity.ok(messages);
    }

    // 보낸 쪽지함 검색/필터/페이징
    @GetMapping("/sent")
    public ResponseEntity<Page<MessageResponse>> getSentMessages(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo,
            @RequestParam(required = false, defaultValue = "all") String period,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String searchValue,
            @PageableDefault(sort = "sentAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Long senderId = tokenUserInfo.getEmployeeNo();
        Page<MessageResponse> messages = messageService.getSentMessages(senderId, searchType, searchValue, period, pageable);
        return ResponseEntity.ok(messages);
    }

    // 쪽지 읽기 (읽음 처리 포함)
    @GetMapping("/{messageId}")
    public ResponseEntity<MessageResponse> readMessage(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo,
            @PathVariable Long messageId) {
        Long receiverId = tokenUserInfo.getEmployeeNo();
        MessageResponse response = messageService.readMessage(messageId, receiverId);
        return ResponseEntity.ok(response);
    }

    // 읽지 않은 쪽지 개수 조회
    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadMessageCount(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
        Long receiverId = tokenUserInfo.getEmployeeNo();
        long count = messageService.getUnreadMessageCount(receiverId);
        return ResponseEntity.ok(count);
    }

    // 쪽지 삭제
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo,
            @PathVariable Long messageId) {
        Long employeeNo = tokenUserInfo.getEmployeeNo(); // 삭제 요청자 (보낸 사람 또는 받은 사람)
        messageService.deleteMessage(messageId, employeeNo);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    

    // 쪽지 발신 취소 (읽지 않은 쪽지만 삭제 가능)
    @DeleteMapping("/{messageId}/recall")
    public ResponseEntity<Void> recallMessage(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo,
            @PathVariable Long messageId) {
        Long senderEmployeeNo = tokenUserInfo.getEmployeeNo();
        messageService.recallMessage(messageId, senderEmployeeNo);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}