package com.playdata.hrservice.hr.repository;

import com.playdata.hrservice.hr.dto.*;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.playdata.hrservice.hr.entity.QDepartment.department;
import static com.playdata.hrservice.hr.entity.QPosition.position;
import static com.playdata.hrservice.hr.entity.QUser.user;

// @RequiredArgsConstructor를 사용하지 않고, 직접 생성자를 구현하여 JPAQueryFactory를 주입받습니다.
public class UserRepositoryCustomImpl implements UserRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    public UserRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<UserListResDto> searchUsers(UserSearchCondition condition, Pageable pageable) {
        // 데이터 조회를 위한 쿼리
        List<UserListResDto> content = queryFactory
                // [수정] Projections.constructor -> new QUserListResDto(...)
                // 이 코드는 컴파일 시점에 타입과 순서를 모두 체크해주므로 매우 안전합니다.
                .select(new QUserListResDto(
                        user.employeeNo,
                        user.userName,
                        user.position.positionName,
                        user.department.departmentId,
                        user.department.name,
                        user.hireDate,
                        user.phone,
                        user.email,
                        // [수정] Address 객체가 아닌, DB에서 조합된 문자열을 전달합니다.
                        Expressions.stringTemplate("CONCAT({0}, ' ', {1})", user.address.city, user.address.street),
                        user.activate
                ))
                .from(user)
                .leftJoin(user.department, department)
                .leftJoin(user.position, position)
                .where(
                        userNameContains(condition.getUserName()),
                        departmentNameContains(condition.getDepartmentName()),
                        activateEq(condition.getActivate()) // [추가] 재직 상태 조건
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(user.employeeNo.desc())
                .fetch();

        // 전체 카운트를 위한 쿼리 (최적화를 위해 분리 )
        JPAQuery<Long> countQuery = queryFactory
                .select(user.count())
                .from(user)
                .leftJoin(user.department, department)
                .where(
                        userNameContains(condition.getUserName()),
                        departmentNameContains(condition.getDepartmentName()),
                        activateEq(condition.getActivate()) // [추가] 재직 상태 조건

                );

        // Page 객체로 변환하여 반환
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // 아래 메소드들이 바로 '동적 쿼리'의 핵심입니다.
    // 검색 조건이 null이거나 비어있으면, where 절에서 무시됩니다.
    private BooleanExpression userNameContains(String userName) {
        return StringUtils.hasText(userName) ? user.userName.contains(userName) : null;
    }

    private BooleanExpression departmentNameContains(String departmentName) {
        return StringUtils.hasText(departmentName) ? department.name.contains(departmentName) : null;
    }

    /**
     * 재직 상태(activate)가 파라미터로 넘어온 경우에만 where 조건에 추가합니다.
     *
     * @param activate "Y" 또는 null
     * @return BooleanExpression (조건문) 또는 null
     */
    private BooleanExpression activateEq(String activate) {
        return StringUtils.hasText(activate) ? user.activate.eq(activate) : null;
    }

}
