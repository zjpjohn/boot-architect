package com.boot.architect.infrast.persist.mapper;

import com.boot.architect.infrast.persist.po.StudentPo;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentMapper extends BaseMapper<StudentPo> {
}
