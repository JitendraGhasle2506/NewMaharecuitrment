package com.maharecruitment.gov.in.web.service.attendance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.attendance.dto.CommonCalendarEntryForm;
import com.maharecruitment.gov.in.attendance.dto.CalendarDateMode;
import com.maharecruitment.gov.in.attendance.dto.CommonCalendarPhotoUpload;
import com.maharecruitment.gov.in.attendance.entity.CalendarEntryType;
import com.maharecruitment.gov.in.attendance.entity.CommonCalendarEntryEntity;
import com.maharecruitment.gov.in.attendance.entity.CommonCalendarEntryPhotoEntity;
import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;
import com.maharecruitment.gov.in.attendance.service.CommonCalendarEntryService;
import com.maharecruitment.gov.in.attendance.service.HolidayService;
import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;

@Service
public class CommonCalendarEntryPageService {

    public static final int MAX_PHOTOS_PER_ENTRY = 10;
    private static final String PHOTO_UPLOAD_MODULE = "common/calendar-entry-photo";
    private static final String HOLIDAY_SOURCE = "HOLIDAY";
    private static final String COMMON_SOURCE = "COMMON";

    private final HolidayService holidayService;
    private final CommonCalendarEntryService calendarEntryService;
    private final FileStorageService fileStorageService;

    public CommonCalendarEntryPageService(
            HolidayService holidayService,
            CommonCalendarEntryService calendarEntryService,
            FileStorageService fileStorageService) {
        this.holidayService = holidayService;
        this.calendarEntryService = calendarEntryService;
        this.fileStorageService = fileStorageService;
    }

    public boolean save(CommonCalendarEntryForm form) {
        boolean editing = form.getId() != null;
        List<MultipartFile> photos = nonEmptyPhotos(form.getPhotos());
        if (form.getEntryType() == CalendarEntryType.HOLIDAY) {
            requireSource(form, HOLIDAY_SOURCE);
            if (form.getDateMode() == CalendarDateMode.RANGE) {
                throw new IllegalArgumentException("Attendance holidays must use a single date.");
            }
            if (!photos.isEmpty()) {
                throw new IllegalArgumentException("Photos can be added only to festivals and events.");
            }
            HolidayMasterEntity holiday = new HolidayMasterEntity();
            holiday.setId(form.getId());
            holiday.setHolidayDate(form.getEntryDate());
            holiday.setHolidayName(form.getTitle());
            holidayService.saveHoliday(holiday);
            return !editing;
        }

        requireSource(form, COMMON_SOURCE);
        LocalDate endDate = resolveEndDate(form);
        validatePhotoLimit(form.getId(), photos.size());
        List<FileUploadResult> uploadedFiles = storePhotos(photos);
        try {
            CommonCalendarEntryEntity entry = new CommonCalendarEntryEntity();
            entry.setId(form.getId());
            entry.setEntryDate(form.getEntryDate());
            entry.setEndDate(endDate);
            entry.setTitle(form.getTitle());
            entry.setEntryType(form.getEntryType());
            entry.setDescription(form.getDescription());
            calendarEntryService.saveEntryWithPhotos(entry, toPhotoUploads(uploadedFiles));
            return !editing;
        } catch (RuntimeException ex) {
            uploadedFiles.forEach(file -> fileStorageService.deleteQuietly(file.fullPath()));
            throw ex;
        }
    }

    public void removePhoto(Long entryId, Long photoId) {
        CommonCalendarEntryPhotoEntity photo = calendarEntryService.archivePhoto(entryId, photoId);
        fileStorageService.deleteQuietly(photo.getFilePath());
    }

    private void requireSource(CommonCalendarEntryForm form, String expectedSource) {
        if (form.getId() == null) {
            form.setSource(expectedSource);
            return;
        }
        if (!expectedSource.equals(form.getSource())) {
            throw new IllegalArgumentException("The calendar entry source is invalid for the selected type.");
        }
    }

    private LocalDate resolveEndDate(CommonCalendarEntryForm form) {
        if (form.getDateMode() != CalendarDateMode.RANGE) {
            return null;
        }
        if (form.getEndDate() == null) {
            throw new IllegalArgumentException("To date is required for a date range.");
        }
        if (form.getEntryDate() != null && form.getEndDate().isBefore(form.getEntryDate())) {
            throw new IllegalArgumentException("To date must be on or after the from date.");
        }
        return form.getEndDate();
    }

    private void validatePhotoLimit(Long entryId, int newPhotoCount) {
        if (newPhotoCount == 0) {
            return;
        }
        long existingCount = calendarEntryService.countActivePhotos(entryId);
        if (existingCount + newPhotoCount > MAX_PHOTOS_PER_ENTRY) {
            throw new IllegalArgumentException(
                    "A festival or event can contain a maximum of " + MAX_PHOTOS_PER_ENTRY + " photos.");
        }
    }

    private List<FileUploadResult> storePhotos(List<MultipartFile> photos) {
        List<FileUploadResult> uploadedFiles = new ArrayList<>(photos.size());
        try {
            for (MultipartFile photo : photos) {
                uploadedFiles.add(fileStorageService.store(photo, PHOTO_UPLOAD_MODULE));
            }
            return uploadedFiles;
        } catch (RuntimeException ex) {
            uploadedFiles.forEach(file -> fileStorageService.deleteQuietly(file.fullPath()));
            throw ex;
        }
    }

    private List<CommonCalendarPhotoUpload> toPhotoUploads(List<FileUploadResult> files) {
        return files.stream()
                .map(file -> new CommonCalendarPhotoUpload(
                        file.originalFileName(),
                        file.storedFileName(),
                        file.fullPath(),
                        file.contentType(),
                        file.size()))
                .toList();
    }

    private List<MultipartFile> nonEmptyPhotos(List<MultipartFile> photos) {
        return photos == null
                ? List.of()
                : photos.stream().filter(photo -> photo != null && !photo.isEmpty()).toList();
    }
}
