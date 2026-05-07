package com.example.aikb.service.admin;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.aikb.entity.User;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.UserMapper;
import com.example.aikb.security.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AdminPermissionServiceTest {

    @Mock
    private UserMapper userMapper;

    private AdminPermissionService service;

    @BeforeEach
    void setUp() {
        service = new AdminPermissionService(userMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ensureAdminRejectsUnauthenticatedRequest() {
        assertThatThrownBy(() -> service.ensureAdmin())
                .isInstanceOf(BusinessException.class)
                .extracting("code", "httpStatus")
                .containsExactly(40100, 401);
    }

    @Test
    void ensureAdminRejectsNormalUser() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(user("USER", 1));

        assertThatThrownBy(() -> service.ensureAdmin())
                .isInstanceOf(BusinessException.class)
                .extracting("code", "httpStatus")
                .containsExactly(40300, 403);
    }

    @Test
    void ensureAdminRejectsDisabledAdmin() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(user("ADMIN", 0));

        assertThatThrownBy(() -> service.ensureAdmin())
                .isInstanceOf(BusinessException.class)
                .extracting("code", "httpStatus")
                .containsExactly(40300, 403);
    }

    @Test
    void ensureAdminAllowsEnabledAdmin() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(user("ADMIN", 1));

        assertThatCode(() -> service.ensureAdmin()).doesNotThrowAnyException();
    }

    private void setCurrentUser(Long userId) {
        LoginUser loginUser = new LoginUser(userId, "alice");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    private User user(String role, Integer status) {
        User user = new User();
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
