package com.campus.secondhand.service;

import com.campus.secondhand.common.ApiResponse;
import com.campus.secondhand.dto.UserDTO;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.service.UserService;
import com.campus.secondhand.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;



@org.springframework.stereotype.Service
public class AuthApplicationService {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private static final Logger log = LoggerFactory.getLogger(AuthApplicationService.class);

    // 登录

    public ApiResponse<?> login(Map<String, String> credentials) {
        try {
            String username = credentials.get("username");
            if (username == null || username.isBlank() || credentials.get("password") == null) return (ApiResponse.error(400, "请填写登录账号和密码"));
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isEmpty() && username != null && !username.isBlank()) {
                userOpt = userService.findByStudentNo(username.trim());
            }
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String rawPassword = credentials.get("password");
                if (rawPassword == null || user.getPassword() == null
                        || !passwordEncoder.matches(rawPassword, user.getPassword())) {
                    return (ApiResponse.error(401, "用户名或密码错误"));
                }
                if (!Integer.valueOf(1).equals(user.getStatus())) {
                    return (ApiResponse.error(403, "账户已被禁用"));
                }

                // 确保用户对象有 role 字段
                if (user.getRole() == null) {
                    user.setRole(0); // 默认角色为普通用户
                }

                String token = jwtUtil.generateToken(user);
                user.setPassword(null);
                return (ApiResponse.success("登录成功", Map.of(
                        "token", token,
                        "user", user
                )));
            } else {
                return (ApiResponse.error(401, "用户名或密码错误"));
            }
        } catch (Exception e) {
            // 添加详细的日志记录
            log.error("登录失败: ", e);
            return (ApiResponse.error(500, "登录失败"));
        }
    }




    // 注册

    public ApiResponse<?> register(UserDTO userDTO) {
        try {
            if (!java.util.Objects.equals(userDTO.getPassword(), userDTO.getConfirmPassword())) {
                return (ApiResponse.error(400, "两次输入的密码不一致"));
            }
            userDTO.setUsername(userDTO.getUsername().trim());
            userDTO.setEmail(userDTO.getEmail().trim());
            if (userDTO.getUsername().length() < 3) return (ApiResponse.error(400, "用户名至少 3 个字符"));
            if (userService.existsByUsername(userDTO.getUsername())) {
                return (ApiResponse.error(70, "用户名已存在"));
            }

            if (userService.existsByEmail(userDTO.getEmail())) {
                return (ApiResponse.error(70, "邮箱已被注册"));
            }
            if (userService.existsByStudentNo(userDTO.getStudentNo().trim())) {
                return (ApiResponse.error(70, "学号已被注册"));
            }

            User user = userService.registerUser(userDTO);
            user.setPassword(null);
            return (ApiResponse.success("注册成功", user));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return (ApiResponse.error(70, "用户名、邮箱或学号已被使用"));
        } catch (Exception e) {
            return (ApiResponse.error(500, "注册失败"));
        }
    }

    public ApiResponse<?> forgotPassword() {
        return (ApiResponse.error(400, "未开通邮件重置，请登录后在个人中心修改密码"));
    }



}
