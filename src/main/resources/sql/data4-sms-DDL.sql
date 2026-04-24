USE concert_ticket;

CREATE TABLE IF NOT EXISTS `sms_verification_code` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `phone` varchar(11) NOT NULL,
  `code` varchar(10) NOT NULL,
  `expire_time` datetime NOT NULL,
  `used` tinyint(1) DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_phone_expire` (`phone`, `expire_time`),
  KEY `idx_phone_code` (`phone`, `code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信验证码表';