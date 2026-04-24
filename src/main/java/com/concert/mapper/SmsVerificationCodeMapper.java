package com.concert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.concert.entity.SmsVerificationCode;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description:    短信验证码Mapper接口
 * @author: hzf
 * @date: 2026年04月24日 10:16
 * @version: 1.0
 */
@Mapper
public interface SmsVerificationCodeMapper extends BaseMapper<SmsVerificationCode> {

}
