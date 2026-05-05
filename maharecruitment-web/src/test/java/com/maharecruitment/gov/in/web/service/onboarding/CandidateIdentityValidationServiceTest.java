package com.maharecruitment.gov.in.web.service.onboarding;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
                    default -> throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
                });

        EmployeeRepository employeeRepository = proxyWithDefaults(
                EmployeeRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsByNormalizedAadhaarNumber" -> {
                        capturedAadhaar.set((String) args[0]);
                        yield duplicateEmployeeAadhaar.get();
                    }
                    case "existsByNormalizedPanNumber" -> {
                        capturedPan.set((String) args[0]);
                        yield duplicateEmployeePan.get();
                    }
                    case "existsByNormalizedEmail" -> {
                        capturedEmail.set((String) args[0]);
                        yield duplicateEmployeeEmail.get();
                    }
                    case "existsByNormalizedMobile" -> {
                        capturedMobile.set((String) args[0]);
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
