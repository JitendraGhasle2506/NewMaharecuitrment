package com.maharecruitment.gov.in.web.service.employee;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeBirthdayWishEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeBirthdayWishRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

@Service
public class EmployeeBirthdayWishService {

    static final int MAX_MESSAGE_LENGTH = 300;
    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalDate PLACEHOLDER_DATE_OF_BIRTH = LocalDate.of(1900, 1, 1);

    private final EmployeeBirthdayWishRepository birthdayWishRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public EmployeeBirthdayWishService(
            EmployeeBirthdayWishRepository birthdayWishRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository) {
        this(birthdayWishRepository, employeeRepository, userRepository, Clock.system(INDIA_ZONE));
    }

    EmployeeBirthdayWishService(
            EmployeeBirthdayWishRepository birthdayWishRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            Clock clock) {
        this.birthdayWishRepository = birthdayWishRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public void sendWish(String loginEmail, Long recipientEmployeeId, String message) {
        EmployeeEntity sender = requireCurrentEmployee(loginEmail);
        EmployeeEntity recipient = requireActiveEmployee(recipientEmployeeId);
        LocalDate today = LocalDate.now(clock);

        if (sender.getEmployeeId().equals(recipient.getEmployeeId())) {
            throw new RecruitmentNotificationException("You cannot send a birthday wish to yourself.");
        }
        if (!isBirthdayToday(recipient.getDateOfBirth(), today)) {
            throw new RecruitmentNotificationException("Birthday wishes can only be sent on the employee's birthday.");
        }
        if (birthdayWishRepository.existsBySender_EmployeeIdAndRecipient_EmployeeIdAndCelebrationDate(
                sender.getEmployeeId(), recipient.getEmployeeId(), today)) {
            throw new RecruitmentNotificationException("You have already sent a birthday wish to this employee today.");
        }

        EmployeeBirthdayWishEntity wish = new EmployeeBirthdayWishEntity();
        wish.setSender(sender);
        wish.setRecipient(recipient);
        wish.setCelebrationDate(today);
        wish.setWishMessage(requireMessage(message, "Birthday wish"));

        try {
            birthdayWishRepository.saveAndFlush(wish);
        } catch (DataIntegrityViolationException ex) {
            throw new RecruitmentNotificationException("You have already sent a birthday wish to this employee today.");
        }
    }

    @Transactional
    public void replyToWish(String loginEmail, Long wishId, String reply) {
        EmployeeEntity recipient = requireCurrentEmployee(loginEmail);
        EmployeeBirthdayWishEntity wish = birthdayWishRepository
                .findForReply(wishId, recipient.getEmployeeId())
                .orElseThrow(() -> new RecruitmentNotificationException("Birthday wish was not found."));

        wish.setReplyMessage(requireMessage(reply, "Reply"));
        wish.setRepliedDate(LocalDateTime.now(clock));
        birthdayWishRepository.save(wish);
    }

    private EmployeeEntity requireCurrentEmployee(String loginEmail) {
        if (!StringUtils.hasText(loginEmail)) {
            throw new RecruitmentNotificationException("Logged-in employee is required.");
        }
        return userRepository.findByEmailIgnoreCaseAndActiveTrue(loginEmail.trim())
                .flatMap(user -> employeeRepository.findByUser_IdAndStatusIgnoreCase(user.getId(), "ACTIVE"))
                .orElseThrow(() -> new RecruitmentNotificationException("Active employee profile was not found."));
    }

    private EmployeeEntity requireActiveEmployee(Long employeeId) {
        if (employeeId == null) {
            throw new RecruitmentNotificationException("Birthday employee is required.");
        }
        return employeeRepository.findById(employeeId)
                .filter(employee -> "ACTIVE".equalsIgnoreCase(employee.getStatus()))
                .orElseThrow(() -> new RecruitmentNotificationException("Birthday employee was not found."));
    }

    private boolean isBirthdayToday(LocalDate dateOfBirth, LocalDate today) {
        return dateOfBirth != null
                && !PLACEHOLDER_DATE_OF_BIRTH.equals(dateOfBirth)
                && MonthDay.from(dateOfBirth).equals(MonthDay.from(today));
    }

    private String requireMessage(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new RecruitmentNotificationException(label + " is required.");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            throw new RecruitmentNotificationException(label + " must not exceed " + MAX_MESSAGE_LENGTH + " characters.");
        }
        return normalized;
    }
}
