package com.boot.architect.application.query.impl;

import com.boot.architect.application.query.IStudentQueryService;
import com.boot.architect.infrast.persist.mapper.StudentMapper;
import com.boot.architect.infrast.persist.po.StudentPo;
import com.cloud.arch.cache.annotations.CacheResult;
import com.cloud.arch.cache.annotations.Remote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentQueryService implements IStudentQueryService {

    private final StudentMapper mapper;

    @Override
    @CacheResult(names = "student", key = "#id", remote = @Remote(expire = 1200))
    public StudentPo getStudent(Long id) {
        return mapper.selectById(id);
    }
}
