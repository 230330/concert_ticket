package com.concert.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.entity.User;
import com.concert.mapper.UserMapper;
import com.concert.service.UserService;
import org.springframework.stereotype.Service;

/**
 * @description:    用户服务实现类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

}
