package com.campus.secondhand.service;

import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.repository.UserRepository;
import com.campus.secondhand.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;



@org.springframework.stereotype.Service
public class UserApplicationService {
    @Autowired private SessionRegistry sessions;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Logger log = LoggerFactory.getLogger(UserApplicationService.class);

    private Map<String, Object> profileBody(User user) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", user.getId());
        body.put("username", user.getUsername());
        body.put("email", user.getEmail());
        body.put("phone", user.getPhone() != null ? user.getPhone() : "");
        body.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
        body.put("bio", user.getBio() != null ? user.getBio() : "");
        body.put("location", user.getLocation() != null ? user.getLocation() : "");
        body.put("qq", user.getQq() != null ? user.getQq() : "");
        body.put("wechat", user.getWechat() != null ? user.getWechat() : "");
        body.put("studentNo", user.getStudentNo() != null ? user.getStudentNo() : "");
        body.put("role", user.getRole() == null ? 0 : user.getRole());
        body.put("createdTime", user.getCreatedTime() == null ? "" : user.getCreatedTime().toLocalDate().toString());
        return body;
    }

    private Optional<User> getUserFromToken(String token) {
        return com.campus.secondhand.config.CurrentUser.get();
    }

    public ApiResponse<?> getProfile(String authHeader) {
        try {
            Optional<User> userOpt = getUserFromToken(authHeader);
            if (userOpt.isEmpty()) {
                return (ApiResponse.error(401, "未登录或登录已过期"));
            }

            return (ApiResponse.success("获取成功", profileBody(userOpt.get())));
        } catch (Exception e) {
            log.error("获取用户资料失败: ", e);
            return (ApiResponse.error(500, "获取资料失败"));
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public ApiResponse<?> updateProfile(
            String authHeader,
            Map<String, String> profileData) {
        try {
            Optional<User> userOpt = getUserFromToken(authHeader);
            if (userOpt.isEmpty()) {
                return (ApiResponse.error(401, "未登录或登录已过期"));
            }

            User user = userOpt.get();
            String previousUsername = user.getUsername();
            String previousPassword = user.getPassword();

            if (profileData.containsKey("username")) {
                String username = profileData.get("username") == null ? "" : profileData.get("username").trim();
                if (username.isEmpty()) {
                    return (ApiResponse.error(400, "用户名不能为空"));
                }
                if (username.length() < 2 || username.length() > 20) {
                    return (ApiResponse.error(400, "用户名需要在2-20个字符之间"));
                }
                Optional<User> duplicate = userRepository.findByUsername(username);
                if (duplicate.isPresent() && !duplicate.get().getId().equals(user.getId())) {
                    return (ApiResponse.error(409, "用户名已被使用"));
                }
                user.setUsername(username);
            }

            if (profileData.containsKey("avatar")) {
                String avatar = profileData.get("avatar");
                if (avatar != null && avatar.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 60000) return (ApiResponse.error(400, "头像内容过大"));
                com.campus.secondhand.util.ImageValue.validate(avatar);
                user.setAvatar(avatar);
            }
            if (profileData.containsKey("bio")) {
                user.setBio(profileData.get("bio"));
            }
            if (profileData.containsKey("location")) {
                user.setLocation(profileData.get("location"));
            }
            if (profileData.containsKey("qq")) {
                user.setQq(profileData.get("qq"));
            }
            if (profileData.containsKey("wechat")) {
                user.setWechat(profileData.get("wechat"));
            }
            if (profileData.containsKey("phone")) {
                user.setPhone(profileData.get("phone"));
            }
            if (profileData.containsKey("studentNo")) {
                String studentNo = profileData.get("studentNo") == null ? "" : profileData.get("studentNo").trim();
                if (!studentNo.isEmpty() && !studentNo.matches("[A-Za-z0-9]{6,20}")) {
                    return (ApiResponse.error(400, "学号需要 6 到 20 位字母或数字"));
                }
                if (!studentNo.isEmpty()) {
                    Optional<User> owner = userRepository.findByStudentNo(studentNo);
                    if (owner.isPresent() && !owner.get().getId().equals(user.getId())) {
                        return (ApiResponse.error(400, "学号已被使用"));
                    }
                }
                user.setStudentNo(studentNo.isEmpty() ? null : studentNo);
            }
            for (Map.Entry<String, Integer> limit : Map.of("bio", 200, "location", 100, "phone", 20, "qq", 20, "wechat", 40, "avatar", 60000).entrySet()) {
                String value = profileData.get(limit.getKey());
                if (value != null && value.length() > limit.getValue()) {
                    return (ApiResponse.error(400, "资料内容超出长度限制"));
                }
            }
            String newPassword = profileData.get("newPassword");
            if (newPassword != null && !newPassword.isBlank()) {
                String oldPassword = profileData.get("oldPassword");
                if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
                    return (ApiResponse.error(400, "原密码不正确"));
                }
                if (newPassword.length() < 6 || newPassword.length() > 20) {
                    return (ApiResponse.error(400, "新密码需要 6 到 20 位"));
                }
                user.setPassword(passwordEncoder.encode(newPassword));
            }

            var update = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<User>();
            update.eq("id", user.getId()).eq("status", 1).eq("password", previousPassword);
            for (String field : java.util.List.of("username", "avatar", "bio", "location", "qq", "wechat", "phone")) {
                if (profileData.containsKey(field)) update.set(field, "username".equals(field) ? user.getUsername() : profileData.get(field));
            }
            if (profileData.containsKey("studentNo")) update.set("student_no", user.getStudentNo());
            if (!previousPassword.equals(user.getPassword())) update.set("password", user.getPassword());
            update.set("updated_time", java.time.LocalDateTime.now());
            if (userRepository.update(null, update) != 1) return (ApiResponse.error(409, "账号状态已变化，请重新登录"));

            Map<String, Object> result = profileBody(user);
            if (!previousPassword.equals(user.getPassword())) {
                sessions.revokeAll(user.getId());
                result.put("token", jwtUtil.generateToken(user));
            }
            return (ApiResponse.success("保存成功", result));
        } catch (IllegalArgumentException e) {
            return (ApiResponse.error(400, e.getMessage()));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新用户资料失败: ", e);
            throw new IllegalStateException("保存失败", e);
        }
    }

    public ApiResponse<?> getUserById(Long id) {
        try {
            User user = userRepository.selectById(id);
            if (user == null) {
                return (ApiResponse.error(404, "用户不存在"));
            }

            boolean student = user.getStudentNo() != null && !user.getStudentNo().isBlank();
            return (ApiResponse.success("获取成功", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                    "student", student
            )));
        } catch (Exception e) {
            log.error("获取用户信息失败: ", e);
            return (ApiResponse.error(500, "获取失败"));
        }
    }
}
