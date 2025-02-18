package com.team5.pyeonjip.global.config;

import org.redisson.api.RedissonClient;
import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6380"); // Redis 서버 주소
        return Redisson.create(config);
    }
}
