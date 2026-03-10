package com.maharecruitment.gov.in.auth.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.auth.entity.Role;

public interface RoleManagementService {

    Page<Role> getAll(Pageable pageable);

    List<Role> getAll();

    Role getById(Long id);

    Role create(String name);

    Role update(Long id, String name);

    void delete(Long id);
}
