package com.maharecruitment.gov.in.department.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.department.repository.DepartmentOnboardingPageRepo;
import com.maharecruitment.gov.in.department.repository.projection.DepartmentOnboardedEmployeeView;
import com.maharecruitment.gov.in.department.service.DepartmentOnboardingPageService;

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

		User user = userRepository.findByEmail(actorEmail);

		Long departmentId = user.getDepartmentRegistrationId().getDepartmentRegistrationId();
		System.out.println("departmentId---" + departmentId);
		List<Object[]> rows = departmentOnboardingPageRepo.getOnboardedEmployees(departmentId);
		List<DepartmentOnboardedEmployeeView> result = new ArrayList<>();
		for (Object[] row : rows) {

			result.add(new DepartmentOnboardedEmployeeView(
		            (Long) row[0],
		            (String) row[1],
		            (String) row[2],
		            (String) row[3],
		            (String) row[4],
		            (String) row[5],
		            (String) row[6],
		            (LocalDate) row[7],
		            (LocalDate) row[8],
		            (LocalDate) row[9],
		            (String) row[10],
		            (String) row[11]
		    ));

			// TODO Auto-generated method stub
		}

		return result;
	}
}
