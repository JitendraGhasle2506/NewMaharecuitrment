package com.maharecruitment.gov.in.attendance.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.maharecruitment.gov.in.attendance.client.InternalAttendanceReportClient;
import com.maharecruitment.gov.in.attendance.client.InternalAttendanceReportClientUnavailableException;
import com.maharecruitment.gov.in.attendance.client.model.InternalAttendanceDayRecord;
import com.maharecruitment.gov.in.attendance.config.InternalAttendanceSyncProperties;
import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.attendance.repository.ManualAttendanceRequestRepository;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceSyncResult;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

class InternalAttendanceSyncServiceImplTest {

    private InternalAttendanceSyncServiceImpl service;
    private final AtomicReference<List<DailyAttendanceInternalEntity>> savedEntities = new AtomicReference<>();

    private List<InternalAttendanceDayRecord> apiResponse = Collections.emptyList();
    private List<DailyAttendanceInternalEntity> existingRows = Collections.emptyList();
    private List<EmployeeEntity> candidateEmployees = Collections.emptyList();
    private RuntimeException fetchFailure;

    @BeforeEach
    void setUp() {
        InternalAttendanceSyncProperties properties = new InternalAttendanceSyncProperties();
        properties.setEnabled(true);
        properties.setUniqueCodePrefix("MahaIT");
        properties.setSchedulerZone("Asia/Kolkata");
        properties.setStopOnUpstreamUnavailable(true);
        properties.setMinRequestIntervalMillis(0);

        EmployeeRepository employeeRepository = proxyWithDefaults(EmployeeRepository.class, (proxy, method, args) -> {
            if ("findInternalAttendanceSyncCandidates".equals(method.getName())) {
                return candidateEmployees;
            }
            throw new UnsupportedOperationException("Unexpected EmployeeRepository call: " + method.getName());
        });

        DailyAttendanceInternalRepository dailyAttendanceInternalRepository = proxyWithDefaults(
                DailyAttendanceInternalRepository.class,
                (proxy, method, args) -> {
                    if ("findByEmployeeIdAndAttendanceDateBetween".equals(method.getName())) {
                        return existingRows;
                    }
                    if ("saveAll".equals(method.getName())) {
                        List<DailyAttendanceInternalEntity> entities = new ArrayList<>();
                        @SuppressWarnings("unchecked")
                        Iterable<DailyAttendanceInternalEntity> iterable = (Iterable<DailyAttendanceInternalEntity>) args[0];
                        iterable.forEach(entities::add);
                        savedEntities.set(entities);
                        return entities;
                    }
                    throw new UnsupportedOperationException(
                            "Unexpected DailyAttendanceInternalRepository call: " + method.getName());
                });

        ManualAttendanceRequestRepository manualAttendanceRequestRepository = proxyWithDefaults(
                ManualAttendanceRequestRepository.class,
                (proxy, method, args) -> {
                    if ("findByUserIdAndAttendanceDateBetweenAndHodStatusIgnoreCase".equals(method.getName())) {
                        return Collections.emptyList();
                    }
                    throw new UnsupportedOperationException(
                            "Unexpected ManualAttendanceRequestRepository call: " + method.getName());
                });

        InternalAttendanceReportClient attendanceReportClient = (uniqueCode, startDate, endDate) -> {
            if (fetchFailure != null) {
                throw fetchFailure;
            }
            return apiResponse;
        };

        TransactionTemplate transactionTemplate = new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(new NoOpTransactionStatus());
            }
        };

        service = new InternalAttendanceSyncServiceImpl(
                employeeRepository,
                dailyAttendanceInternalRepository,
                manualAttendanceRequestRepository,
                attendanceReportClient,
                properties,
                transactionTemplate);
    }

    @Test
    void syncEmployeeAttendanceSetsAuditDatesForNewRows() {
        LocalDate attendanceDate = LocalDate.of(2026, 5, 4);
        EmployeeEntity employee = buildEmployee(101L);
        apiResponse = List.of(new InternalAttendanceDayRecord(
                "Test Employee",
                "MahaIT1234",
                attendanceDate,
                "09:00",
                "18:00",
                "P"));
        existingRows = Collections.emptyList();

        int savedRows = service.syncEmployeeAttendance(employee, "MahaIT1234", attendanceDate, attendanceDate);

        assertEquals(1, savedRows);
        DailyAttendanceInternalEntity savedEntity = savedEntities.get().get(0);
        assertNotNull(savedEntity.getCreatedDate());
        assertNotNull(savedEntity.getUpdatedDate());
        assertEquals(savedEntity.getCreatedDate(), savedEntity.getUpdatedDate());
        assertEquals("PRESENT", savedEntity.getStatus());
    }

    @Test
    void syncEmployeeAttendancePreservesCreatedDateAndRefreshesUpdatedDateForExistingRows() {
        LocalDate attendanceDate = LocalDate.of(2026, 5, 4);
        EmployeeEntity employee = buildEmployee(202L);
        LocalDateTime createdDate = LocalDateTime.of(2026, 4, 1, 9, 0);
        LocalDateTime previousUpdatedDate = LocalDateTime.of(2026, 4, 15, 18, 0);

        DailyAttendanceInternalEntity existingEntity = new DailyAttendanceInternalEntity();
        existingEntity.setId(77L);
        existingEntity.setEmployeeId(employee.getEmployeeId());
        existingEntity.setAttendanceDate(attendanceDate);
        existingEntity.setInTime("09:00");
        existingEntity.setOutTime("18:00");
        existingEntity.setTotalHours("09:00");
        existingEntity.setStatus("PRESENT");
        existingEntity.setMonth(attendanceDate.getMonthValue());
        existingEntity.setYear(attendanceDate.getYear());
        existingEntity.setCreatedDate(createdDate);
        existingEntity.setUpdatedDate(previousUpdatedDate);

        apiResponse = List.of(new InternalAttendanceDayRecord(
                "Test Employee",
                "MahaIT1234",
                attendanceDate,
                "09:00",
                "18:00",
                "P"));
        existingRows = List.of(existingEntity);

        int savedRows = service.syncEmployeeAttendance(employee, "MahaIT1234", attendanceDate, attendanceDate);

        assertEquals(1, savedRows);
        DailyAttendanceInternalEntity savedEntity = savedEntities.get().get(0);
        assertEquals(createdDate, savedEntity.getCreatedDate());
        assertNotNull(savedEntity.getUpdatedDate());
        assertTrue(savedEntity.getUpdatedDate().isAfter(previousUpdatedDate));
    }

    @Test
    void syncAttendanceStopsAfterFirstUpstreamUnavailability() {
        LocalDate attendanceDate = LocalDate.of(2026, 5, 4);
        candidateEmployees = List.of(buildEmployee(301L, "123412341234"), buildEmployee(302L, "567856785678"));
        fetchFailure = new InternalAttendanceReportClientUnavailableException(
                "Upstream attendance API is unreachable.",
                new RuntimeException("Connection refused"));

        InternalAttendanceSyncResult result = service.syncAttendance(attendanceDate, attendanceDate);

        assertEquals(1, result.getEmployeesAttempted());
        assertEquals(0, result.getEmployeesSynced());
        assertEquals(1, result.getEmployeesFailed());
        assertEquals(1, result.getEmployeesSkipped());
    }

    private EmployeeEntity buildEmployee(Long employeeId) {
        return buildEmployee(employeeId, "123412341234");
    }

    private EmployeeEntity buildEmployee(Long employeeId, String aadhaarNumber) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(employeeId);
        employee.setJoiningDate(LocalDate.of(2026, 1, 1));
        employee.setAadhaarNumber(aadhaarNumber);
        return employee;
    }

    private static <T> T proxyWithDefaults(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[] { type },
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        switch (method.getName()) {
                            case "toString":
                                return type.getSimpleName() + "Stub";
                            case "hashCode":
                                return System.identityHashCode(proxy);
                            case "equals":
                                return proxy == args[0];
                            default:
                                return null;
                        }
                    }
                    return handler.invoke(proxy, method, args);
                }));
    }

    private static final class NoOpTransactionStatus implements TransactionStatus {

        @Override
        public boolean isNewTransaction() {
            return false;
        }

        @Override
        public boolean hasSavepoint() {
            return false;
        }

        @Override
        public void setRollbackOnly() {
        }

        @Override
        public boolean isRollbackOnly() {
            return false;
        }

        @Override
        public void flush() {
        }

        @Override
        public boolean isCompleted() {
            return false;
        }

        @Override
        public Object createSavepoint() {
            return null;
        }

        @Override
        public void rollbackToSavepoint(Object savepoint) {
        }

        @Override
        public void releaseSavepoint(Object savepoint) {
        }
    }
}
