package com.maharecruitment.gov.in.attendance.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.attendance.entity.WeekOffWorkingDayEntity;
import com.maharecruitment.gov.in.attendance.repository.HolidayRepository;
import com.maharecruitment.gov.in.attendance.repository.WeekOffWorkingDayRepository;
import com.maharecruitment.gov.in.audit.dto.AuditEventView;
import com.maharecruitment.gov.in.audit.dto.AuditRecordRequest;
import com.maharecruitment.gov.in.audit.service.AuditTrailService;
import com.maharecruitment.gov.in.common.service.CurrentActorProvider;

@Service
@Transactional
public class WeekOffWorkingDayServiceImpl implements WeekOffWorkingDayService {

    private static final String AUDIT_MODULE = "ATTENDANCE";
    private static final String AUDIT_ENTITY_TYPE = "WEEK_OFF_WORKING_DAY";

    private final WeekOffWorkingDayRepository weekOffWorkingDayRepository;
    private final HolidayRepository holidayRepository;
    private final AuditTrailService auditTrailService;
    private final CurrentActorProvider currentActorProvider;

    public WeekOffWorkingDayServiceImpl(
            WeekOffWorkingDayRepository weekOffWorkingDayRepository,
            HolidayRepository holidayRepository,
            AuditTrailService auditTrailService,
            CurrentActorProvider currentActorProvider) {
        this.weekOffWorkingDayRepository = weekOffWorkingDayRepository;
        this.holidayRepository = holidayRepository;
        this.auditTrailService = auditTrailService;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeekOffWorkingDayEntity> getWorkingDaysBetween(LocalDate startDate, LocalDate endDate) {
        return weekOffWorkingDayRepository.findByWorkingDateBetween(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<LocalDate> getWorkingDayDatesBetween(LocalDate startDate, LocalDate endDate) {
        return getWorkingDaysBetween(startDate, endDate).stream()
                .map(WeekOffWorkingDayEntity::getWorkingDate)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public WeekOffWorkingDayEntity getWorkingDayById(Long id) {
        return weekOffWorkingDayRepository.findByIdAndActiveTrue(id).orElse(null);
    }

    @Override
    public WeekOffWorkingDayEntity saveWorkingDay(WeekOffWorkingDayEntity workingDay) {
        LocalDate workingDate = requireWorkingDate(workingDay);
        requireWeekend(workingDate);
        requireOfficeOrder(workingDay);
        validateDuplicateDate(workingDate, workingDay.getId());
        validateHolidayConflict(workingDate);

        if (workingDay.getId() == null) {
            WeekOffWorkingDayEntity entity = new WeekOffWorkingDayEntity();
            copyValues(workingDay, entity, workingDate);
            entity.setActive(Boolean.TRUE);
            WeekOffWorkingDayEntity saved = weekOffWorkingDayRepository.save(entity);
            recordCreateAudit(saved);
            return saved;
        }

        WeekOffWorkingDayEntity existing = weekOffWorkingDayRepository.findByIdAndActiveTrue(workingDay.getId())
                .orElseThrow(() -> new IllegalArgumentException("Working-day override record not found."));

        LocalDate previousDate = existing.getWorkingDate();
        String previousOfficeOrderName = existing.getOfficeOrderOriginalName();

        copyValues(workingDay, existing, workingDate);
        WeekOffWorkingDayEntity saved = weekOffWorkingDayRepository.save(existing);
        if (!Objects.equals(previousDate, workingDate)
                || !Objects.equals(previousOfficeOrderName, saved.getOfficeOrderOriginalName())) {
            recordUpdateAudit(saved, previousDate, previousOfficeOrderName);
        }
        return saved;
    }

    @Override
    public void archiveWorkingDay(Long id) {
        WeekOffWorkingDayEntity existing = weekOffWorkingDayRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Working-day override record not found."));
        existing.setActive(Boolean.FALSE);
        WeekOffWorkingDayEntity archived = weekOffWorkingDayRepository.save(existing);
        recordArchiveAudit(archived);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEventView> getWorkingDayAuditTrail(Long id) {
        if (id == null) {
            return List.of();
        }
        return auditTrailService.getTimeline(AUDIT_MODULE, AUDIT_ENTITY_TYPE, id.toString());
    }

    private void copyValues(
            WeekOffWorkingDayEntity source,
            WeekOffWorkingDayEntity target,
            LocalDate workingDate) {
        target.setWorkingDate(workingDate);
        target.setOfficeOrderOriginalName(source.getOfficeOrderOriginalName().trim());
        target.setOfficeOrderStoredName(source.getOfficeOrderStoredName().trim());
        target.setOfficeOrderPath(source.getOfficeOrderPath().trim());
        target.setOfficeOrderContentType(StringUtils.hasText(source.getOfficeOrderContentType())
                ? source.getOfficeOrderContentType().trim()
                : null);
        target.setOfficeOrderFileSize(source.getOfficeOrderFileSize());
    }

    private LocalDate requireWorkingDate(WeekOffWorkingDayEntity workingDay) {
        if (workingDay == null || workingDay.getWorkingDate() == null) {
            throw new IllegalArgumentException("Working day date is required.");
        }
        return workingDay.getWorkingDate();
    }

    private void requireWeekend(LocalDate workingDate) {
        if (!isWeekend(workingDate)) {
            throw new IllegalArgumentException("Only Saturday or Sunday can be converted into a working day.");
        }
    }

    private void requireOfficeOrder(WeekOffWorkingDayEntity workingDay) {
        if (!StringUtils.hasText(workingDay.getOfficeOrderOriginalName())
                || !StringUtils.hasText(workingDay.getOfficeOrderStoredName())
                || !StringUtils.hasText(workingDay.getOfficeOrderPath())) {
            throw new IllegalArgumentException("Office order upload is required.");
        }
    }

    private void validateDuplicateDate(LocalDate workingDate, Long workingDayId) {
        weekOffWorkingDayRepository.findByWorkingDate(workingDate)
                .filter(existing -> !Objects.equals(existing.getId(), workingDayId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "A working-day override is already configured for " + workingDate + ".");
                });
    }

    private void validateHolidayConflict(LocalDate workingDate) {
        holidayRepository.findByHolidayDate(workingDate)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "A holiday is already configured for " + workingDate + ". Archive the holiday before marking it as a working day.");
                });
    }

    private void recordCreateAudit(WeekOffWorkingDayEntity workingDay) {
        auditTrailService.record(AuditRecordRequest.builder()
                .moduleName(AUDIT_MODULE)
                .entityType(AUDIT_ENTITY_TYPE)
                .entityId(workingDay.getId().toString())
                .actionType("CREATE")
                .actorUserId(currentActorProvider.getCurrentUserId())
                .actorLoginId(currentActorProvider.getCurrentActorEmail())
                .activitySummary("Week off converted to working day for " + workingDay.getWorkingDate())
                .activityDetails(buildCreateOrDeleteDetails("created", workingDay))
                .metadata(buildMetadata(workingDay, null, null))
                .build());
    }

    private void recordUpdateAudit(
            WeekOffWorkingDayEntity workingDay,
            LocalDate previousDate,
            String previousOfficeOrderName) {
        auditTrailService.record(AuditRecordRequest.builder()
                .moduleName(AUDIT_MODULE)
                .entityType(AUDIT_ENTITY_TYPE)
                .entityId(workingDay.getId().toString())
                .actionType("UPDATE")
                .actorUserId(currentActorProvider.getCurrentUserId())
                .actorLoginId(currentActorProvider.getCurrentActorEmail())
                .activitySummary("Working-day override updated for " + workingDay.getWorkingDate())
                .activityDetails(buildUpdateDetails(workingDay, previousDate, previousOfficeOrderName))
                .metadata(buildMetadata(workingDay, previousDate, previousOfficeOrderName))
                .build());
    }

    private void recordArchiveAudit(WeekOffWorkingDayEntity workingDay) {
        auditTrailService.record(AuditRecordRequest.builder()
                .moduleName(AUDIT_MODULE)
                .entityType(AUDIT_ENTITY_TYPE)
                .entityId(workingDay.getId().toString())
                .actionType("DEACTIVATE")
                .actorUserId(currentActorProvider.getCurrentUserId())
                .actorLoginId(currentActorProvider.getCurrentActorEmail())
                .activitySummary("Working-day override archived for " + workingDay.getWorkingDate())
                .activityDetails(buildArchiveDetails(workingDay))
                .metadata(buildMetadata(workingDay, null, null))
                .build());
    }

    private String buildCreateOrDeleteDetails(String action, WeekOffWorkingDayEntity workingDay) {
        return "Weekend " + describeWorkingDay(workingDay.getWorkingDate())
                + " was " + action
                + " as a working day using office order \"" + workingDay.getOfficeOrderOriginalName()
                + "\". Internal attendance will no longer treat this date as WEEK_OFF.";
    }

    private String buildArchiveDetails(WeekOffWorkingDayEntity workingDay) {
        return "Working-day override for " + describeWorkingDay(workingDay.getWorkingDate())
                + " was archived. Internal attendance will treat the date as WEEK_OFF again unless a holiday is configured.";
    }

    private String buildUpdateDetails(
            WeekOffWorkingDayEntity workingDay,
            LocalDate previousDate,
            String previousOfficeOrderName) {
        StringBuilder details = new StringBuilder("Working-day override changed from ");
        details.append(describeWorkingDay(previousDate));
        details.append(" to ");
        details.append(describeWorkingDay(workingDay.getWorkingDate()));
        if (!Objects.equals(previousOfficeOrderName, workingDay.getOfficeOrderOriginalName())) {
            details.append(". Office order changed from \"")
                    .append(previousOfficeOrderName)
                    .append("\" to \"")
                    .append(workingDay.getOfficeOrderOriginalName())
                    .append("\"");
        }
        details.append(".");
        return details.toString();
    }

    private String describeWorkingDay(LocalDate workingDate) {
        if (workingDate == null) {
            return "an unselected date";
        }
        String dayName = workingDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return dayName + ", " + workingDate;
    }

    private Map<String, Object> buildMetadata(
            WeekOffWorkingDayEntity workingDay,
            LocalDate previousDate,
            String previousOfficeOrderName) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("workingDayId", workingDay.getId());
        metadata.put("workingDate", workingDay.getWorkingDate());
        metadata.put("dayOfWeek", workingDay.getWorkingDate().getDayOfWeek().toString());
        metadata.put("officeOrderOriginalName", workingDay.getOfficeOrderOriginalName());
        metadata.put("officeOrderContentType", workingDay.getOfficeOrderContentType());
        metadata.put("officeOrderFileSize", workingDay.getOfficeOrderFileSize());
        metadata.put("active", !Boolean.FALSE.equals(workingDay.getActive()));
        metadata.put("weekOffConvertedToWorkingDay", true);
        if (previousDate != null) {
            metadata.put("previousWorkingDate", previousDate);
        }
        if (StringUtils.hasText(previousOfficeOrderName)) {
            metadata.put("previousOfficeOrderOriginalName", previousOfficeOrderName);
        }
        return metadata;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}
