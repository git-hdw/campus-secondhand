package com.campus.secondhand.controller;

import com.campus.secondhand.dto.UserDTO;
import com.campus.secondhand.service.UserService;
import com.campus.secondhand.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock UserService users;
    @InjectMocks com.campus.secondhand.service.AuthApplicationService controller;

    @Test void refusesMismatchedPasswordsBeforeCreatingAccount() {
        UserDTO dto = new UserDTO();
        dto.setPassword("abcdef"); dto.setConfirmPassword("different");
        ApiResponse<?> response = controller.register(dto);
        assertNotNull(response);
        assertEquals(400, response.getCode());
        verifyNoInteractions(users);
    }
}
