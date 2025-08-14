package com.playdata.messageservice.repository;

import com.playdata.messageservice.entity.Message;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class MessageRepositoryImpl implements MessageRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Message> searchReceivedMessages(Long receiverId, String searchType, String searchValue, List<Long> searchEmployeeNos, LocalDateTime startDate, LocalDateTime endDate, Boolean unreadOnly, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Message> cq = cb.createQuery(Message.class);
        Root<Message> message = cq.from(Message.class);

        // 받은 편지함 또는 공지사항
        Predicate receiverPredicate = cb.equal(message.get("receiverId"), receiverId);
        Predicate noticePredicate = cb.isTrue(message.get("isNotice"));
        Predicate mainPredicate = cb.or(receiverPredicate, noticePredicate);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(mainPredicate);

        if (searchEmployeeNos != null && !searchEmployeeNos.isEmpty()) {
            if ("sender".equals(searchType)) {
                predicates.add(message.get("senderId").in(searchEmployeeNos));
            }
        } else if (StringUtils.hasText(searchType) && "title".equals(searchType) && StringUtils.hasText(searchValue)) {
            predicates.add(cb.like(message.get("subject"), "%" + searchValue + "%"));
        }

        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(message.get("sentAt"), startDate));
        }
        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(message.get("sentAt"), endDate));
        }

        if (unreadOnly != null && unreadOnly) {
            predicates.add(cb.isFalse(message.get("isRead")));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(message.get("sentAt"))); // 최신순 정렬

        List<Message> result = entityManager.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // count 쿼리 수정
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<Message> countMessage = countCq.from(Message.class);

        // 받은 편지함 또는 공지사항 (count 쿼리에도 동일하게 적용)
        Predicate countReceiverPredicate = cb.equal(countMessage.get("receiverId"), receiverId);
        Predicate countNoticePredicate = cb.isTrue(countMessage.get("isNotice"));
        Predicate countMainPredicate = cb.or(countReceiverPredicate, countNoticePredicate);

        List<Predicate> countPredicates = new ArrayList<>();
        countPredicates.add(countMainPredicate);

        if (searchEmployeeNos != null && !searchEmployeeNos.isEmpty()) {
            if ("sender".equals(searchType)) {
                countPredicates.add(countMessage.get("senderId").in(searchEmployeeNos));
            }
        } else if (StringUtils.hasText(searchType) && "title".equals(searchType) && StringUtils.hasText(searchValue)) {
            countPredicates.add(cb.like(countMessage.get("subject"), "%" + searchValue + "%"));
        }
        if (startDate != null) {
            countPredicates.add(cb.greaterThanOrEqualTo(countMessage.get("sentAt"), startDate));
        }
        if (endDate != null) {
            countPredicates.add(cb.lessThanOrEqualTo(countMessage.get("sentAt"), endDate));
        }
        if (unreadOnly != null && unreadOnly) {
            countPredicates.add(cb.isFalse(countMessage.get("isRead")));
        }
        countCq.select(cb.count(countMessage)).where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countCq).getSingleResult();

        return new PageImpl<>(result, pageable, total);
    }

    @Override
    public Page<Message> searchSentMessages(Long senderId, String searchType, String searchValue, List<Long> searchEmployeeNos, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Message> cq = cb.createQuery(Message.class);
        Root<Message> message = cq.from(Message.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(message.get("senderId"), senderId));

        if (searchEmployeeNos != null && !searchEmployeeNos.isEmpty()) {
            if ("receiver".equals(searchType)) {
                predicates.add(message.get("receiverId").in(searchEmployeeNos));
            }
        } else if (StringUtils.hasText(searchType) && "title".equals(searchType) && StringUtils.hasText(searchValue)) {
            predicates.add(cb.like(message.get("subject"), "%" + searchValue + "%"));
        }

        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(message.get("sentAt"), startDate));
        }
        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(message.get("sentAt"), endDate));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(message.get("sentAt"))); // 최신순 정렬

        List<Message> result = entityManager.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // count 쿼리 수정
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<Message> countMessage = countCq.from(Message.class);
        List<Predicate> countPredicates = new ArrayList<>();
        countPredicates.add(cb.equal(countMessage.get("senderId"), senderId));

        if (searchEmployeeNos != null && !searchEmployeeNos.isEmpty()) {
            if ("receiver".equals(searchType)) {
                countPredicates.add(countMessage.get("receiverId").in(searchEmployeeNos));
            }
        } else if (StringUtils.hasText(searchType) && "title".equals(searchType) && StringUtils.hasText(searchValue)) {
            countPredicates.add(cb.like(countMessage.get("subject"), "%" + searchValue + "%"));
        }
        if (startDate != null) {
            countPredicates.add(cb.greaterThanOrEqualTo(countMessage.get("sentAt"), startDate));
        }
        if (endDate != null) {
            countPredicates.add(cb.lessThanOrEqualTo(countMessage.get("sentAt"), endDate));
        }
        countCq.select(cb.count(countMessage)).where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countCq).getSingleResult();

        return new PageImpl<>(result, pageable, total);
    }
}