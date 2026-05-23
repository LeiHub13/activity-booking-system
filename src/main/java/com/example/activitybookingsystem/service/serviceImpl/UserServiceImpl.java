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
            throw new BusinessException("Username already exists");
        }

        User user = new User();
        BeanUtils.copyProperties(registerDTO, user);
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        // New users are bound to the USER role by default.
        Role role = roleMapper.selectByRoleCode("USER");
        if (role == null) {
            throw new BusinessException("Default role USER not found");
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
            throw new BusinessException("Invalid username or password");
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("Invalid username or password");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("Account is disabled");
        }

        List<String> roleCodes = roleMapper.selectRoleCodesByUserId(user.getId());
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BusinessException("Current user has no role assigned");
        }

        // Put role into JWT so the filter can rebuild Spring Security authorities.
        String role = roleCodes.contains("ADMIN") ? "ROLE_ADMIN" : "ROLE_" + roleCodes.get(0);
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), role);
        // Keep a server-side token record to support logout and forced invalidation later.
        stringRedisTemplate.opsForValue().set(
                "login:token:" + token,
                user.getId().toString(),
                24,
                TimeUnit.HOURS
        );

        return new LoginVO(token);
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

    private User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException("Current user not found");
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, authentication.getName());
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException("Current user does not exist");
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
