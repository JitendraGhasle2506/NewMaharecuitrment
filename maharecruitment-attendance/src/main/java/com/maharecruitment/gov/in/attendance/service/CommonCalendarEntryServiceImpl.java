package com.maharecruitment.gov.in.attendance.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.attendance.entity.CalendarEntryType;
import com.maharecruitment.gov.in.attendance.dto.CommonCalendarPhotoUpload;
import com.maharecruitment.gov.in.attendance.entity.CommonCalendarEntryEntity;
import com.maharecruitment.gov.in.attendance.entity.CommonCalendarEntryPhotoEntity;
import com.maharecruitment.gov.in.attendance.repository.CommonCalendarEntryRepository;
import com.maharecruitment.gov.in.attendance.repository.CommonCalendarEntryPhotoRepository;
import com.maharecruitment.gov.in.audit.dto.AuditEventView;
import com.maharecruitment.gov.in.audit.dto.AuditRecordRequest;
import com.maharecruitment.gov.in.audit.service.AuditTrailService;
import com.maharecruitment.gov.in.common.service.CurrentActorProvider;

@Service
@Transactional
public class CommonCalendarEntryServiceImpl implements CommonCalendarEntryService {

    private static final String AUDIT_MODULE = "COMMON";
    private static final String AUDIT_ENTITY_TYPE = "CALENDAR_ENTRY";

    private final CommonCalendarEntryRepository repository;
    private final CommonCalendarEntryPhotoRepository photoRepository;
    private final AuditTrailService auditTrailService;
    private final CurrentActorProvider currentActorProvider;

    public CommonCalendarEntryServiceImpl(
            CommonCalendarEntryRepository repository,
            CommonCalendarEntryPhotoRepository photoRepository,
            AuditTrailService auditTrailService,
            CurrentActorProvider currentActorProvider) {
        this.repository = repository;
        this.photoRepository = photoRepository;
        this.auditTrailService = auditTrailService;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommonCalendarEntryEntity> getEntriesBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("A valid calendar date range is required.");
        }
        return repository.findActiveBetween(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public CommonCalendarEntryEntity getEntryById(Long id) {
        return id == null ? null : repository.findByIdAndActiveTrue(id).orElse(null);
    }

    @Override
    public CommonCalendarEntryEntity saveEntry(CommonCalendarEntryEntity entry) {
        return saveEntryWithPhotos(entry, List.of());
    }

    @Override
    public CommonCalendarEntryEntity saveEntryWithPhotos(
            CommonCalendarEntryEntity entry,
            List<CommonCalendarPhotoUpload> photos) {
        LocalDate entryDate = requireDate(entry);
        String title = requireTitle(entry);
        CalendarEntryType entryType = requireInformationType(entry);
        String description = normalizeDescription(entry.getDescription());

        if (repository.existsDuplicate(entryDate, entryType, title, entry.getId())) {
            throw new IllegalArgumentException(
                    "This " + entryType.getDisplayName().toLowerCase() + " is already configured for " + entryDate + ".");
        }

        if (entry.getId() == null) {
            CommonCalendarEntryEntity created = new CommonCalendarEntryEntity();
            copyValues(created, entryDate, title, entryType, description);
            CommonCalendarEntryEntity saved = repository.save(created);
            recordAudit(saved, "CREATE", "created", null);
            appendPhotos(saved, photos);
            return saved;
        }

        CommonCalendarEntryEntity existing = repository.findByIdAndActiveTrue(entry.getId())
                .orElseThrow(() -> new IllegalArgumentException("Calendar entry not found."));
        Map<String, Object> previousValues = snapshot(existing);
        boolean changed = !Objects.equals(existing.getEntryDate(), entryDate)
                || !Objects.equals(existing.getTitle(), title)
                || existing.getEntryType() != entryType
                || !Objects.equals(existing.getDescription(), description);
        copyValues(existing, entryDate, title, entryType, description);
        CommonCalendarEntryEntity saved = repository.save(existing);
        if (changed) {
            recordAudit(saved, "UPDATE", "updated", previousValues);
        }
        appendPhotos(saved, photos);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommonCalendarEntryPhotoEntity> getPhotosForEntries(Collection<Long> entryIds) {
        if (entryIds == null || entryIds.isEmpty()) {
            return List.of();
        }
        return photoRepository.findByCalendarEntryIdInAndActiveTrueOrderByDisplayOrderAscIdAsc(entryIds);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActivePhotos(Long entryId) {
        return entryId == null ? 0 : photoRepository.countByCalendarEntryIdAndActiveTrue(entryId);
    }

    @Override
    public CommonCalendarEntryPhotoEntity archivePhoto(Long entryId, Long photoId) {
        CommonCalendarEntryEntity entry = repository.findByIdAndActiveTrue(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Calendar entry not found."));
        CommonCalendarEntryPhotoEntity photo = photoRepository
                .findByIdAndCalendarEntryIdAndActiveTrue(photoId, entryId)
                .orElseThrow(() -> new IllegalArgumentException("Calendar photo not found."));
        photo.setActive(Boolean.FALSE);
        CommonCalendarEntryPhotoEntity archived = photoRepository.save(photo);
        recordPhotoAudit(entry, archived, "PHOTO_REMOVE", "Photo removed");
        return archived;
    }

    @Override
    public void archiveEntry(Long id) {
        CommonCalendarEntryEntity existing = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Calendar entry not found."));
        existing.setActive(Boolean.FALSE);
        CommonCalendarEntryEntity archived = repository.save(existing);
        recordAudit(archived, "DEACTIVATE", "archived", null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEventView> getEntryAuditTrail(Long id) {
        return id == null
                ? List.of()
                : auditTrailService.getTimeline(AUDIT_MODULE, AUDIT_ENTITY_TYPE, id.toString());
    }

    private void copyValues(
            CommonCalendarEntryEntity target,
            LocalDate date,
            String title,
            CalendarEntryType type,
            String description) {
        target.setEntryDate(date);
        target.setTitle(title);
        target.setEntryType(type);
        target.setDescription(description);
        target.setActive(Boolean.TRUE);
    }

    private void appendPhotos(
            CommonCalendarEntryEntity entry,
            List<CommonCalendarPhotoUpload> photos) {
        if (photos == null || photos.isEmpty()) {
            return;
        }
        int displayOrder = Math.toIntExact(photoRepository.countByCalendarEntryIdAndActiveTrue(entry.getId()));
        List<CommonCalendarEntryPhotoEntity> entities = new ArrayList<>(photos.size());
        for (CommonCalendarPhotoUpload photo : photos) {
            CommonCalendarEntryPhotoEntity entity = new CommonCalendarEntryPhotoEntity();
            entity.setCalendarEntryId(entry.getId());
            entity.setOriginalName(requireText(photo.originalName(), "Photo name is required."));
            entity.setStoredName(requireText(photo.storedName(), "Stored photo name is required."));
            entity.setFilePath(requireText(photo.filePath(), "Stored photo path is required."));
            entity.setContentType(requireText(photo.contentType(), "Photo content type is required."));
            entity.setFileSize(photo.fileSize());
            entity.setDisplayOrder(displayOrder++);
            entity.setActive(Boolean.TRUE);
            entities.add(entity);
        }
        photoRepository.saveAll(entities);
        recordPhotoAudit(entry, null, "PHOTO_ADD", photos.size() + " photo(s) added");
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private LocalDate requireDate(CommonCalendarEntryEntity entry) {
        if (entry == null || entry.getEntryDate() == null) {
            throw new IllegalArgumentException("Date is required.");
        }
        return entry.getEntryDate();
    }

    private String requireTitle(CommonCalendarEntryEntity entry) {
        if (entry == null || !StringUtils.hasText(entry.getTitle())) {
            throw new IllegalArgumentException("Title is required.");
        }
        String title = entry.getTitle().trim();
        if (title.length() > 150) {
            throw new IllegalArgumentException("Title must be 150 characters or less.");
        }
        return title;
    }

    private CalendarEntryType requireInformationType(CommonCalendarEntryEntity entry) {
        CalendarEntryType type = entry == null ? null : entry.getEntryType();
        if (type != CalendarEntryType.FESTIVAL && type != CalendarEntryType.EVENT) {
            throw new IllegalArgumentException("Select Festival or Event for an informational calendar entry.");
        }
        return type;
    }

    private String normalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        String normalized = description.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("Description must be 500 characters or less.");
        }
        return normalized;
    }

    private Map<String, Object> snapshot(CommonCalendarEntryEntity entry) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("entryDate", entry.getEntryDate());
        values.put("title", entry.getTitle());
        values.put("entryType", entry.getEntryType());
        values.put("description", entry.getDescription());
        return values;
    }

    private void recordAudit(
            CommonCalendarEntryEntity entry,
            String actionType,
            String actionLabel,
            Map<String, Object> previousValues) {
        Map<String, Object> metadata = snapshot(entry);
        metadata.put("active", !Boolean.FALSE.equals(entry.getActive()));
        if (previousValues != null) {
            metadata.put("previousValues", previousValues);
        }
        auditTrailService.record(AuditRecordRequest.builder()
                .moduleName(AUDIT_MODULE)
                .entityType(AUDIT_ENTITY_TYPE)
                .entityId(entry.getId().toString())
                .actionType(actionType)
                .actorUserId(currentActorProvider.getCurrentUserId())
                .actorLoginId(currentActorProvider.getCurrentActorEmail())
                .activitySummary(entry.getEntryType().getDisplayName() + " " + actionLabel)
                .activityDetails("\"" + entry.getTitle() + "\" was " + actionLabel + " for " + entry.getEntryDate() + ".")
                .metadata(metadata)
                .build());
    }

    private void recordPhotoAudit(
            CommonCalendarEntryEntity entry,
            CommonCalendarEntryPhotoEntity photo,
            String actionType,
            String summary) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("calendarEntryId", entry.getId());
        metadata.put("entryType", entry.getEntryType());
        if (photo != null) {
            metadata.put("photoId", photo.getId());
            metadata.put("photoName", photo.getOriginalName());
        }
        auditTrailService.record(AuditRecordRequest.builder()
                .moduleName(AUDIT_MODULE)
                .entityType(AUDIT_ENTITY_TYPE)
                .entityId(entry.getId().toString())
                .actionType(actionType)
                .actorUserId(currentActorProvider.getCurrentUserId())
                .actorLoginId(currentActorProvider.getCurrentActorEmail())
                .activitySummary(summary)
                .activityDetails(summary + " for \"" + entry.getTitle() + "\".")
                .metadata(metadata)
                .build());
    }
}
