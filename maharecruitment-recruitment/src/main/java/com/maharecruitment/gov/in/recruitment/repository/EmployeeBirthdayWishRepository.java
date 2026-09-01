package com.maharecruitment.gov.in.recruitment.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeBirthdayWishEntity;

@Repository
public interface EmployeeBirthdayWishRepository extends JpaRepository<EmployeeBirthdayWishEntity, Long> {

    boolean existsBySender_EmployeeIdAndRecipient_EmployeeIdAndCelebrationDate(
            Long senderEmployeeId,
            Long recipientEmployeeId,
            LocalDate celebrationDate);

    @EntityGraph(attributePaths = { "sender", "recipient" })
    List<EmployeeBirthdayWishEntity> findBySender_EmployeeIdAndCelebrationDateOrderByCreatedDateDesc(
            Long senderEmployeeId,
            LocalDate celebrationDate);

    @EntityGraph(attributePaths = { "sender", "recipient" })
    List<EmployeeBirthdayWishEntity> findByRecipient_EmployeeIdAndCelebrationDateGreaterThanEqualOrderByCreatedDateDesc(
            Long recipientEmployeeId,
            LocalDate fromDate,
            Pageable pageable);

    @EntityGraph(attributePaths = { "sender", "recipient" })
    @Query("""
            select wish
            from EmployeeBirthdayWishEntity wish
            where wish.sender.employeeId = :senderEmployeeId
              and wish.celebrationDate >= :fromDate
              and wish.celebrationDate < :beforeDate
              and wish.replyMessage is not null
              and trim(wish.replyMessage) <> ''
            order by wish.repliedDate desc, wish.createdDate desc
            """)
    List<EmployeeBirthdayWishEntity> findRecentRepliesForSender(
            @Param("senderEmployeeId") Long senderEmployeeId,
            @Param("fromDate") LocalDate fromDate,
            @Param("beforeDate") LocalDate beforeDate,
            Pageable pageable);

    @EntityGraph(attributePaths = { "sender", "recipient" })
    @Query("""
            select wish
            from EmployeeBirthdayWishEntity wish
            where wish.wishId = :wishId
              and wish.recipient.employeeId = :recipientEmployeeId
            """)
    Optional<EmployeeBirthdayWishEntity> findForReply(
            @Param("wishId") Long wishId,
            @Param("recipientEmployeeId") Long recipientEmployeeId);
}
