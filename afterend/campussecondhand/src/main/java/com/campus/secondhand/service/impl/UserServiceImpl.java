package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.secondhand.dto.UserDTO;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.repository.UserRepository;
import com.campus.secondhand.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User registerUser(UserDTO userDTO) {
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setStudentNo(userDTO.getStudentNo().trim());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
//        user.setPhone(userDTO.getPhone());
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());

        userRepository.insert(user);
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        User user = userRepository.selectOne(wrapper);
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<User> findByStudentNo(String studentNo) {
        return userRepository.findByStudentNo(studentNo);
    }

    @Override
    public boolean existsByStudentNo(String studentNo) {
        return userRepository.findByStudentNo(studentNo).isPresent();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        User user = userRepository.selectOne(wrapper);
        return Optional.ofNullable(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        return userRepository.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        return userRepository.selectCount(wrapper) > 0;
    }


    @Override
    public User updateUser(User user) {
        user.setUpdatedTime(LocalDateTime.now());
        userRepository.updateById(user);
        return user;
    }
}
