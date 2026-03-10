package com.maharecruitment.gov.in.auth.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.auth.dto.UserUpsertRequest;
import com.maharecruitment.gov.in.auth.entity.User;

public interface UserManagementService {

    Page<User> getAll(Pageable pageable);

    User getById(Long id);

    User create(UserUpsertRequest request);

    User update(Long id, UserUpsertRequest request);

    void delete(Long id);
}
