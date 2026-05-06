package com.maharecruitment.gov.in.web.service.onboarding;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.recruitment.entity.AgencyCandidatePreOnboardingEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.AgencyCandidatePreOnboardingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

class CandidateIdentityValidationServiceTest {

    private final AtomicBoolean duplicatePreOnboardingAadhaar = new AtomicBoolean(false);
    private final AtomicBoolean duplicateEmployeeAadhaar = new AtomicBoolean(false);
    private final AtomicBoolean duplicatePreOnboardingPan = new AtomicBoolean(false);
    private final AtomicBoolean duplicateEmployeePan = new AtomicBoolean(false);
    private final AtomicBoolean duplicatePreOnboardingEmail = new AtomicBoolean(false);
    private final AtomicBoolean duplicateEmployeeEmail = new AtomicBoolean(false);
    private final AtomicBoolean duplicatePreOnboardingMobile = new AtomicBoolean(false);
    private final AtomicBoolean duplicateEmployeeMobile = new AtomicBoolean(false);
    private final AtomicReference<String> capturedAadhaar = new AtomicReference<>();
    private final AtomicReference<String> capturedPan = new AtomicReference<>();
    private final AtomicReference<String> capturedEmail = new AtomicReference<>();
    private final AtomicReference<String> capturedMobile = new AtomicReference<>();
    private final AtomicReference<Long> capturedEmployeeAadhaarExcludedPreOnboardingId = new AtomicReference<>();
    private final AtomicReference<Long> capturedEmployeePanExcludedPreOnboardingId = new AtomicReference<>();
    private final AtomicReference<Long> capturedEmployeeEmailExcludedPreOnboardingId = new AtomicReference<>();
    private final AtomicReference<Long> capturedEmployeeMobileExcludedPreOnboardingId = new AtomicReference<>();
    private final AtomicReference<AgencyCandidatePreOnboardingEntity> currentPreOnboarding = new AtomicReference<>();

    private CandidateIdentityValidationService service;

    @BeforeEach
    void setUp() {
        AgencyCandidatePreOnboardingRepository preOnboardingRepository = proxyWithDefaults(
                AgencyCandidatePreOnboardingRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsByAadhaarNumberExcludingPreOnboardingId" -> {
                        capturedAadhaar.set((String) args[0]);
                        yield duplicatePreOnboardingAadhaar.get();
                    }
                    case "existsByPanNumberExcludingPreOnboardingId" -> {
                        capturedPan.set((String) args[0]);
                        yield duplicatePreOnboardingPan.get();
                    }
                    case "existsByCandidateEmailExcludingPreOnboardingId" -> {
                        capturedEmail.set((String) args[0]);
                        yield duplicatePreOnboardingEmail.get();
                    }
                    case "existsByCandidateMobileExcludingPreOnboardingId" -> {
                        capturedMobile.set((String) args[0]);
                        yield duplicatePreOnboardingMobile.get();
                    }
                    case "findById" -> {
                        Long requestedId = (Long) args[0];
                        AgencyCandidatePreOnboardingEntity entity = currentPreOnboarding.get();
                        if (entity == null || entity.getPreOnboardingId() == null
                                || !entity.getPreOnboardingId().equals(requestedId)) {
                            yield Optional.empty();
                        }
                        yield Optional.of(entity);
                    }
                    default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                });

        EmployeeRepository employeeRepository = proxyWithDefaults(
                EmployeeRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsByNormalizedAadhaarNumberExcludingPreOnboardingId" -> {
                        capturedAadhaar.set((String) args[0]);
                        capturedEmployeeAadhaarExcludedPreOnboardingId.set((Long) args[1]);
                        yield duplicateEmployeeAadhaar.get();
                    }
                    case "existsByNormalizedPanNumberExcludingPreOnboardingId" -> {
                        capturedPan.set((String) args[0]);
                        capturedEmployeePanExcludedPreOnboardingId.set((Long) args[1]);
                        yield duplicateEmployeePan.get();
                    }
                    case "existsByNormalizedEmailExcludingPreOnboardingId" -> {
                        capturedEmail.set((String) args[0]);
                        capturedEmployeeEmailExcludedPreOnboardingId.set((Long) args[1]);
                        yield duplicateEmployeeEmail.get();
                    }
                    case "existsByNormalizedMobileExcludingPreOnboardingId" -> {
                        capturedMobile.set((String) args[0]);
                        capturedEmployeeMobileExcludedPreOnboardingId.set((Long) args[1]);
                        yield duplicateEmployeeMobile.get();
                    }
                    default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                });

        service = new CandidateIdentityValidationService(preOnboardingRepository, employeeRepository);
    }

    @Test
    void validateUniqueGovernmentIdsNormalizesValuesBeforeLookup() {
        assertDoesNotThrow(() -> service.validateUniqueGovernmentIds(9L, "1234 1234 1234", "abcde1234f"));

        assertEquals("123412341234", capturedAadhaar.get());
        assertEquals("ABCDE1234F", capturedPan.get());
    }

    @Test
    void validateUniqueGovernmentIdsRejectsDuplicateAadhaar() {
        duplicatePreOnboardingAadhaar.set(true);

        RecruitmentNotificationException exception = assertThrows(
                RecruitmentNotificationException.class,
                () -> service.validateUniqueGovernmentIds(null, "123412341234", "ABCDE1234F"));

        assertEquals("Aadhaar number already exists in the system.", exception.getMessage());
    }

    @Test
    void validateUniqueGovernmentIdsRejectsDuplicatePan() {
        duplicateEmployeePan.set(true);

        RecruitmentNotificationException exception = assertThrows(
                RecruitmentNotificationException.class,
                () -> service.validateUniqueGovernmentIds(null, "123412341234", "abcde1234f"));

        assertEquals("PAN number already exists in the system.", exception.getMessage());
    }

    @Test
    void validateUniqueCandidateDetailsNormalizesEmailAndMobileBeforeLookup() {
        assertDoesNotThrow(() -> service.validateUniqueCandidateDetails(
                9L,
                "1234 1234 1234",
                "abcde1234f",
                " Test.User@Example.com ",
                "9876543210"));

        assertEquals("123412341234", capturedAadhaar.get());
        assertEquals("ABCDE1234F", capturedPan.get());
        assertEquals("test.user@example.com", capturedEmail.get());
        assertEquals("9876543210", capturedMobile.get());
        assertEquals(9L, capturedEmployeeAadhaarExcludedPreOnboardingId.get());
        assertEquals(9L, capturedEmployeePanExcludedPreOnboardingId.get());
        assertEquals(9L, capturedEmployeeEmailExcludedPreOnboardingId.get());
        assertEquals(9L, capturedEmployeeMobileExcludedPreOnboardingId.get());
    }

    @Test
    void validateUniqueCandidateDetailsRejectsDuplicateEmail() {
        duplicateEmployeeEmail.set(true);

        RecruitmentNotificationException exception = assertThrows(
                RecruitmentNotificationException.class,
                () -> service.validateUniqueCandidateDetails(
                        null,
                        "123412341234",
                        "ABCDE1234F",
                        "duplicate@example.com",
                        "9876543210"));

        assertEquals("Email already exists in the system.", exception.getMessage());
    }

    @Test
    void validateUniqueCandidateDetailsRejectsDuplicateMobile() {
        duplicatePreOnboardingMobile.set(true);

        RecruitmentNotificationException exception = assertThrows(
                RecruitmentNotificationException.class,
                () -> service.validateUniqueCandidateDetails(
                        null,
                        "123412341234",
                        "ABCDE1234F",
                        "unique@example.com",
                        "9876543210"));

        assertEquals("Mobile number already exists in the system.", exception.getMessage());
    }

    @Test
    void validateUniqueCandidateDetailsAllowsEditingWhenValuesMatchCurrentPreOnboardingRecord() {
        duplicatePreOnboardingAadhaar.set(true);
        duplicateEmployeeAadhaar.set(true);
        duplicatePreOnboardingPan.set(true);
        duplicateEmployeePan.set(true);
        duplicatePreOnboardingEmail.set(true);
        duplicateEmployeeEmail.set(true);
        duplicatePreOnboardingMobile.set(true);
        duplicateEmployeeMobile.set(true);

        AgencyCandidatePreOnboardingEntity entity = new AgencyCandidatePreOnboardingEntity();
        entity.setPreOnboardingId(9L);
        entity.setAadhaarNumber("123412341234");
        entity.setPanNumber("ABCDE1234F");
        entity.setCandidateEmail("test.user@example.com");
        entity.setCandidateMobile("9876543210");
        currentPreOnboarding.set(entity);

        assertDoesNotThrow(() -> service.validateUniqueCandidateDetails(
                9L,
                "1234 1234 1234",
                "abcde1234f",
                " Test.User@Example.com ",
                "9876543210"));
    }

    private static <T> T proxyWithDefaults(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[] { type },
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> type.getSimpleName() + "Stub";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }
                    return handler.invoke(proxy, method, args);
                }));
    }
}
