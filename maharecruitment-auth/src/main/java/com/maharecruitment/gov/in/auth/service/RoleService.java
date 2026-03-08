package com.maharecruitment.gov.in.auth.service;

import com.maharecruitment.gov.in.auth.entity.Role;

public interface RoleService {

    Role getByName(String roleName);
}
