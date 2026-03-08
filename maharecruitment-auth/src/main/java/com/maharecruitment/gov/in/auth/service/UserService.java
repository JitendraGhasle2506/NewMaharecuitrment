package com.maharecruitment.gov.in.auth.service;

import com.maharecruitment.gov.in.auth.entity.User;

public interface UserService {

    User findUserByEmail(String email);
}
