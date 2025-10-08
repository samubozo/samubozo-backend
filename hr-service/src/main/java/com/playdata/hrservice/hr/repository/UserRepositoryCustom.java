package com.playdata.hrservice.hr.repository;

import com.playdata.hrservice.hr.dto.UserListResDto;
import com.playdata.hrservice.hr.dto.UserResDto;
import com.playdata.hrservice.hr.dto.UserSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepositoryCustom {
    /**
     * 검색 조건에 따라 사용자를 동적으로 조회하고 페이징 처리합니다.
     *
     * @param condition 검색 조건 (userName, departmentName, hrRole)
     * @param pageable  페이징 정보
     * @return 페이징 처리된 사용자 정보
     */
    Page<UserListResDto> searchUsers(UserSearchCondition condition, Pageable pageable);
}
