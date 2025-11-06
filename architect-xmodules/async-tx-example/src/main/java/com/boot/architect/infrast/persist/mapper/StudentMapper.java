package com.boot.architect.infrast.persist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boot.architect.infrast.persist.po.StudentPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentMapper extends BaseMapper<StudentPo> {
}
