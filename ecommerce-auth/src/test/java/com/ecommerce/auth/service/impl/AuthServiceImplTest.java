package com.ecommerce.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.auth.common.AuthErrorCode;
import com.ecommerce.auth.dto.request.LoginRequest;
import com.ecommerce.auth.dto.request.RegisterRequest;
import com.ecommerce.auth.dto.response.LoginResponse;
import com.ecommerce.auth.entity.AdminUser;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.mapper.AdminUserMapper;
import com.ecommerce.auth.mapper.UserMapper;
import com.ecommerce.common.result.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private AdminUserMapper adminUserMapper;
    @InjectMocks private AuthServiceImpl authService;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "123456";
    // MD5 of "123456"
    private static final String MD5_PASSWORD = "e10adc3949ba59abbe56e057f20f883e";

    private User user;
    private AdminUser admin;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername(TEST_USERNAME);
        user.setPassword(MD5_PASSWORD);
        user.setStatus(1);

        admin = new AdminUser();
        admin.setId(100L);
        admin.setUsername("admin");
        admin.setPassword(MD5_PASSWORD);
        admin.setStatus(1);
    }

    @Nested
    class RegisterTests {

        @Test
        void register_shouldCreateUserAndReturnToken() {
            RegisterRequest req = new RegisterRequest();
            req.setUsername(TEST_USERNAME);
            req.setPassword(TEST_PASSWORD);
            when(userMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(userMapper.insert(any(User.class))).thenReturn(1);

            LoginResponse resp = authService.register(req);

            assertThat(resp.getUsername()).isEqualTo(TEST_USERNAME);
            assertThat(resp.getUserId()).isNotNull();
            assertThat(resp.getToken()).isNotBlank();
        }

        @Test
        void register_shouldThrow_whenUsernameExists() {
            RegisterRequest req = new RegisterRequest();
            req.setUsername(TEST_USERNAME);
            req.setPassword(TEST_PASSWORD);
            when(userMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(AuthErrorCode.USERNAME_DUPLICATE.getCode());
            verify(userMapper, never()).insert(any(User.class));
        }
    }

    @Nested
    class LoginTests {

        @Test
        void login_shouldReturnToken_whenCredentialsCorrect() {
            LoginRequest req = new LoginRequest();
            req.setUsername(TEST_USERNAME);
            req.setPassword(TEST_PASSWORD);
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

            LoginResponse resp = authService.login(req);

            assertThat(resp.getUsername()).isEqualTo(TEST_USERNAME);
            assertThat(resp.getToken()).isNotBlank();
        }

        @Test
        void login_shouldThrow_whenUserNotFound() {
            LoginRequest req = new LoginRequest();
            req.setUsername("nobody");
            req.setPassword(TEST_PASSWORD);
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(AuthErrorCode.USER_NOT_FOUND.getCode());
        }

        @Test
        void login_shouldThrow_whenPasswordWrong() {
            LoginRequest req = new LoginRequest();
            req.setUsername(TEST_USERNAME);
            req.setPassword("wrongpass");
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(AuthErrorCode.PASSWORD_ERROR.getCode());
        }

        @Test
        void login_shouldThrow_whenUserForbidden() {
            user.setStatus(0);
            LoginRequest req = new LoginRequest();
            req.setUsername(TEST_USERNAME);
            req.setPassword(TEST_PASSWORD);
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(AuthErrorCode.USER_FORBIDDEN.getCode());
        }
    }

    @Nested
    class AdminLoginTests {

        @Test
        void adminLogin_shouldReturnToken_whenCredentialsCorrect() {
            LoginRequest req = new LoginRequest();
            req.setUsername("admin");
            req.setPassword(TEST_PASSWORD);
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);

            LoginResponse resp = authService.adminLogin(req);

            assertThat(resp.getUsername()).isEqualTo("admin");
            assertThat(resp.getToken()).isNotBlank();
        }

        @Test
        void adminLogin_shouldThrow_whenAdminNotFound() {
            LoginRequest req = new LoginRequest();
            req.setUsername("nobody");
            req.setPassword(TEST_PASSWORD);
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertThatThrownBy(() -> authService.adminLogin(req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(AuthErrorCode.ADMIN_NOT_FOUND.getCode());
        }

        @Test
        void adminLogin_shouldThrow_whenPasswordWrong() {
            LoginRequest req = new LoginRequest();
            req.setUsername("admin");
            req.setPassword("wrongpass");
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);

            assertThatThrownBy(() -> authService.adminLogin(req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(AuthErrorCode.ADMIN_PASSWORD_ERROR.getCode());
        }

        @Test
        void adminLogin_shouldThrow_whenAdminForbidden() {
            admin.setStatus(0);
            LoginRequest req = new LoginRequest();
            req.setUsername("admin");
            req.setPassword(TEST_PASSWORD);
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);

            assertThatThrownBy(() -> authService.adminLogin(req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(AuthErrorCode.ADMIN_FORBIDDEN.getCode());
        }
    }

    @Nested
    class ValidateTokenTests {

        @Test
        void validateToken_shouldStripBearerAndReturnUserId() {
            String token = com.ecommerce.common.util.JwtUtils.generate(1L, TEST_USERNAME, "user");

            Long userId = authService.validateToken("Bearer " + token);

            assertThat(userId).isEqualTo(1L);
        }

        @Test
        void validateToken_shouldThrow_whenTokenInvalid() {
            assertThatThrownBy(() -> authService.validateToken("invalid-token"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(AuthErrorCode.TOKEN_INVALID.getCode());
        }
    }

    @Nested
    class BoundaryTests {

        @Test
        void register_shouldWorkWithMinLengthUsername() {
            RegisterRequest req = new RegisterRequest();
            req.setUsername("abc"); // exactly 3 chars (min)
            req.setPassword("123456");
            when(userMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(userMapper.insert(any(User.class))).thenReturn(1);

            LoginResponse resp = authService.register(req);

            assertThat(resp.getUsername()).isEqualTo("abc");
            assertThat(resp.getToken()).isNotBlank();
        }

        @Test
        void register_shouldWorkWithMaxLengthUsername() {
            String maxUsername = "abcdefghijklmnopqrstuvwxyz012345"; // exactly 32 chars
            RegisterRequest req = new RegisterRequest();
            req.setUsername(maxUsername);
            req.setPassword("123456");
            when(userMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(userMapper.insert(any(User.class))).thenReturn(1);

            LoginResponse resp = authService.register(req);

            assertThat(resp.getUsername()).isEqualTo(maxUsername);
        }

        @Test
        void register_shouldWorkWithMinLengthPassword() {
            RegisterRequest req = new RegisterRequest();
            req.setUsername("newuser");
            req.setPassword("123456"); // exactly 6 chars (min)
            when(userMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(userMapper.insert(any(User.class))).thenReturn(1);

            LoginResponse resp = authService.register(req);

            assertThat(resp.getUsername()).isEqualTo("newuser");
        }

        @Test
        void register_shouldWorkWithNullPhone() {
            RegisterRequest req = new RegisterRequest();
            req.setUsername("nophone");
            req.setPassword("123456");
            req.setPhone(null);
            when(userMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(userMapper.insert(any(User.class))).thenReturn(1);

            assertThatCode(() -> authService.register(req)).doesNotThrowAnyException();
        }

        @Test
        void login_shouldPass_whenStatusIsNull() {
            user.setStatus(null);
            LoginRequest req = new LoginRequest();
            req.setUsername(TEST_USERNAME);
            req.setPassword(TEST_PASSWORD);
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

            // status=null should NOT trigger forbidden (null != 0 is false)
            LoginResponse resp = authService.login(req);

            assertThat(resp.getUsername()).isEqualTo(TEST_USERNAME);
        }

        @Test
        void login_shouldHandleUsernameWithSpecialChars() {
            user.setUsername("test.user@domain");
            LoginRequest req = new LoginRequest();
            req.setUsername("test.user@domain");
            req.setPassword(TEST_PASSWORD);
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

            LoginResponse resp = authService.login(req);

            assertThat(resp.getUsername()).isEqualTo("test.user@domain");
        }

        @Test
        void validateToken_shouldWorkWithoutBearerPrefix() {
            String token = com.ecommerce.common.util.JwtUtils.generate(1L, TEST_USERNAME, "user");

            Long userId = authService.validateToken(token); // no "Bearer " prefix

            assertThat(userId).isEqualTo(1L);
        }

        @Test
        void validateToken_shouldThrow_whenTokenEmpty() {
            assertThatThrownBy(() -> authService.validateToken(""))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void validateToken_shouldThrow_whenOnlyBearerPrefix() {
            // "Bearer " with nothing after
            assertThatThrownBy(() -> authService.validateToken("Bearer "))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void adminLogin_shouldPass_whenStatusIsNull() {
            admin.setStatus(null);
            LoginRequest req = new LoginRequest();
            req.setUsername("admin");
            req.setPassword(TEST_PASSWORD);
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);

            LoginResponse resp = authService.adminLogin(req);

            assertThat(resp.getUsername()).isEqualTo("admin");
        }

        @Test
        void register_shouldMd5EncodePassword() {
            RegisterRequest req = new RegisterRequest();
            req.setUsername("hashuser");
            req.setPassword("123456");
            when(userMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(userMapper.insert(any(User.class))).thenReturn(1);

            authService.register(req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userMapper).insert(captor.capture());
            // MD5 should be 32 hex chars, not plaintext
            assertThat(captor.getValue().getPassword()).hasSize(32);
            assertThat(captor.getValue().getPassword()).isNotEqualTo("123456");
        }
    }
}
