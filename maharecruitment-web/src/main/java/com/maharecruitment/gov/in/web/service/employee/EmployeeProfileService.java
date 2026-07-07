package com.maharecruitment.gov.in.web.service.employee;

import java.nio.file.Path;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.web.dto.employee.EmployeeProfileDTO;

public interface EmployeeProfileService {

    EmployeeProfileDTO getCurrentEmployeeProfile(String loginEmail);

    EmployeeProfileDTO updateCurrentEmployeeProfile(String loginEmail, EmployeeProfileDTO profileDTO);

    EmployeeProfileDTO uploadCurrentEmployeePhoto(String loginEmail, MultipartFile file);

    Optional<Path> resolveCurrentEmployeePhoto(String loginEmail);
}
