package com.codewithyash.websocket_server.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {
    private final StringRedisTemplate redisTemplate;

    public void setUserIntoRedis(String username, String serverid) {
        redisTemplate.opsForValue().set(username, serverid);
        log.info("{} is mapped with user {} inside redis", serverid, username);
    }

    public boolean userExistInRedis(String username) {
        return redisTemplate.opsForValue().get(username) != null;
    }

    public void removeUser(String username) {
        redisTemplate.delete(username);
        log.info("{} user removed from redis", username);
    }
}
