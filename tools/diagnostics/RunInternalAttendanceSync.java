import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;

import com.maharecruitment.gov.in.attendance.client.impl.MahaitInternalAttendanceReportClient;
import com.maharecruitment.gov.in.attendance.client.model.InternalAttendanceDayRecord;
import com.maharecruitment.gov.in.attendance.config.InternalAttendanceSyncProperties;
import com.maharecruitment.gov.in.attendance.repository.DailyAttendanceInternalRepository;
import com.maharecruitment.gov.in.attendance.repository.ManualAttendanceRequestRepository;
import com.maharecruitment.gov.in.attendance.service.impl.InternalAttendanceSyncServiceImpl;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

import jakarta.persistence.EntityManager;

/** Explicit, one-day maintenance invocation of the application sync service. No schema changes or schedulers. */
public final class RunInternalAttendanceSync {
    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !"--execute".equals(args[0])) {
            throw new IllegalArgumentException("Usage: RunInternalAttendanceSync --execute YYYY-MM-DD");
        }
        LocalDate date = LocalDate.parse(args[1]);
        if (date.isAfter(LocalDate.now(ZoneId.of("Asia/Kolkata")))) {
            throw new IllegalArgumentException("A future attendance date is not allowed");
        }
        Properties settings = new Properties();
        for (String name : List.of("application.properties", "application-local.properties")) {
            try (InputStream input = Files.newInputStream(Path.of("maharecruitment-web/src/main/resources", name))) {
                settings.load(input);
            }
        }
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new PropertiesPropertySource("application", settings));
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(environment.getRequiredProperty("spring.datasource.url"));
        dataSource.setUsername(environment.getRequiredProperty("spring.datasource.username"));
        dataSource.setPassword(environment.getRequiredProperty("spring.datasource.password"));
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("ApplicationName", "maharecruitment-manual-attendance-sync");
        connectionProperties.setProperty("connectTimeout", "10");
        connectionProperties.setProperty("socketTimeout", "30");
        connectionProperties.setProperty("options", environment.getProperty(
                "spring.datasource.hikari.data-source-properties.options", ""));
        dataSource.setConnectionProperties(connectionProperties);

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setPackagesToScan("com.maharecruitment.gov.in");
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "none",
                "hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"));
        try {
            factory.afterPropertiesSet();
            var transactionManager = new JpaTransactionManager(factory.getObject());
            var beans = new DefaultListableBeanFactory();
            beans.registerSingleton("transactionManager", transactionManager);
            beans.registerSingleton("entityPathResolver", org.springframework.data.querydsl.SimpleEntityPathResolver.INSTANCE);
            EntityManager manager = SharedEntityManagerCreator.createSharedEntityManager(factory.getObject());
            EmployeeRepository employees = repository(EmployeeRepository.class, manager, beans);
            DailyAttendanceInternalRepository attendance = repository(DailyAttendanceInternalRepository.class, manager, beans);
            ManualAttendanceRequestRepository manual = repository(ManualAttendanceRequestRepository.class, manager, beans);

            InternalAttendanceSyncProperties properties = new InternalAttendanceSyncProperties();
            properties.setBaseUrl(environment.getProperty("attendance.integration.internal.base-url", properties.getBaseUrl()));
            properties.setRateLimitRetryAttempts(0);
            properties.setMinRequestIntervalMillis(0);
            // Fetch before any writes so an unreachable API cannot cause employee-code updates.
            List<InternalAttendanceDayRecord> records = new MahaitInternalAttendanceReportClient(properties)
                    .fetchAttendanceReport(date, date);
            Map<String, EmployeeEntity> identities = new HashMap<>();
            for (EmployeeEntity employee : employees.findInternalAttendanceSyncCandidates()) {
                if (employee.getEmployeeCode() != null) {
                    identities.put(employee.getEmployeeCode().trim().toUpperCase(Locale.ROOT), employee);
                }
            }
            // Quarantine clear identity conflicts during this maintenance run; never repair codes by guessing names.
            var conflictingCodes = new HashSet<String>();
            for (InternalAttendanceDayRecord record : records) {
                if (record.getUniqueCode() == null) continue;
                String code = record.getUniqueCode().trim().toUpperCase(Locale.ROOT);
                EmployeeEntity employee = identities.get(code);
                if (employee != null && namesHaveNoCommonToken(employee.getFullName(), record.getEmployeeName())) {
                    conflictingCodes.add(code);
                }
            }
            conflictingCodes.stream().sorted().forEach(code -> System.out.println("identity_conflict_skipped=" + code));
            List<InternalAttendanceDayRecord> safeRecords = records.stream()
                    .filter(record -> record.getUniqueCode() == null
                            || !conflictingCodes.contains(record.getUniqueCode().trim().toUpperCase(Locale.ROOT)))
                    .toList();
            System.out.printf("date=%s eligible=%d api_rows=%d conflicting_codes=%d%n",
                    date, employees.countInternalAttendanceSyncCandidates(), records.size(), conflictingCodes.size());
            var service = new InternalAttendanceSyncServiceImpl(employees, attendance, manual,
                    (start, end) -> safeRecords, properties, new TransactionTemplate(transactionManager));
            var result = service.syncAttendance(date, date);
            System.out.printf("RESULT attempted=%d synced=%d skipped=%d failed=%d inserted=%d updated=%d rows_skipped=%d%n",
                    result.getEmployeesAttempted(), result.getEmployeesSynced(), result.getEmployeesSkipped(),
                    result.getEmployeesFailed(), result.getAttendanceRowsInserted(), result.getAttendanceRowsUpdated(),
                    result.getAttendanceRowsSkipped());
            if (result.getFailureMessage() != null) System.out.println("result_message=" + result.getFailureMessage());
            if (result.getEmployeesFailed() > 0) throw new IllegalStateException("Some employee syncs failed; inspect run log");
        } finally {
            factory.destroy();
        }
    }

    private static boolean namesHaveNoCommonToken(String local, String remote) {
        if (local == null || local.isBlank() || remote == null || remote.isBlank()) return false;
        List<String> localTokens = Arrays.stream(local.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(token -> token.length() > 1).toList();
        List<String> remoteTokens = Arrays.stream(remote.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(token -> token.length() > 1).toList();
        return !localTokens.isEmpty() && !remoteTokens.isEmpty()
                && localTokens.stream().noneMatch(remoteTokens::contains);
    }

    private static <S, ID, T extends JpaRepository<S, ID>> T repository(
            Class<T> type, EntityManager manager, DefaultListableBeanFactory beans) throws Exception {
        JpaRepositoryFactoryBean<T, S, ID> bean = new JpaRepositoryFactoryBean<>(type);
        bean.setEntityManager(manager);
        bean.setEntityPathResolver(beans.getBeanProvider(org.springframework.data.querydsl.EntityPathResolver.class));
        bean.setBeanFactory(beans);
        bean.setTransactionManager("transactionManager");
        bean.afterPropertiesSet();
        return bean.getObject();
    }
}
