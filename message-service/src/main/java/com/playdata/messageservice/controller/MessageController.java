package com.playdata.messageservice.controller;

import com.playdata.messageservice.dto.MessageRequest;
import com.playdata.messageservice.dto.MessageResponse;
import com.playdata.messageservice.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.playdata.messageservice.common.auth.TokenUserInfo; // TokenUserInfo 경로 확인

import java.util.List;

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
            @RequestPart(value = "attachment", required = false) MultipartFile attachment) {
        // 실제 senderId는 인증된 사용자 정보에서 가져옴
        Long senderId = tokenUserInfo.getEmployeeNo();
        MessageResponse response = messageService.sendMessage(senderId, request, attachment);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 받은 쪽지함 조회
    @GetMapping("/received")
    public ResponseEntity<List<MessageResponse>> getReceivedMessages(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
        Long receiverId = tokenUserInfo.getEmployeeNo();
        List<MessageResponse> messages = messageService.getReceivedMessages(receiverId);
        return ResponseEntity.ok(messages);
    }

    // 보낸 쪽지함 조회
    @GetMapping("/sent")
    public ResponseEntity<List<MessageResponse>> getSentMessages(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
        Long senderId = tokenUserInfo.getEmployeeNo();
        List<MessageResponse> messages = messageService.getSentMessages(senderId);
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

    // 쪽지 수정 (삭제 후 재등록 방식)
    @PutMapping(value = "/{messageId}", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<MessageResponse> modifyMessage(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo,
            @PathVariable Long messageId,
            @RequestPart("request") MessageRequest request,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment) {
        Long senderId = tokenUserInfo.getEmployeeNo(); // 수정 요청자 (보낸 사람)
        MessageResponse response = messageService.modifyMessage(messageId, senderId, request, attachment);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}