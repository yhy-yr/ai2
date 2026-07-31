package com.localcare.healthcare.service;

import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.Department;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DepartmentService {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Department> rowMapper = (rs, rowNum) -> {
        Department department = new Department();
        department.setId(rs.getLong("id"));
        department.setName(rs.getString("name"));
        department.setDescription(rs.getString("description"));
        return department;
    };

    public DepartmentService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Department> list(String keyword) {
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            return jdbcTemplate.query("""
                    SELECT * FROM departments
                    WHERE name LIKE ? OR description LIKE ?
                    ORDER BY id ASC
                    """, rowMapper, like, like);
        }

        return jdbcTemplate.query("SELECT * FROM departments ORDER BY id ASC", rowMapper);
    }

    public Department create(Department department) {
        validate(department);
        jdbcTemplate.update(
                "INSERT INTO departments (name, description) VALUES (?, ?)",
                department.getName(),
                department.getDescription()
        );
        return latestByName(department.getName());
    }

    public Department update(Long id, Department department) {
        validate(department);
        int rows = jdbcTemplate.update(
                "UPDATE departments SET name = ?, description = ? WHERE id = ?",
                department.getName(),
                department.getDescription(),
                id
        );
        if (rows == 0) {
            throw new BusinessException("科室不存在");
        }
        return get(id);
    }

    public void delete(Long id) {
        int rows = jdbcTemplate.update("DELETE FROM departments WHERE id = ?", id);
        if (rows == 0) {
            throw new BusinessException("科室不存在");
        }
    }

    private Department get(Long id) {
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM departments WHERE id = ?", rowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("科室不存在");
        }
    }

    private Department latestByName(String name) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM departments WHERE name = ? ORDER BY id DESC LIMIT 1",
                rowMapper,
                name
        );
    }

    private void validate(Department department) {
        if (department == null || !StringUtils.hasText(department.getName())) {
            throw new BusinessException("科室名称不能为空");
        }
    }
}

