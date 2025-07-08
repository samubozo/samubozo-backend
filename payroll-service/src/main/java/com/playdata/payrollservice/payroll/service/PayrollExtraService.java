package com.playdata.payrollservice.payroll.service;

import com.playdata.payrollservice.payroll.dto.PayrollExtraDetailDto;
import com.playdata.payrollservice.payroll.dto.PayrollExtraRequestDto;
import com.playdata.payrollservice.payroll.entity.PayrollExtra;
import com.playdata.payrollservice.payroll.repository.PayrollExtraRepository;
import com.playdata.payrollservice.payroll.repository.PayrollRepository;
import com.playdata.payrollservice.payroll.repository.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PayrollExtraService {

    private final PayrollExtraRepository payrollExtraRepository;
    private final UserServiceClient userServiceClient;

    // 추가 수당 저장
    public PayrollExtra saveExtra(PayrollExtra extra) {
        return payrollExtraRepository.save(extra);
    }

    // 추가 수당 조회
    public PayrollExtra getExtraById(Long id) {
        return payrollExtraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 수당 내역이 없습니다."));
    }

    // 유저 정보와 함께
    public PayrollExtraDetailDto getExtraWithUser(Long id) {
        PayrollExtra extra = getExtraById(id);

        // ❗ 나중에 실제 연결되면 이 부분 사용
        // UserResDto user = userServiceClient.getUserById(extra.getUserId());

        // TODO: 임시 더미 데이터
        return new PayrollExtraDetailDto(
                extra.getExtraId(),
                extra.getUserId(),
                "강해린",  // user.getName()
                "hrin123@naver.com",      // user.getEmail()
                extra.getAmount(),
                extra.getDescription(),
                extra.getDateGiven()
        );
    }

    // 수당 수정
    public PayrollExtra updateExtra(Long id, PayrollExtraRequestDto requestDto) {
        // DB에서 기존 데이터 가져옴
        PayrollExtra existing = getExtraById(id); // 없으면 예외 발생

        // 기존 데이터에 새 값 덮어씌우기
        existing.setAmount(requestDto.getAmount());
        existing.setDescription(requestDto.getDescription());
        existing.setDateGiven(requestDto.getDateGiven());

        return payrollExtraRepository.save(existing); // 다시 저장
    }

    // 수당 삭제
    public void deleteExtra(Long id) {
        PayrollExtra existing = getExtraById(id);
        payrollExtraRepository.delete(existing);
    }

}
