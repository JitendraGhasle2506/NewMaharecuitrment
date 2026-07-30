package com.maharecruitment.gov.in.web.service.mobile.impl;

import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeFcmToken;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeFcmTokenRepository;
import com.maharecruitment.gov.in.web.dto.mobile.FcmTokenRequest;
import com.maharecruitment.gov.in.web.dto.mobile.MobileFcmTokenResponse;
import com.maharecruitment.gov.in.web.service.mobile.MobileApiException;
import com.maharecruitment.gov.in.web.service.mobile.MobileEmployeeAccessService;
import com.maharecruitment.gov.in.web.service.mobile.NotificationService;

/**
 * Persists mobile device FCM tokens for authenticated employees.
 */
@Service
public class MobileNotificationServiceImpl implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobileNotificationServiceImpl.class);
    private static final String SAVE_MESSAGE = "FCM token saved successfully";
    private static final String UPDATE_MESSAGE = "FCM token updated successfully";

    private final MobileEmployeeAccessService mobileEmployeeAccessService;
    private final EmployeeFcmTokenRepository employeeFcmTokenRepository;

    public MobileNotificationServiceImpl(
            MobileEmployeeAccessService mobileEmployeeAccessService,
            EmployeeFcmTokenRepository employeeFcmTokenRepository) {
        this.mobileEmployeeAccessService = mobileEmployeeAccessService;
        this.employeeFcmTokenRepository = employeeFcmTokenRepository;
    }

    /**
     * Saves or updates the current employee's FCM token for the supplied device.
     */
    @Override
    @Transactional
    public MobileFcmTokenResponse saveToken(FcmTokenRequest request) {
        if (request == null) {
            throw new MobileApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_FCM_TOKEN_REQUEST",
                    "FCM token request is required.");
        }

        Long employeeId = request.employeeId();
        String deviceId = normalizeRequiredText(request.deviceId(), "DEVICE_ID_REQUIRED", "Device ID is required.");
        String fcmToken = normalizeRequiredText(request.fcmToken(), "FCM_TOKEN_REQUIRED", "FCM token is required.");
        String platform = normalizeRequiredText(request.platform(), "PLATFORM_REQUIRED", "Platform is required.")
                .toUpperCase(Locale.ROOT);

        mobileEmployeeAccessService.requireCurrentActiveEmployeeContext(employeeId);

        Optional<EmployeeFcmToken> existingToken = employeeFcmTokenRepository
                .findByEmployeeIdAndDeviceId(employeeId, deviceId);
        boolean update = existingToken.isPresent();
        EmployeeFcmToken employeeFcmToken = existingToken.orElseGet(EmployeeFcmToken::new);

        employeeFcmToken.setEmployeeId(employeeId);
        employeeFcmToken.setDeviceId(deviceId);
        employeeFcmToken.setFcmToken(fcmToken);
        employeeFcmToken.setPlatform(platform);
        employeeFcmTokenRepository.save(employeeFcmToken);

        String operation = update ? "updated" : "saved";
        LOGGER.info(
                "Mobile FCM token {} for employeeId={} deviceId={} token={}",
                operation,
                employeeId,
                deviceId,
                maskToken(fcmToken));

        return new MobileFcmTokenResponse(true, update ? UPDATE_MESSAGE : SAVE_MESSAGE);
    }

    private String normalizeRequiredText(String value, String code, String message) {
        if (!StringUtils.hasText(value)) {
            throw new MobileApiException(HttpStatus.BAD_REQUEST, code, message);
        }
        return value.trim();
    }

    private String maskToken(String token) {
        if (!StringUtils.hasText(token)) {
            return "****";
        }
        String trimmed = token.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 4) + "***********" + trimmed.substring(trimmed.length() - 3);
    }
}
