package com.localcare.healthcare.service;

import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.Medicine;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MedicineService {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Medicine> rowMapper = (rs, rowNum) -> {
        Medicine medicine = new Medicine();
        medicine.setId(rs.getLong("id"));
        medicine.setName(rs.getString("name"));
        medicine.setType(rs.getString("type"));
        medicine.setSpecification(rs.getString("specification"));
        medicine.setPrice(rs.getBigDecimal("price"));
        medicine.setStock(rs.getInt("stock"));
        medicine.setUsageText(rs.getString("usage_text"));
        return medicine;
    };

    public MedicineService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Medicine> list(String keyword) {
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            return jdbcTemplate.query("""
                    SELECT * FROM medicines
                    WHERE name LIKE ? OR type LIKE ? OR specification LIKE ? OR usage_text LIKE ?
                    ORDER BY id ASC
                    """, rowMapper, like, like, like, like);
        }

        return jdbcTemplate.query("SELECT * FROM medicines ORDER BY id ASC", rowMapper);
    }

    public Medicine create(Medicine medicine) {
        validate(medicine);
        jdbcTemplate.update("""
                INSERT INTO medicines (name, type, specification, price, stock, usage_text)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                medicine.getName(),
                medicine.getType(),
                medicine.getSpecification(),
                medicine.getPrice(),
                medicine.getStock(),
                medicine.getUsageText()
        );
        return latestByName(medicine.getName());
    }

    public Medicine update(Long id, Medicine medicine) {
        validate(medicine);
        int rows = jdbcTemplate.update("""
                UPDATE medicines
                SET name = ?, type = ?, specification = ?, price = ?, stock = ?, usage_text = ?
                WHERE id = ?
                """,
                medicine.getName(),
                medicine.getType(),
                medicine.getSpecification(),
                medicine.getPrice(),
                medicine.getStock(),
                medicine.getUsageText(),
                id
        );
        if (rows == 0) {
            throw new BusinessException("药品不存在");
        }
        return get(id);
    }

    public void delete(Long id) {
        int rows = jdbcTemplate.update("DELETE FROM medicines WHERE id = ?", id);
        if (rows == 0) {
            throw new BusinessException("药品不存在");
        }
    }

    private Medicine get(Long id) {
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM medicines WHERE id = ?", rowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("药品不存在");
        }
    }

    private Medicine latestByName(String name) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM medicines WHERE name = ? ORDER BY id DESC LIMIT 1",
                rowMapper,
                name
        );
    }

    private void validate(Medicine medicine) {
        if (medicine == null || !StringUtils.hasText(medicine.getName())) {
            throw new BusinessException("药品名称不能为空");
        }
        if (medicine.getPrice() == null) {
            medicine.setPrice(BigDecimal.ZERO);
        }
        if (medicine.getStock() == null) {
            medicine.setStock(0);
        }
        if (medicine.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("药品价格不能小于 0");
        }
        if (medicine.getStock() < 0) {
            throw new BusinessException("药品库存不能小于 0");
        }
    }
}

