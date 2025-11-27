package com.boot.architect.infrast.persist.mapper;

import com.boot.architect.infrast.persist.po.UserPo;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserPo> {
}
