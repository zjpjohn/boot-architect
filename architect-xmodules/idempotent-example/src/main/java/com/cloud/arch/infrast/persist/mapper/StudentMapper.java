package com.cloud.arch.infrast.persist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.arch.infrast.persist.po.StudentPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentMapper extends BaseMapper<StudentPo> {
}
