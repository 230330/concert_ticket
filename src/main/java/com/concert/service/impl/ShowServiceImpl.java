package com.concert.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.entity.Show;
import com.concert.mapper.ShowMapper;
import com.concert.service.ShowService;
import org.springframework.stereotype.Service;

/**
 * 场次服务实现类
 */
@Service
public class ShowServiceImpl extends ServiceImpl<ShowMapper, Show> implements ShowService {

}
