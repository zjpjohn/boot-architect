package com.boot.architect.application.query;

import com.boot.architect.infrast.persist.po.StudentPo;

public interface IStudentQueryService {

    StudentPo getStudent(Long id);

}
