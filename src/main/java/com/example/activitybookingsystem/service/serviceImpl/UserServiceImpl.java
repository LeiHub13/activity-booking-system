package com.example.activitybookingsystem.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.activitybookingsystem.common.exception.BusinessException;
import com.example.activitybookingsystem.dto.LoginDTO;
import com.example.activitybookingsystem.dto.RegisterDTO;
import com.example.activitybookingsystem.dto.UpdateUserInfoDTO;
import com.example.activitybookingsystem.entity.Role;
import com.example.activitybookingsystem.entity.User;
import com.example.activitybookingsystem.entity.UserRole;
import com.example.activitybookingsystem.mapper.RoleMapper;
import com.example.activitybookingsystem.mapper.UserMapper;
import com.example.activitybookingsystem.mapper.UserRoleMapper;
import com.example.activitybookingsystem.service.UserService;
import com.example.activitybookingsystem.utils.JwtUtil;
import com.example.activitybookingsystem.vo.LoginVO;
import com.example.activitybookingsystem.vo.UserInfoVO;
import io.jsonwebtoken.Claims;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String LOGIN_ACCESS_TOKEN_KEY_PREFIX = "login:access:";
    private static final String LOGIN_REFRESH_TOKEN_KEY_PREFIX = "login:refresh:";
    private static final String LOGIN_USER_ACCESS_KEY_PREFIX = "login:user:access:";
    private static final String LOGIN_USER_REFRESH_KEY_PREFIX = "login:user:refresh:";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    public UserServiceImpl(UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           StringRedisTemplate stringRedisTemplate,
                           UserRoleMapper userRoleMapper,
                           RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.stringRedisTemplate = stringRedisTemplate;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO registerDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, registerDTO.getUsername());
        User existUser = userMapper.selectOne(queryWrapper);
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        BeanUtils.copyProperties(registerDTO, user);
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        Role role = roleMapper.selectByRoleCode("USER");
        if (role == null) {
            throw new BusinessException("默认用户角色不存在");
        }

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        String role = getUserRole(user.getId());
        String accessToken = JwtUtil.generateAccessToken(user.getId(), user.getUsername(), role);
        String refreshToken = JwtUtil.generateRefreshToken(user.getId(), user.getUsername());
        saveLoginTokens(user.getId(), accessToken, refreshToken);

        return buildLoginVO(accessToken, refreshToken);
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException("刷新 token 不能为空");
        }

        Claims claims;
        try {
            claims = JwtUtil.parseToken(refreshToken);
        } catch (Exception e) {
            throw new BusinessException("刷新 token 已过期或无效");
        }

        String tokenType = claims.get("tokenType", String.class);
        if (!JwtUtil.TOKEN_TYPE_REFRESH.equals(tokenType)) {
            throw new BusinessException("token 类型不正确");
        }

        Long userId = claims.get("userId", Long.class);
        if (userId == null) {
            throw new BusinessException("刷新 token 无效");
        }

        String redisUserId = stringRedisTemplate.opsForValue().get(LOGIN_REFRESH_TOKEN_KEY_PREFIX + refreshToken);
        String currentRefreshToken = stringRedisTemplate.opsForValue().get(LOGIN_USER_REFRESH_KEY_PREFIX + userId);
        if (!Objects.equals(String.valueOf(userId), redisUserId) || !refreshToken.equals(currentRefreshToken)) {
            throw new BusinessException("刷新 token 已失效");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("当前用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        String role = getUserRole(user.getId());
        String accessToken = JwtUtil.generateAccessToken(user.getId(), user.getUsername(), role);
        refreshAccessToken(user.getId(), accessToken);
        return buildLoginVO(accessToken, refreshToken);
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        Long userId = resolveUserIdFromRedis(LOGIN_ACCESS_TOKEN_KEY_PREFIX, accessToken);
        if (userId == null) {
            userId = resolveUserIdFromRedis(LOGIN_REFRESH_TOKEN_KEY_PREFIX, refreshToken);
        }

        if (StringUtils.hasText(accessToken)) {
            stringRedisTemplate.delete(LOGIN_ACCESS_TOKEN_KEY_PREFIX + accessToken);
        }
        if (StringUtils.hasText(refreshToken)) {
            stringRedisTemplate.delete(LOGIN_REFRESH_TOKEN_KEY_PREFIX + refreshToken);
        }

        if (userId != null) {
            deleteCurrentToken(LOGIN_USER_ACCESS_KEY_PREFIX + userId, LOGIN_ACCESS_TOKEN_KEY_PREFIX, accessToken);
            deleteCurrentToken(LOGIN_USER_REFRESH_KEY_PREFIX + userId, LOGIN_REFRESH_TOKEN_KEY_PREFIX, refreshToken);
        }
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        User user = getCurrentUserEntity();
        return toUserInfoVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO updateCurrentUserInfo(UpdateUserInfoDTO updateUserInfoDTO) {
        User user = getCurrentUserEntity();

        user.setRealName(trimToNull(updateUserInfoDTO.getRealName()));
        user.setEmail(trimToNull(updateUserInfoDTO.getEmail()));
        user.setPhone(trimToNull(updateUserInfoDTO.getPhone()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        return toUserInfoVO(user);
    }

    private void saveLoginTokens(Long userId, String accessToken, String refreshToken) {
        String userAccessKey = LOGIN_USER_ACCESS_KEY_PREFIX + userId;
        String userRefreshKey = LOGIN_USER_REFRESH_KEY_PREFIX + userId;

        String oldAccessToken = stringRedisTemplate.opsForValue().get(userAccessKey);
        if (StringUtils.hasText(oldAccessToken)) {
            stringRedisTemplate.delete(LOGIN_ACCESS_TOKEN_KEY_PREFIX + oldAccessToken);
        }
        String oldRefreshToken = stringRedisTemplate.opsForValue().get(userRefreshKey);
        if (StringUtils.hasText(oldRefreshToken)) {
            stringRedisTemplate.delete(LOGIN_REFRESH_TOKEN_KEY_PREFIX + oldRefreshToken);
        }

        saveAccessToken(userId, accessToken);
        stringRedisTemplate.opsForValue().set(
                LOGIN_REFRESH_TOKEN_KEY_PREFIX + refreshToken,
                userId.toString(),
                JwtUtil.REFRESH_TOKEN_EXPIRE_SECONDS,
                TimeUnit.SECONDS
        );
        stringRedisTemplate.opsForValue().set(
                userRefreshKey,
                refreshToken,
                JwtUtil.REFRESH_TOKEN_EXPIRE_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private void refreshAccessToken(Long userId, String accessToken) {
        String userAccessKey = LOGIN_USER_ACCESS_KEY_PREFIX + userId;
        String oldAccessToken = stringRedisTemplate.opsForValue().get(userAccessKey);
        if (StringUtils.hasText(oldAccessToken)) {
            stringRedisTemplate.delete(LOGIN_ACCESS_TOKEN_KEY_PREFIX + oldAccessToken);
        }
        saveAccessToken(userId, accessToken);
    }

    private void saveAccessToken(Long userId, String accessToken) {
        stringRedisTemplate.opsForValue().set(
                LOGIN_ACCESS_TOKEN_KEY_PREFIX + accessToken,
                userId.toString(),
                JwtUtil.ACCESS_TOKEN_EXPIRE_SECONDS,
                TimeUnit.SECONDS
        );
        stringRedisTemplate.opsForValue().set(
                LOGIN_USER_ACCESS_KEY_PREFIX + userId,
                accessToken,
                JwtUtil.ACCESS_TOKEN_EXPIRE_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private void deleteCurrentToken(String userTokenKey, String tokenKeyPrefix, String providedToken) {
        String currentToken = stringRedisTemplate.opsForValue().get(userTokenKey);
        if (!StringUtils.hasText(currentToken)) {
            return;
        }
        if (!StringUtils.hasText(providedToken) || currentToken.equals(providedToken)) {
            stringRedisTemplate.delete(tokenKeyPrefix + currentToken);
            stringRedisTemplate.delete(userTokenKey);
        }
    }

    private Long resolveUserIdFromRedis(String tokenKeyPrefix, String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        String userId = stringRedisTemplate.opsForValue().get(tokenKeyPrefix + token);
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        return Long.valueOf(userId);
    }

    private LoginVO buildLoginVO(String accessToken, String refreshToken) {
        return LoginVO.of(
                accessToken,
                refreshToken,
                JwtUtil.ACCESS_TOKEN_EXPIRE_SECONDS,
                JwtUtil.REFRESH_TOKEN_EXPIRE_SECONDS
        );
    }

    private String getUserRole(Long userId) {
        List<String> roleCodes = roleMapper.selectRoleCodesByUserId(userId);
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BusinessException("当前用户未分配角色");
        }
        return roleCodes.contains("ADMIN") ? "ROLE_ADMIN" : "ROLE_" + roleCodes.get(0);
    }

    private User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException("当前用户未登录");
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, authentication.getName());
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException("当前用户不存在");
        }
        return user;
    }

    private UserInfoVO toUserInfoVO(User user) {
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfoVO);
        return userInfoVO;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
