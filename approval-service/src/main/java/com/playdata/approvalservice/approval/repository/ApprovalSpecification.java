package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.entity.ApprovalRequest;
import com.playdata.approvalservice.approval.entity.ApprovalStatus;
import com.playdata.approvalservice.approval.entity.RequestType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ApprovalSpecification {

    public static Specification<ApprovalRequest> withFilter(Long applicantId, String status, String requestType) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (applicantId != null) {
                predicates.add(criteriaBuilder.equal(root.get("applicantId"), applicantId));
            }

            if (status != null && !status.isEmpty()) {
                if ("PENDING".equalsIgnoreCase(status)) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), ApprovalStatus.PENDING));
                } else if ("PROCESSED".equalsIgnoreCase(status)) {
                    predicates.add(root.get("status").in(ApprovalStatus.APPROVED, ApprovalStatus.REJECTED));
                }
            }

            if (requestType != null && !requestType.isEmpty()) {
                try {
                    RequestType rt = RequestType.valueOf(requestType.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("requestType"), rt));
                } catch (IllegalArgumentException e) {
                    // 유효하지 않은 requestType 문자열인 경우, 에러를 던지거나 무시할 수 있습니다.
                    // 여기서는 검색 조건에서 제외하도록 무시합니다.
                    query.where(criteriaBuilder.disjunction()); // 항상 false인 조건을 추가하여 결과를 반환하지 않도록 함
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
