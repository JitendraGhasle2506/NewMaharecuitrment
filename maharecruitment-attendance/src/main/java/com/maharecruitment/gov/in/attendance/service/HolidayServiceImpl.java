package com.maharecruitment.gov.in.attendance.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.audit.dto.AuditEventView;
import com.maharecruitment.gov.in.audit.dto.AuditRecordRequest;
import com.maharecruitment.gov.in.audit.service.AuditTrailService;
import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;
import com.maharecruitment.gov.in.attendance.repository.HolidayRepository;
import com.maharecruitment.gov.in.common.service.CurrentActorProvider;

@Service
@Transactional
public class HolidayServiceImpl implements HolidayService {

    private static final String AUDIT_MODULE = "ATTENDANCE";
    private static final String AUDIT_ENTITY_TYPE = "HOLIDAY";

    private final HolidayRepository holidayRepository;
    private final AuditTrailService auditTrailService;
    private final CurrentActorProvider currentActorProvider;

    public HolidayServiceImpl(
            HolidayRepository holidayRepository,
            AuditTrailService auditTrailService,
            CurrentActorProvider currentActorProvider) {
        this.holidayRepository = holidayRepository;
        this.auditTrailService = auditTrailService;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    public List<HolidayMasterEntity> getAllHolidays() {
        return holidayRepository.findAllByOrderByHolidayDateAsc();
    }

    @Override
    public List<HolidayMasterEntity> getHolidaysBetween(LocalDate startDate, LocalDate endDate) {
        return holidayRepository.findByHolidayDateBetween(startDate, endDate).stream()
                .sorted((left, right) -> {
                    if (left.getHolidayDate() == null && right.getHolidayDate() == null) {
                        return 0;
                    }
                    if (left.getHolidayDate() == null) {
                        return 1;
                    }
                    if (right.getHolidayDate() == null) {
                        return -1;
                    }
                    return left.getHolidayDate().compareTo(right.getHolidayDate());
                })
                .toList();
    }

    @Override
    public HolidayMasterEntity getHolidayById(Long id) {
        return holidayRepository.findByIdAndActiveTrue(id).orElse(null);
    }

    @Override
    public HolidayMasterEntity saveHoliday(HolidayMasterEntity holiday) {
        LocalDate holidayDate = requireHolidayDate(holiday);
        String holidayName = requireHolidayName(holiday);
        validateDuplicateDate(holidayDate, holiday.getId());

        if (holiday.getId() == null) {
            HolidayMasterEntity entity = new HolidayMasterEntity();
            entity.setHolidayDate(holidayDate);
            entity.setHolidayName(holidayName);
            entity.setActive(Boolean.TRUE);

            HolidayMasterEntity saved = holidayRepository.save(entity);
            recordCreateAudit(saved);
            return saved;
        }

        HolidayMasterEntity existing = holidayRepository.findByIdAndActiveTrue(holiday.getId())
                .orElseThrow(() -> new IllegalArgumentException("Holiday record not found."));

        LocalDate previousDate = existing.getHolidayDate();
        String previousName = existing.getHolidayName();

        existing.setHolidayDate(holidayDate);
        existing.setHolidayName(holidayName);

        HolidayMasterEntity saved = holidayRepository.save(existing);
        if (!Objects.equals(previousDate, holidayDate) || !Objects.equals(previousName, holidayName)) {
            recordUpdateAudit(saved, previousDate, previousName);
        }
        return saved;
    }

    @Override
    public void archiveHoliday(Long id) {
        HolidayMasterEntity existing = holidayRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Holiday record not found."));
        existing.setActive(Boolean.FALSE);
        HolidayMasterEntity archived = holidayRepository.save(existing);
        recordArchiveAudit(archived);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEventView> getHolidayAuditTrail(Long id) {
        if (id == null) {
            return List.of();
        }
        return auditTrailService.getTimeline(AUDIT_MODULE, AUDIT_ENTITY_TYPE, id.toString());
    }

    private LocalDate requireHolidayDate(HolidayMasterEntity holiday) {
        if (holiday == null || holiday.getHolidayDate() == null) {
            throw new IllegalArgumentException("Holiday date is required.");
        }
        return holiday.getHolidayDate();
    }

    private String requireHolidayName(HolidayMasterEntity holiday) {
        String holidayName = holiday == null ? null : holiday.getHolidayName();
        if (!StringUtils.hasText(holidayName)) {
            throw new IllegalArgumentException("Holiday name is required.");
        }
        return holidayName.trim();
    }

    private void validateDuplicateDate(LocalDate holidayDate, Long holidayId) {
        holidayRepository.findByHolidayDate(holidayDate)
                .filter(existing -> !Objects.equals(existing.getId(), holidayId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "A holiday is already configured for " + holidayDate + ".");
                });
    }

    private void recordCreateAudit(HolidayMasterEntity holiday) {
        auditTrailService.record(AuditRecordRequest.builder()
                .moduleName(AUDIT_MODULE)
                .entityType(AUDIT_ENTITY_TYPE)
                .entityId(holiday.getId().toString())
                .actionType("CREATE")
                .actorUserId(currentActorProvider.getCurrentUserId())
                .actorLoginId(currentActorProvider.getCurrentActorEmail())
                .activitySummary("Holiday created for " + holiday.getHolidayDate())
                .activityDetails(buildCreateOrDeleteDetails("created", holiday))
                .metadata(buildHolidayMetadata(holiday, null, null))
                .build());
    }

    private void recordUpdateAudit(
            HolidayMasterEntity holiday,
            LocalDate previousDate,
            String previousName) {
        auditTrailService.record(AuditRecordRequest.builder()
                .moduleName(AUDIT_MODULE)
                .entityType(AUDIT_ENTITY_TYPE)
                .entityId(holiday.getId().toString())
                .actionType("UPDATE")
                .actorUserId(currentActorProvider.getCurrentUserId())
                .actorLoginId(currentActorProvider.getCurrentActorEmail())
                .activitySummary("Holiday updated for " + holiday.getHolidayDate())
                .activityDetails(buildUpdateDetails(holiday, previousDate, previousName))
                .metadata(buildHolidayMetadata(holiday, previousDate, previousName))
                .build());
    }

    private void recordArchiveAudit(HolidayMasterEntity holiday) {
        auditTrailService.record(AuditRecordRequest.builder()
                .moduleName(AUDIT_MODULE)
                .entityType(AUDIT_ENTITY_TYPE)
                .entityId(holiday.getId().toString())
                .actionType("DEACTIVATE")
                .actorUserId(currentActorProvider.getCurrentUserId())
                .actorLoginId(currentActorProvider.getCurrentActorEmail())
                .activitySummary("Holiday archived for " + holiday.getHolidayDate())
                .activityDetails(buildArchiveDetails(holiday))
                .metadata(buildHolidayMetadata(holiday, null, null))
                .build());
    }

    private String buildCreateOrDeleteDetails(String action, HolidayMasterEntity holiday) {
        String dayName = holiday.getHolidayDate().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return "Holiday \"" + holiday.getHolidayName() + "\" was " + action
                + " on " + dayName + ", " + holiday.getHolidayDate()
                + ". Internal attendance will treat this date as HOLIDAY.";
    }

    private String buildArchiveDetails(HolidayMasterEntity holiday) {
        String dayName = holiday.getHolidayDate().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return "Holiday \"" + holiday.getHolidayName() + "\" was archived on "
                + dayName + ", " + holiday.getHolidayDate()
                + ". Internal attendance will no longer treat this date as HOLIDAY.";
    }

    private String buildUpdateDetails(
            HolidayMasterEntity holiday,
            LocalDate previousDate,
            String previousName) {
        return "Holiday changed from "
                + describeHoliday(previousDate, previousName)
                + " to "
                + describeHoliday(holiday.getHolidayDate(), holiday.getHolidayName())
                + ".";
    }

    private String describeHoliday(LocalDate holidayDate, String holidayName) {
        if (holidayDate == null && !StringUtils.hasText(holidayName)) {
            return "an empty value";
        }
        String dayName = holidayDate == null
                ? "Unknown day"
                : holidayDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return "\"" + holidayName + "\" on " + dayName + ", " + holidayDate;
    }

    private Map<String, Object> buildHolidayMetadata(
            HolidayMasterEntity holiday,
            LocalDate previousDate,
            String previousName) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("holidayId", holiday.getId());
        metadata.put("holidayDate", holiday.getHolidayDate());
        metadata.put("holidayName", holiday.getHolidayName());
        metadata.put("active", !Boolean.FALSE.equals(holiday.getActive()));
        metadata.put("dayOfWeek", holiday.getHolidayDate().getDayOfWeek().toString());
        metadata.put("workingDayHoliday", isWorkingDay(holiday.getHolidayDate()));
        if (previousDate != null) {
            metadata.put("previousHolidayDate", previousDate);
        }
        if (StringUtils.hasText(previousName)) {
            metadata.put("previousHolidayName", previousName);
        }
        return metadata;
    }

    private boolean isWorkingDay(LocalDate holidayDate) {
        DayOfWeek dayOfWeek = holidayDate.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }
}
