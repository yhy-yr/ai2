package com.localcare.healthcare.service;

import com.localcare.healthcare.common.BusinessException;
import com.localcare.healthcare.model.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private static final String LOGIN_EXPIRED_MESSAGE = "未登录或登录已过期";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getString("role"));
        user.setRealName(rs.getString("real_name"));
        user.setPhone(rs.getString("phone"));
        user.setStatus(rs.getString("status"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return user;
    };

    public AuthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BusinessException("用户名和密码不能为空");
        }

        User user;
        try {
            user = jdbcTemplate.queryForObject(
                    "SELECT * FROM users WHERE username = ? AND status = 'ENABLED'",
                    userRowMapper,
                    username
            );
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user == null || !password.equals(user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String sessionToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusDays(1);

        jdbcTemplate.update("""
                INSERT INTO auth_sessions (user_id, session_token, expired_at, created_at)
                VALUES (?, ?, ?, ?)
                """, user.getId(), sessionToken, expiredAt, now);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionToken", sessionToken);
        result.put("user", toSafeUserMap(user));
        return result;
    }

    @Transactional
    public Map<String, Object> register(Map<String, String> body) {
        String username = trim(body == null ? null : body.get("username"));
        String password = trim(body == null ? null : body.get("password"));
        String confirmPassword = trim(body == null ? null : body.get("confirmPassword"));
        String name = trim(body == null ? null : body.get("name"));
        String phone = trim(body == null ? null : body.get("phone"));

        validateRegister(username, password, confirmPassword, name, phone);

        Integer usernameCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                username
        );
        if (usernameCount != null && usernameCount > 0) {
            throw new BusinessException("用户名已存在");
        }

        Integer phoneCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE phone = ?",
                Integer.class,
                phone
        );
        if (phoneCount != null && phoneCount > 0) {
            throw new BusinessException("手机号已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO users (username, password, role, real_name, phone, status, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, username);
            // 当前课程设计登录逻辑使用明文密码，为兼容演示账号暂沿用；后续可统一升级为 BCrypt。
            ps.setString(2, password);
            ps.setString(3, "PATIENT");
            ps.setString(4, name);
            ps.setString(5, phone);
            ps.setString(6, "ENABLED");
            ps.setObject(7, now);
            return ps;
        }, keyHolder);

        Long userId = keyHolder.getKey().longValue();
        jdbcTemplate.update("""
                INSERT INTO patients (user_id, gender, age, address, allergy_history, medical_history)
                VALUES (?, ?, ?, ?, ?, ?)
                """, userId, "", 0, "", "", "");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "注册成功，请登录");
        result.put("username", username);
        return result;
    }

    public Map<String, Object> currentUserInfo(String sessionToken) {
        return toSafeUserMap(requireUser(sessionToken));
    }

    public User requireUser(String sessionToken) {
        if (!StringUtils.hasText(sessionToken)) {
            throw new BusinessException(LOGIN_EXPIRED_MESSAGE);
        }

        try {
            Map<String, Object> session = jdbcTemplate.queryForMap(
                    "SELECT user_id, expired_at FROM auth_sessions WHERE session_token = ?",
                    sessionToken
            );

            LocalDateTime expiredAt = toLocalDateTime(session.get("expired_at"));
            if (expiredAt.isBefore(LocalDateTime.now())) {
                jdbcTemplate.update("DELETE FROM auth_sessions WHERE session_token = ?", sessionToken);
                throw new BusinessException(LOGIN_EXPIRED_MESSAGE);
            }

            Long userId = ((Number) session.get("user_id")).longValue();
            User user = jdbcTemplate.queryForObject(
                    "SELECT * FROM users WHERE id = ? AND status = 'ENABLED'",
                    userRowMapper,
                    userId
            );
            if (user == null) {
                throw new BusinessException(LOGIN_EXPIRED_MESSAGE);
            }
            return user;
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException(LOGIN_EXPIRED_MESSAGE);
        }
    }

    public void logout(String sessionToken) {
        if (StringUtils.hasText(sessionToken)) {
            jdbcTemplate.update("DELETE FROM auth_sessions WHERE session_token = ?", sessionToken);
        }
    }

    public void requireRole(User user, String expectedRole) {
        if (user == null || !expectedRole.equals(user.getRole())) {
            throw new BusinessException("无权访问该页面");
        }
    }

    public Map<String, Object> toSafeUserMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("realName", user.getRealName());
        map.put("role", user.getRole());
        return map;
    }

    private void validateRegister(String username, String password, String confirmPassword, String name, String phone) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)
                || !StringUtils.hasText(confirmPassword) || !StringUtils.hasText(name)
                || !StringUtils.hasText(phone)) {
            throw new BusinessException("参数不能为空");
        }
        if (password.length() < 6) {
            throw new BusinessException("密码长度至少 6 位");
        }
        if (!password.equals(confirmPassword)) {
            throw new BusinessException("两次密码不一致");
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        throw new BusinessException(LOGIN_EXPIRED_MESSAGE);
    }
}
