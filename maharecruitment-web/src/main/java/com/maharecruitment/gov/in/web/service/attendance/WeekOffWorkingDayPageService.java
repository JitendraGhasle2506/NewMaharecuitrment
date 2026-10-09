package com.maharecruitment.gov.in.web.service.attendance;

import java.time.DayOfWeek;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.attendance.dto.WeekOffWorkingDayForm;
import com.maharecruitment.gov.in.attendance.entity.WeekOffWorkingDayEntity;
import com.maharecruitment.gov.in.attendance.service.WeekOffWorkingDayService;
import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;

@Service
public class WeekOffWorkingDayPageService {

    private static final String UPLOAD_MODULE = "attendance/week-off-working-day-office-order";

    private final WeekOffWorkingDayService workingDayService;
    private final FileStorageService fileStorageService;

    public WeekOffWorkingDayPageService(
            WeekOffWorkingDayService workingDayService,
            FileStorageService fileStorageService) {
        this.workingDayService = workingDayService;
        this.fileStorageService = fileStorageService;
    }

    public boolean save(WeekOffWorkingDayForm form) {
        validateDate(form);
        WeekOffWorkingDayEntity existing = form.getId() == null
                ? null
                : workingDayService.getWorkingDayById(form.getId());
        if (form.getId() != null && existing == null) {
            throw new IllegalArgumentException("The working-day override was not found.");
        }

        MultipartFile officeOrder = form.getOfficeOrderFile();
        boolean hasNewFile = officeOrder != null && !officeOrder.isEmpty();
        if (!hasNewFile && existing == null) {
            throw new IllegalArgumentException("Office order is required.");
        }

        FileUploadResult uploadedFile = hasNewFile ? fileStorageService.store(officeOrder, UPLOAD_MODULE) : null;
        String previousPath = existing == null ? null : existing.getOfficeOrderPath();
        WeekOffWorkingDayEntity entity = toEntity(form, existing, uploadedFile);
        try {
            workingDayService.saveWorkingDay(entity);
        } catch (RuntimeException ex) {
            deleteUploadedFile(uploadedFile);
            throw ex;
        }

        if (uploadedFile != null && StringUtils.hasText(previousPath)
                && !previousPath.equals(uploadedFile.fullPath())) {
            fileStorageService.deleteQuietly(previousPath);
        }
        return existing == null;
    }

    private void validateDate(WeekOffWorkingDayForm form) {
        if (form.getWorkingDate() == null) {
            throw new IllegalArgumentException("Working day date is required.");
        }
        DayOfWeek day = form.getWorkingDate().getDayOfWeek();
        if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("Only Saturday or Sunday can be converted into a working day.");
        }
    }

    private WeekOffWorkingDayEntity toEntity(
            WeekOffWorkingDayForm form,
            WeekOffWorkingDayEntity existing,
            FileUploadResult uploadedFile) {
        WeekOffWorkingDayEntity entity = new WeekOffWorkingDayEntity();
        entity.setId(form.getId());
        entity.setWorkingDate(form.getWorkingDate());
        if (uploadedFile != null) {
            entity.setOfficeOrderOriginalName(uploadedFile.originalFileName());
            entity.setOfficeOrderStoredName(uploadedFile.storedFileName());
            entity.setOfficeOrderPath(uploadedFile.fullPath());
            entity.setOfficeOrderContentType(uploadedFile.contentType());
            entity.setOfficeOrderFileSize(uploadedFile.size());
        } else {
            entity.setOfficeOrderOriginalName(existing.getOfficeOrderOriginalName());
            entity.setOfficeOrderStoredName(existing.getOfficeOrderStoredName());
            entity.setOfficeOrderPath(existing.getOfficeOrderPath());
            entity.setOfficeOrderContentType(existing.getOfficeOrderContentType());
            entity.setOfficeOrderFileSize(existing.getOfficeOrderFileSize());
        }
        return entity;
    }

    private void deleteUploadedFile(FileUploadResult uploadedFile) {
        if (uploadedFile != null) {
            fileStorageService.deleteQuietly(uploadedFile.fullPath());
        }
    }
}
