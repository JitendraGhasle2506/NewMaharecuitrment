package com.maharecruitment.gov.in.recruitment.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "employee_birthday_wish",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_employee_birthday_wish_sender_recipient_date",
                columnNames = { "sender_employee_id", "recipient_employee_id", "celebration_date" }),
        indexes = {
                @Index(
                        name = "idx_employee_birthday_wish_recipient_date",
                        columnList = "recipient_employee_id,celebration_date"),
                @Index(
                        name = "idx_employee_birthday_wish_sender_date",
                        columnList = "sender_employee_id,celebration_date")
        })
@Getter
@Setter
@NoArgsConstructor
public class EmployeeBirthdayWishEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wish_id")
    private Long wishId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_employee_id", nullable = false)
    private EmployeeEntity recipient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_employee_id", nullable = false)
    private EmployeeEntity sender;

    @Column(name = "celebration_date", nullable = false)
    private LocalDate celebrationDate;

    @Column(name = "wish_message", nullable = false, length = 300)
    private String wishMessage;

    @Column(name = "reply_message", length = 300)
    private String replyMessage;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "replied_date")
    private LocalDateTime repliedDate;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }
}
