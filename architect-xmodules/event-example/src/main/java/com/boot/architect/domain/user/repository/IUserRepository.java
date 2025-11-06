package com.boot.architect.domain.user.repository;

import com.boot.architect.domain.user.model.UserInfoDo;
import com.cloud.arch.aggregate.Repository;

public interface IUserRepository extends Repository<Long, UserInfoDo> {

}
