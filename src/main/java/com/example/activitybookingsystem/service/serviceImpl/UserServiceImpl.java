package com.example.activitybookingsystem.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.activitybookingsystem.common.exception.BusinessException;
import com.example.activitybookingsystem.dto.LoginDTO;
import com.example.activitybookingsystem.dto.RegisterDTO;
import com.example.activitybookingsystem.entity.User;
import com.example.activitybookingsystem.mapper.UserMapper;
import com.example.activitybookingsystem.service.UserService;
import com.example.activitybookingsystem.utils.JwtUtil;
import com.example.activitybookingsystem.vo.LoginVO;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, StringRedisTemplate stringRedisTemplate) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Transactional(rollbackFor = Exception.class)//事务注解
    @Override
    public void register(RegisterDTO registerDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, registerDTO.getUsername());
        User existUser = userMapper.selectOne(queryWrapper);
        if (existUser != null) {
            throw new BusinessException("用户已经存在");
        }

        User user = new User();
        BeanUtils.copyProperties(registerDTO, user);
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
    }

    @Transactional(rollbackFor = Exception.class)//事务注解
    @Override
    public LoginVO login(LoginDTO loginDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = userMapper.selectOne(queryWrapper);
        log.info("数据库username="+ user.getUsername() + " 数据库password=" + user.getPassword());
        log.info("DTOusername=" +  loginDTO.getUsername() + " DTOpassword=" + loginDTO.getPassword());
        if (user == null) {
            throw new BusinessException("用户或密码错误"); //用户名错误
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误"); //密码错误
        }
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        System.out.println("校验成功，用户登录");
        /**
         *生成token
         */
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());

        //把token存入redis，过期时间24小时
        stringRedisTemplate.opsForValue().set("login:token:" + token, user.getId().toString(), 24, TimeUnit.MINUTES);
        return new LoginVO(token);
    }
}
