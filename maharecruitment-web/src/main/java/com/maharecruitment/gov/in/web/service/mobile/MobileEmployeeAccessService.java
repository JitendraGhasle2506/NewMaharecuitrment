package com.maharecruitment.gov.in.web.service.mobile;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

@Service
public class MobileEmployeeAccessService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    public MobileEmployeeAccessService(
            UserRepository userRepository,
            EmployeeRepository employeeRepository) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public EmployeeEntity requireCurrentActiveEmployee(Long requestedEmployeeId) {
        return requireCurrentActiveEmployeeContext(requestedEmployeeId).employee();
    }

    @Transactional(readOnly = true)
    public MobileEmployeeAccessContext requireCurrentActiveEmployeeContext(Long requestedEmployeeId) {
        if (requestedEmployeeId == null) {
            throw badRequest("EMPLOYEE_ID_REQUIRED", "Employee ID is required.");
        }

        String username = currentAuthenticatedUsername();
        User user = userRepository.findByEmailIgnoreCaseAndActiveTrue(username)
                .orElseThrow(() -> new MobileApiException(
                        HttpStatus.UNAUTHORIZED,
                        "UNAUTHENTICATED",
                        "Valid mobile authentication token is required."));

        List<EmployeeEntity> profiles = employeeRepository.findMobileLoginProfilesByEmail(user.getEmail().trim());
        EmployeeEntity employee = profiles.stream()
                .filter(this::isActiveEmployee)
                .findFirst()
                .orElseThrow(() -> new MobileApiException(
                        HttpStatus.FORBIDDEN,
                        "EMPLOYEE_INACTIVE",
                        "Active employee profile was not found for the logged-in user."));

        if (!Objects.equals(employee.getEmployeeId(), requestedEmployeeId)) {
            throw new MobileApiException(
                    HttpStatus.FORBIDDEN,
                    "EMPLOYEE_MISMATCH",
                    "You can access only the logged-in employee details.");
        }
        return new MobileEmployeeAccessContext(user, employee);
    }

    private String currentAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !StringUtils.hasText(authentication.getName())) {
            throw new MobileApiException(
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHENTICATED",
                    "Valid mobile authentication token is required.");
        }
        return authentication.getName().trim();
    }

    private boolean isActiveEmployee(EmployeeEntity employee) {
        return employee != null && ACTIVE_STATUS.equalsIgnoreCase(StringUtils.trimWhitespace(employee.getStatus()));
    }

    private MobileApiException badRequest(String code, String message) {
        return new MobileApiException(HttpStatus.BAD_REQUEST, code, message);
    }
}
