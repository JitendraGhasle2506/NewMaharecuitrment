package com.maharecruitment.gov.in.recruitment.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

class EmployeeEntityTest {

    @Test
    void departmentUsesDepartmentIdJoinColumn() throws NoSuchFieldException {
        Field department = EmployeeEntity.class.getDeclaredField("department");

        assertNotNull(department.getAnnotation(ManyToOne.class));
        JoinColumn joinColumn = department.getAnnotation(JoinColumn.class);
        assertNotNull(joinColumn);
        assertEquals("department_id", joinColumn.name());
    }
}
