package com.sparktech.motorx.Services;

import com.sparktech.motorx.entity.UserEntity;

public interface ICurrentUserService {
    UserEntity getAuthenticatedUser();
}

