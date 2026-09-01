package com.maharecruitment.gov.in.web.service.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;
import com.maharecruitment.gov.in.attendance.service.HolidayService;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeBirthdayWishRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeProfileRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.projection.EmployeeAnniversaryProjection;
import com.maharecruitment.gov.in.recruitment.repository.projection.EmployeeBirthdayProjection;
import com.maharecruitment.gov.in.web.dto.employee.EmployeeProfileDTO;

class EmployeeDashboardContentServiceTest {

    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 1);

    @Test
    void springSelectsTheProductionConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(EmployeeRepository.class, () -> mock(EmployeeRepository.class));
            context.registerBean(EmployeeProfileRepository.class, () -> mock(EmployeeProfileRepository.class));
            context.registerBean(HolidayService.class, () -> mock(HolidayService.class));
            context.registerBean(
                    EmployeeBirthdayWishRepository.class,
                    () -> mock(EmployeeBirthdayWishRepository.class));
            context.register(EmployeeDashboardContentService.class);

            context.refresh();

            assertThat(context.getBean(EmployeeDashboardContentService.class)).isNotNull();
        }
    }

    @Test
    void buildsTodayCelebrationsUpcomingEventsAndActionableAnnouncements() {
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        EmployeeProfileRepository profileRepository = mock(EmployeeProfileRepository.class);
        HolidayService holidayService = mock(HolidayService.class);
        EmployeeBirthdayWishRepository birthdayWishRepository = mock(EmployeeBirthdayWishRepository.class);

        List<EmployeeBirthdayProjection> birthdays = List.of(
                birthday("Anita Patil", LocalDate.of(1992, 9, 1)),
                birthday("Rahul More", LocalDate.of(1990, 9, 2)),
                birthday("Placeholder Employee", LocalDate.of(1900, 1, 1)));
        List<EmployeeAnniversaryProjection> anniversaries = List.of(
                anniversary("Vikram Shah", LocalDate.of(2020, 9, 1)),
                anniversary("Meera Joshi", LocalDate.of(2022, 9, 10)));
        when(employeeRepository.findActiveEmployeeBirthdays()).thenReturn(birthdays);
        when(profileRepository.findActiveEmployeeAnniversaries()).thenReturn(anniversaries);
        when(holidayService.getHolidaysBetween(TODAY.plusDays(1), TODAY.plusDays(30)))
                .thenReturn(List.of(holiday("MahaIT Foundation Day", TODAY.plusDays(5))));

        EmployeeProfileDTO currentProfile = new EmployeeProfileDTO();
        currentProfile.setEmployeeId(99L);
        currentProfile.setCompletionPercentage(75);
        EmployeeDashboardContentService service = new EmployeeDashboardContentService(
                employeeRepository,
                profileRepository,
                holidayService,
                birthdayWishRepository,
                fixedClock(TODAY));

        var content = service.getDashboardContent(currentProfile);

        assertThat(content.today()).isEqualTo(TODAY);
        assertThat(content.todayLabel()).isEqualTo("Tuesday, 1 September 2026");
        assertThat(content.todaysCelebrations())
                .extracting(celebration -> celebration.employeeName() + ":" + celebration.type())
                .containsExactly("Vikram Shah:ANNIVERSARY", "Anita Patil:BIRTHDAY");
        assertThat(content.todaysCelebrations())
                .filteredOn(celebration -> celebration.type().equals("BIRTHDAY"))
                .singleElement()
                .satisfies(celebration -> {
                    assertThat(celebration.canWish()).isTrue();
                    assertThat(celebration.wishSent()).isFalse();
                    assertThat(celebration.ownCelebration()).isFalse();
                });
        assertThat(content.upcomingEvents())
                .extracting(event -> event.title())
                .containsExactly("MahaIT Foundation Day");
        assertThat(content.announcements())
                .extracting(announcement -> announcement.title())
                .containsExactly("Complete your employee profile");
    }

    @Test
    void returnsCleanEmptyStatesWhenNoCelebrationsEventsOrAnnouncementsApply() {
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        EmployeeProfileRepository profileRepository = mock(EmployeeProfileRepository.class);
        HolidayService holidayService = mock(HolidayService.class);
        EmployeeBirthdayWishRepository birthdayWishRepository = mock(EmployeeBirthdayWishRepository.class);
        when(employeeRepository.findActiveEmployeeBirthdays()).thenReturn(List.of());
        when(profileRepository.findActiveEmployeeAnniversaries()).thenReturn(List.of());
        when(holidayService.getHolidaysBetween(TODAY.plusDays(1), TODAY.plusDays(30)))
                .thenReturn(List.of());

        EmployeeProfileDTO currentProfile = new EmployeeProfileDTO();
        currentProfile.setCompletionPercentage(100);
        EmployeeDashboardContentService service = new EmployeeDashboardContentService(
                employeeRepository,
                profileRepository,
                holidayService,
                birthdayWishRepository,
                fixedClock(TODAY));

        var content = service.getDashboardContent(currentProfile);

        assertThat(content.todaysCelebrations()).isEmpty();
        assertThat(content.upcomingEvents()).isEmpty();
        assertThat(content.announcements()).isEmpty();
    }

    private EmployeeBirthdayProjection birthday(String name, LocalDate dateOfBirth) {
        EmployeeBirthdayProjection projection = mock(EmployeeBirthdayProjection.class);
        when(projection.getEmployeeId()).thenReturn((long) Math.abs(name.hashCode()));
        when(projection.getFullName()).thenReturn(name);
        when(projection.getDateOfBirth()).thenReturn(dateOfBirth);
        return projection;
    }

    private EmployeeAnniversaryProjection anniversary(String name, LocalDate marriageDate) {
        EmployeeAnniversaryProjection projection = mock(EmployeeAnniversaryProjection.class);
        when(projection.getFullName()).thenReturn(name);
        when(projection.getMarriageDate()).thenReturn(marriageDate);
        return projection;
    }

    private HolidayMasterEntity holiday(String name, LocalDate date) {
        HolidayMasterEntity holiday = new HolidayMasterEntity();
        holiday.setHolidayName(name);
        holiday.setHolidayDate(date);
        return holiday;
    }

    private Clock fixedClock(LocalDate date) {
        return Clock.fixed(date.atStartOfDay(INDIA_ZONE).toInstant(), INDIA_ZONE);
    }
}
