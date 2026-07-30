package com.maharecruitment.gov.in.department.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.department.repository.DepartmentOnboardingPageRepo;
import com.maharecruitment.gov.in.department.repository.projection.DepartmentOnboardedEmployeeView;
import com.maharecruitment.gov.in.department.service.DepartmentOnboardingPageService;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;

@Service
@Transactional(readOnly = true)
public class DepartmentOnboardingPageServiceImpl implements DepartmentOnboardingPageService {

	private final UserRepository userRepository;
	private final DepartmentOnboardingPageRepo departmentOnboardingPageRepo;

	public DepartmentOnboardingPageServiceImpl(UserRepository userRepository,
			DepartmentOnboardingPageRepo departmentOnboardingPageRepo) {
		this.userRepository = userRepository;
		this.departmentOnboardingPageRepo = departmentOnboardingPageRepo;

	}

	@Override
	public List<DepartmentOnboardedEmployeeView> getOnboardedEmployees(String actorEmail) {
		if (!StringUtils.hasText(actorEmail)) {
			throw new RecruitmentNotificationException("Authenticated department user is required.");
		}

		User user = userRepository.findByEmailIgnoreCase(actorEmail.trim())
				.orElseThrow(() -> new RecruitmentNotificationException("Department user details were not found."));

		if (user.getDepartmentRegistrationId() == null
				|| user.getDepartmentRegistrationId().getDepartmentRegistrationId() == null) {
			throw new RecruitmentNotificationException("Department mapping is not available for the logged-in user.");
		}

		Long departmentId = user.getDepartmentRegistrationId().getDepartmentRegistrationId();
		return departmentOnboardingPageRepo.findActiveOnboardedEmployeesByDepartmentId(departmentId);
	}
}
