package com.maharecruitment.gov.in.web.service.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeBirthdayWishEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeBirthdayWishRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

class EmployeeBirthdayWishServiceTest {

    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 1);
    private static final String LOGIN_EMAIL = "sender@mahait.org";

    private EmployeeBirthdayWishRepository birthdayWishRepository;
    private EmployeeRepository employeeRepository;
    private UserRepository userRepository;
    private EmployeeBirthdayWishService service;
    private EmployeeEntity sender;

    @BeforeEach
    void setUp() {
        birthdayWishRepository = mock(EmployeeBirthdayWishRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        userRepository = mock(UserRepository.class);
        service = new EmployeeBirthdayWishService(
                birthdayWishRepository,
                employeeRepository,
                userRepository,
                fixedClock());

        User user = new User();
        user.setId(10L);
        sender = employee(100L, "Sender Employee", LocalDate.of(1990, 2, 3));
        when(userRepository.findByEmailIgnoreCaseAndActiveTrue(LOGIN_EMAIL)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUser_IdAndStatusIgnoreCase(10L, "ACTIVE")).thenReturn(Optional.of(sender));
    }

    @Test
    void sendsTrimmedWishOnlyToEmployeeWhoseBirthdayIsToday() {
        EmployeeEntity recipient = employee(200L, "Birthday Employee", LocalDate.of(1994, 9, 1));
        when(employeeRepository.findById(200L)).thenReturn(Optional.of(recipient));

        service.sendWish(LOGIN_EMAIL, 200L, "  Happy birthday! Have a wonderful year.  ");

        ArgumentCaptor<EmployeeBirthdayWishEntity> captor = ArgumentCaptor.forClass(EmployeeBirthdayWishEntity.class);
        verify(birthdayWishRepository).saveAndFlush(captor.capture());
        EmployeeBirthdayWishEntity savedWish = captor.getValue();
        assertThat(savedWish.getSender()).isSameAs(sender);
        assertThat(savedWish.getRecipient()).isSameAs(recipient);
        assertThat(savedWish.getCelebrationDate()).isEqualTo(TODAY);
        assertThat(savedWish.getWishMessage()).isEqualTo("Happy birthday! Have a wonderful year.");
    }

    @Test
    void preventsDuplicateBirthdayWish() {
        EmployeeEntity recipient = employee(200L, "Birthday Employee", LocalDate.of(1994, 9, 1));
        when(employeeRepository.findById(200L)).thenReturn(Optional.of(recipient));
        when(birthdayWishRepository.existsBySender_EmployeeIdAndRecipient_EmployeeIdAndCelebrationDate(
                100L, 200L, TODAY)).thenReturn(true);

        assertThatThrownBy(() -> service.sendWish(LOGIN_EMAIL, 200L, "Happy birthday!"))
                .isInstanceOf(RecruitmentNotificationException.class)
                .hasMessage("You have already sent a birthday wish to this employee today.");
        verify(birthdayWishRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsWishWhenItIsNotTheRecipientsBirthday() {
        EmployeeEntity recipient = employee(200L, "Another Employee", LocalDate.of(1994, 9, 2));
        when(employeeRepository.findById(200L)).thenReturn(Optional.of(recipient));

        assertThatThrownBy(() -> service.sendWish(LOGIN_EMAIL, 200L, "Happy birthday!"))
                .isInstanceOf(RecruitmentNotificationException.class)
                .hasMessage("Birthday wishes can only be sent on the employee's birthday.");
        verify(birthdayWishRepository, never()).saveAndFlush(any());
    }

    @Test
    void letsOnlyTheRecipientSaveAReply() {
        EmployeeBirthdayWishEntity wish = new EmployeeBirthdayWishEntity();
        wish.setWishId(500L);
        wish.setRecipient(sender);
        when(birthdayWishRepository.findForReply(500L, 100L)).thenReturn(Optional.of(wish));

        service.replyToWish(LOGIN_EMAIL, 500L, "  Thank you so much!  ");

        assertThat(wish.getReplyMessage()).isEqualTo("Thank you so much!");
        assertThat(wish.getRepliedDate()).isEqualTo(LocalDateTime.of(2026, 9, 1, 0, 0));
        verify(birthdayWishRepository).save(wish);
    }

    @Test
    void rejectsReplyWhenWishDoesNotBelongToCurrentEmployee() {
        when(birthdayWishRepository.findForReply(500L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.replyToWish(LOGIN_EMAIL, 500L, "Thank you!"))
                .isInstanceOf(RecruitmentNotificationException.class)
                .hasMessage("Birthday wish was not found.");
        verify(birthdayWishRepository, never()).save(any());
    }

    private EmployeeEntity employee(Long id, String name, LocalDate dateOfBirth) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(id);
        employee.setFullName(name);
        employee.setDateOfBirth(dateOfBirth);
        employee.setStatus("ACTIVE");
        return employee;
    }

    private Clock fixedClock() {
        return Clock.fixed(TODAY.atStartOfDay(INDIA_ZONE).toInstant(), INDIA_ZONE);
    }
}
