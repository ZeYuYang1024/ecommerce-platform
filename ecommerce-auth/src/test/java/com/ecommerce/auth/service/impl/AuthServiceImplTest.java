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
            assertThat(captor.getValue().getPassword()).hasSize(32);
            assertThat(captor.getValue().getPassword()).isNotEqualTo("123456");
        }
    }

    @Nested
    class ProfileTests {
        @Test
        void shouldGetProfile() {
            when(userMapper.selectById(1L)).thenReturn(user);

            var vo = authService.getProfile(1L);
            assertThat(vo.getUsername()).isEqualTo(TEST_USERNAME);
            assertThat(vo.getId()).isEqualTo(1L);
        }

        @Test
        void shouldGetProfileWithNullPhone() {
            user.setPhone(null);
            when(userMapper.selectById(1L)).thenReturn(user);
            var vo = authService.getProfile(1L);
            assertThat(vo.getPhone()).isNull();
        }

        @Test
        void shouldGetProfileWithNullAvatar() {
            user.setAvatar(null);
            when(userMapper.selectById(1L)).thenReturn(user);
            var vo = authService.getProfile(1L);
            assertThat(vo.getAvatar()).isNull();
        }

        @Test
        void shouldThrowWhenProfileNotFound() {
            when(userMapper.selectById(999L)).thenReturn(null);
            assertThatThrownBy(() -> authService.getProfile(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(AuthErrorCode.USER_NOT_FOUND.getCode());
        }

        @Test
        void shouldUpdatePhone() {
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.updateById(any(User.class))).thenReturn(1);
            when(userMapper.selectById(1L)).thenReturn(user);

            var vo = authService.updateProfile(1L, "13900001111", null);
            assertThat(vo.getPhone()).isEqualTo("13900001111");
            assertThat(user.getPhone()).isEqualTo("13900001111");
        }

        @Test
        void shouldUpdateAvatar() {
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.updateById(any(User.class))).thenReturn(1);
            when(userMapper.selectById(1L)).thenReturn(user);

            var vo = authService.updateProfile(1L, null, "https://example.com/avatar.png");
            assertThat(vo.getAvatar()).isEqualTo("https://example.com/avatar.png");
        }

        @Test
        void shouldUpdateBothPhoneAndAvatar() {
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.updateById(any(User.class))).thenReturn(1);
            when(userMapper.selectById(1L)).thenReturn(user);

            var vo = authService.updateProfile(1L, "13800000000", "https://img.url");
            assertThat(vo.getPhone()).isEqualTo("13800000000");
            assertThat(vo.getAvatar()).isEqualTo("https://img.url");
        }

        @Test
        void shouldNotChangePhoneWhenNull() {
            user.setPhone("original");
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.updateById(any(User.class))).thenReturn(1);
            when(userMapper.selectById(1L)).thenReturn(user);

            var vo = authService.updateProfile(1L, null, "new_avatar");
            assertThat(vo.getPhone()).isEqualTo("original");
        }

        @Test
        void shouldNotChangeAvatarWhenNull() {
            user.setAvatar("original_avatar");
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.updateById(any(User.class))).thenReturn(1);
            when(userMapper.selectById(1L)).thenReturn(user);

            var vo = authService.updateProfile(1L, "new_phone", null);
            assertThat(vo.getAvatar()).isEqualTo("original_avatar");
        }

        @Test
        void shouldThrowWhenUpdateProfileNotFound() {
            when(userMapper.selectById(999L)).thenReturn(null);
            assertThatThrownBy(() -> authService.updateProfile(999L, "138", null))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    class AdminTypeAndMerchantIdTests {
        @Test
        void adminLogin_shouldReturnSuperAdminTypeByDefault() {
            LoginRequest req = new LoginRequest();
            req.setUsername("admin"); req.setPassword(TEST_PASSWORD);
            admin.setType(null); // default
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);

            LoginResponse resp = authService.adminLogin(req);
            assertThat(resp.getType()).isEqualTo("super_admin");
        }

        @Test
        void adminLogin_shouldReturnMerchantType() {
            LoginRequest req = new LoginRequest();
            req.setUsername("m_100"); req.setPassword(TEST_PASSWORD);
            admin.setType("merchant"); admin.setMerchantId(100L);
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);

            LoginResponse resp = authService.adminLogin(req);
            assertThat(resp.getType()).isEqualTo("merchant");
        }

        @Test
        void adminLogin_shouldExposeMerchantIdInResponse() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setUsername("m_2001");
            req.setPassword(TEST_PASSWORD);
            admin.setType("merchant");
            admin.setMerchantId(2001L);
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);

            LoginResponse resp = authService.adminLogin(req);

            Object merchantId = LoginResponse.class.getMethod("getMerchantId").invoke(resp);
            assertThat(merchantId).isEqualTo(2001L);
        }

        @Test
        void adminLogin_shouldReturnOpsType() {
            LoginRequest req = new LoginRequest();
            req.setUsername("ops_user"); req.setPassword(TEST_PASSWORD);
            admin.setType("ops"); admin.setMerchantId(null);
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);

            LoginResponse resp = authService.adminLogin(req);
            assertThat(resp.getType()).isEqualTo("ops");
        }

        @Test
        void adminLogin_shouldGenerateTokenWithType() {
            LoginRequest req = new LoginRequest();
            req.setUsername("admin"); req.setPassword(TEST_PASSWORD);
            admin.setType("super_admin");
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);

            LoginResponse resp = authService.adminLogin(req);
            String tokenType = com.ecommerce.common.util.JwtUtils.getType(resp.getToken());
            assertThat(tokenType).isEqualTo("super_admin");
        }

        @Test
        void adminLogin_shouldGenerateTokenWithMerchantId() {
            LoginRequest req = new LoginRequest();
            req.setUsername("m_100"); req.setPassword(TEST_PASSWORD);
            admin.setType("merchant"); admin.setMerchantId(100L);
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);

            LoginResponse resp = authService.adminLogin(req);
            Long mid = com.ecommerce.common.util.JwtUtils.getMerchantId(resp.getToken());
            assertThat(mid).isEqualTo(100L);
        }

        @Test
        void adminLogin_shouldNotHaveMerchantIdWhenNull() {
            LoginRequest req = new LoginRequest();
            req.setUsername("ops_user"); req.setPassword(TEST_PASSWORD);
            admin.setType("ops"); admin.setMerchantId(null);
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);

            LoginResponse resp = authService.adminLogin(req);
            Long mid = com.ecommerce.common.util.JwtUtils.getMerchantId(resp.getToken());
            assertThat(mid).isNull();
        }

        @Test
        void adminLogin_tokenShouldContainUsernameAndRole() {
            LoginRequest req = new LoginRequest();
            req.setUsername("admin"); req.setPassword(TEST_PASSWORD);
            admin.setType("super_admin");
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);

            LoginResponse resp = authService.adminLogin(req);
            var claims = com.ecommerce.common.util.JwtUtils.parse(resp.getToken());
            assertThat(claims.get("username", String.class)).isEqualTo("admin");
            assertThat(claims.get("role", String.class)).isEqualTo("admin");
        }

        @Test
        void adminLogin_shouldHandleAllRoleTypes() {
            String[] types = {"super_admin", "ops", "merchant"};
            for (String type : types) {
                admin.setType(type);
                admin.setMerchantId(type.equals("merchant") ? 1L : null);
                LoginRequest req = new LoginRequest();
                req.setUsername(type); req.setPassword(TEST_PASSWORD);
                when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);

                LoginResponse resp = authService.adminLogin(req);
                assertThat(resp.getType()).isEqualTo(type);
            }
        }
    }
}
