package com.campus.secondhand.service;

import com.campus.secondhand.dto.UserDTO;
import com.campus.secondhand.entity.User;
import java.util.Optional;

/**
 * 用户服务接口
 */
public interface UserService {
    /**
     * 注册新用户
     * @param userDTO 用户注册信息
     * @return 注册后的用户实体
     * @throws RuntimeException 当用户名或邮箱已存在时抛出异常
     */
    User registerUser(UserDTO userDTO);

    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户实体对象
     */
    Optional<User> findByUsername(String username);

    Optional<User> findByStudentNo(String studentNo);

    boolean existsByStudentNo(String studentNo);

    /**
     * 根据邮箱查找用户
     * @param email 邮箱地址
     * @return 用户实体对象
     */
    Optional<User> findByEmail(String email);

    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     * @param email 邮箱地址
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 更新用户信息
     * @param user 用户实体
     * @return 更新后的用户实体
     */
    User updateUser(User user);
}
