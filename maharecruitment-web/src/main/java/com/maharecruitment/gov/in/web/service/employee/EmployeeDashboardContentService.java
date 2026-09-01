package com.maharecruitment.gov.in.web.service.employee;

import java.time.Clock;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;
import com.maharecruitment.gov.in.attendance.service.HolidayService;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeBirthdayWishEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeBirthdayWishRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeProfileRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.projection.EmployeeAnniversaryProjection;
import com.maharecruitment.gov.in.recruitment.repository.projection.EmployeeBirthdayProjection;
import com.maharecruitment.gov.in.web.dto.employee.EmployeeProfileDTO;
import com.maharecruitment.gov.in.web.service.employee.model.EmployeeDashboardContentView;
import com.maharecruitment.gov.in.web.service.employee.model.EmployeeDashboardContentView.AnnouncementView;
import com.maharecruitment.gov.in.web.service.employee.model.EmployeeDashboardContentView.BirthdayWishView;
import com.maharecruitment.gov.in.web.service.employee.model.EmployeeDashboardContentView.CelebrationView;
import com.maharecruitment.gov.in.web.service.employee.model.EmployeeDashboardContentView.UpcomingEventView;

@Service
@Transactional(readOnly = true)
public class EmployeeDashboardContentService {

    static final int UPCOMING_WINDOW_DAYS = 30;
    static final int UPCOMING_EVENT_LIMIT = 8;
    static final int WISH_HISTORY_DAYS = 30;
    static final int WISH_HISTORY_LIMIT = 20;
    private static final LocalDate PLACEHOLDER_DATE_OF_BIRTH = LocalDate.of(1900, 1, 1);
    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter TODAY_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter EVENT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH);
    private static final DateTimeFormatter EVENT_MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final DateTimeFormatter WISH_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH);

    private final EmployeeRepository employeeRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final HolidayService holidayService;
    private final EmployeeBirthdayWishRepository birthdayWishRepository;
    private final Clock clock;

    @Autowired
    public EmployeeDashboardContentService(
            EmployeeRepository employeeRepository,
            EmployeeProfileRepository employeeProfileRepository,
            HolidayService holidayService,
            EmployeeBirthdayWishRepository birthdayWishRepository) {
        this(
                employeeRepository,
                employeeProfileRepository,
                holidayService,
                birthdayWishRepository,
                Clock.system(INDIA_ZONE));
    }

    EmployeeDashboardContentService(
            EmployeeRepository employeeRepository,
            EmployeeProfileRepository employeeProfileRepository,
            HolidayService holidayService,
            EmployeeBirthdayWishRepository birthdayWishRepository,
            Clock clock) {
        this.employeeRepository = employeeRepository;
        this.employeeProfileRepository = employeeProfileRepository;
        this.holidayService = holidayService;
        this.birthdayWishRepository = birthdayWishRepository;
        this.clock = clock;
    }

    public EmployeeDashboardContentView getDashboardContent(EmployeeProfileDTO currentProfile) {
        LocalDate today = LocalDate.now(clock);
        List<EmployeeBirthdayProjection> birthdays = employeeRepository.findActiveEmployeeBirthdays();
        List<EmployeeAnniversaryProjection> anniversaries = employeeProfileRepository.findActiveEmployeeAnniversaries();
        List<HolidayMasterEntity> holidays = holidayService.getHolidaysBetween(
                today.plusDays(1),
                today.plusDays(UPCOMING_WINDOW_DAYS));
        Long currentEmployeeId = currentProfile == null ? null : currentProfile.getEmployeeId();
        List<EmployeeBirthdayWishEntity> sentToday = currentEmployeeId == null
                ? List.of()
                : birthdayWishRepository.findBySender_EmployeeIdAndCelebrationDateOrderByCreatedDateDesc(
                        currentEmployeeId, today);
        Map<Long, EmployeeBirthdayWishEntity> sentWishesByRecipient = sentToday.stream()
                .collect(Collectors.toMap(
                        wish -> wish.getRecipient().getEmployeeId(),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));

        List<CelebrationView> todaysCelebrations = todaysCelebrations(
                today,
                birthdays,
                anniversaries,
                currentEmployeeId,
                sentWishesByRecipient);
        List<UpcomingEventView> upcomingEvents = upcomingEvents(today, holidays);
        List<AnnouncementView> announcements = announcements(currentProfile);
        List<BirthdayWishView> receivedBirthdayWishes = receivedBirthdayWishes(currentEmployeeId, today);
        List<BirthdayWishView> birthdayReplies = birthdayReplies(currentEmployeeId, today);

        return new EmployeeDashboardContentView(
                today,
                today.format(TODAY_FORMATTER),
                todaysCelebrations,
                upcomingEvents,
                announcements,
                receivedBirthdayWishes,
                birthdayReplies);
    }

    private List<CelebrationView> todaysCelebrations(
            LocalDate today,
            List<EmployeeBirthdayProjection> birthdays,
            List<EmployeeAnniversaryProjection> anniversaries,
            Long currentEmployeeId,
            Map<Long, EmployeeBirthdayWishEntity> sentWishesByRecipient) {
        List<CelebrationView> celebrations = new ArrayList<>();

        birthdays.stream()
                .filter(birthday -> isRealDateOfBirth(birthday.getDateOfBirth()))
                .filter(birthday -> occursToday(birthday.getDateOfBirth(), today))
                .map(birthday -> birthdayCelebration(birthday, currentEmployeeId, sentWishesByRecipient))
                .forEach(celebrations::add);

        anniversaries.stream()
                .filter(anniversary -> anniversary.getMarriageDate() != null)
                .filter(anniversary -> occursToday(anniversary.getMarriageDate(), today))
                .map(anniversary -> new CelebrationView(
                        anniversary.getEmployeeId(),
                        displayName(anniversary.getFullName()),
                        "ANNIVERSARY",
                        "Marriage Anniversary",
                        anniversaryMessage(anniversary.getMarriageDate(), today),
                        Objects.equals(currentEmployeeId, anniversary.getEmployeeId()),
                        false,
                        false,
                        null))
                .forEach(celebrations::add);

        return celebrations.stream()
                .sorted(Comparator.comparing(CelebrationView::type)
                        .thenComparing(CelebrationView::employeeName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private CelebrationView birthdayCelebration(
            EmployeeBirthdayProjection birthday,
            Long currentEmployeeId,
            Map<Long, EmployeeBirthdayWishEntity> sentWishesByRecipient) {
        Long birthdayEmployeeId = birthday.getEmployeeId();
        boolean ownCelebration = Objects.equals(currentEmployeeId, birthdayEmployeeId);
        EmployeeBirthdayWishEntity sentWish = sentWishesByRecipient.get(birthdayEmployeeId);
        boolean wishSent = sentWish != null;
        return new CelebrationView(
                birthdayEmployeeId,
                displayName(birthday.getFullName()),
                "BIRTHDAY",
                "Birthday",
                "Wishing you a wonderful year ahead!",
                ownCelebration,
                currentEmployeeId != null && !ownCelebration && !wishSent,
                wishSent,
                wishSent ? sentWish.getReplyMessage() : null);
    }

    private List<UpcomingEventView> upcomingEvents(
            LocalDate today,
            List<HolidayMasterEntity> holidays) {
        LocalDate endDate = today.plusDays(UPCOMING_WINDOW_DAYS);
        return holidays.stream()
                .filter(holiday -> holiday.getHolidayDate() != null)
                .filter(holiday -> holiday.getHolidayDate().isAfter(today)
                        && !holiday.getHolidayDate().isAfter(endDate))
                .map(holiday -> event(holiday.getHolidayName(), holiday.getHolidayDate(), today))
                .sorted(Comparator.comparing(UpcomingEventView::date)
                        .thenComparing(UpcomingEventView::title, String.CASE_INSENSITIVE_ORDER))
                .limit(UPCOMING_EVENT_LIMIT)
                .toList();
    }

    private UpcomingEventView event(
            String title,
            LocalDate date,
            LocalDate today) {
        long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, date);
        String relativeLabel = daysUntil == 1 ? "Tomorrow" : "In " + daysUntil + " days";
        return new UpcomingEventView(
                title,
                date,
                Integer.toString(date.getDayOfMonth()),
                date.format(EVENT_MONTH_FORMATTER).toUpperCase(Locale.ENGLISH),
                date.format(EVENT_DATE_FORMATTER),
                relativeLabel);
    }

    private List<AnnouncementView> announcements(EmployeeProfileDTO currentProfile) {
        List<AnnouncementView> announcements = new ArrayList<>();
        int completion = currentProfile == null ? 0 : currentProfile.getCompletionPercentage();
        if (completion < 100) {
            announcements.add(new AnnouncementView(
                    "Complete your employee profile",
                    "Some profile details are still pending. Keep your employee record current.",
                    "ACTION"));
        }

        return List.copyOf(announcements);
    }

    private List<BirthdayWishView> receivedBirthdayWishes(Long currentEmployeeId, LocalDate today) {
        if (currentEmployeeId == null) {
            return List.of();
        }
        return birthdayWishRepository
                .findByRecipient_EmployeeIdAndCelebrationDateGreaterThanEqualOrderByCreatedDateDesc(
                        currentEmployeeId,
                        today.minusDays(WISH_HISTORY_DAYS),
                        PageRequest.of(0, WISH_HISTORY_LIMIT))
                .stream()
                .map(wish -> new BirthdayWishView(
                        wish.getWishId(),
                        displayName(wish.getSender().getFullName()),
                        wish.getWishMessage(),
                        wish.getReplyMessage(),
                        formatWishTime(wish.getCreatedDate()),
                        formatWishTime(wish.getRepliedDate())))
                .toList();
    }

    private List<BirthdayWishView> birthdayReplies(Long currentEmployeeId, LocalDate today) {
        if (currentEmployeeId == null) {
            return List.of();
        }
        return birthdayWishRepository
                .findRecentRepliesForSender(
                        currentEmployeeId,
                        today.minusDays(WISH_HISTORY_DAYS),
                        today,
                        PageRequest.of(0, WISH_HISTORY_LIMIT))
                .stream()
                .map(wish -> new BirthdayWishView(
                        wish.getWishId(),
                        displayName(wish.getRecipient().getFullName()),
                        wish.getWishMessage(),
                        wish.getReplyMessage(),
                        formatWishTime(wish.getCreatedDate()),
                        formatWishTime(wish.getRepliedDate())))
                .toList();
    }

    private String formatWishTime(java.time.LocalDateTime value) {
        return value == null ? "" : value.format(WISH_TIME_FORMATTER);
    }

    private boolean occursToday(LocalDate date, LocalDate today) {
        return date != null && MonthDay.from(date).equals(MonthDay.from(today));
    }

    private boolean isRealDateOfBirth(LocalDate date) {
        return date != null && !PLACEHOLDER_DATE_OF_BIRTH.equals(date);
    }

    private String anniversaryMessage(LocalDate marriageDate, LocalDate today) {
        int years = Math.max(0, today.getYear() - marriageDate.getYear());
        return years > 0
                ? "Celebrating " + years + (years == 1 ? " year" : " years") + " of togetherness!"
                : "Wishing you joy on this special milestone!";
    }

    private String displayName(String name) {
        return name == null || name.isBlank() ? "MahaIT Colleague" : name.trim();
    }
}
