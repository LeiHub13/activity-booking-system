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
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final long LOGIN_EXPIRE_HOURS = 24;
    private static final String LOGIN_TOKEN_KEY_PREFIX = "login:token:";
    private static final String LOGIN_USER_KEY_PREFIX = "login:user:";

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

        List<String> roleCodes = roleMapper.selectRoleCodesByUserId(user.getId());
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BusinessException("当前用户未分配角色");
        }

        String role = roleCodes.contains("ADMIN") ? "ROLE_ADMIN" : "ROLE_" + roleCodes.get(0);
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), role);
        saveLoginToken(user.getId(), token);

        return new LoginVO(token);
    }

    @Override
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }

        String userId = stringRedisTemplate.opsForValue().get(LOGIN_TOKEN_KEY_PREFIX + token);
        stringRedisTemplate.delete(LOGIN_TOKEN_KEY_PREFIX + token);

        if (StringUtils.hasText(userId)) {
            String userKey = LOGIN_USER_KEY_PREFIX + userId;
            String currentToken = stringRedisTemplate.opsForValue().get(userKey);
            if (token.equals(currentToken)) {
                stringRedisTemplate.delete(userKey);
            }
            return;
        }

        try {
            Claims claims = JwtUtil.parseToken(token);
            Long tokenUserId = claims.get("userId", Long.class);
            if (tokenUserId != null) {
                String userKey = LOGIN_USER_KEY_PREFIX + tokenUserId;
                String currentToken = stringRedisTemplate.opsForValue().get(userKey);
                if (token.equals(currentToken)) {
                    stringRedisTemplate.delete(userKey);
                }
            }
        } catch (Exception ignored) {
            // token 已损坏或过期时，只需要确保 token key 被删除。
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

    private void saveLoginToken(Long userId, String token) {
        String userKey = LOGIN_USER_KEY_PREFIX + userId;
        String oldToken = stringRedisTemplate.opsForValue().get(userKey);
        if (StringUtils.hasText(oldToken)) {
            stringRedisTemplate.delete(LOGIN_TOKEN_KEY_PREFIX + oldToken);
        }

        stringRedisTemplate.opsForValue().set(
                LOGIN_TOKEN_KEY_PREFIX + token,
                userId.toString(),
                LOGIN_EXPIRE_HOURS,
                TimeUnit.HOURS
        );
        stringRedisTemplate.opsForValue().set(
                userKey,
                token,
                LOGIN_EXPIRE_HOURS,
                TimeUnit.HOURS
        );
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
