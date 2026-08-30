package com.jobradar.sandbox;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisConnectionSmokeTest implements CommandLineRunner {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisConnectionSmokeTest(
            StringRedisTemplate stringRedisTemplate) {

        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void run(String... args) {

        String key = "jobradar:test:redis";
        String value = "hello-redis";

        stringRedisTemplate
                .opsForValue()
                .set(
                        key,
                        value,
                        Duration.ofMinutes(10)
                );

        String result =
                stringRedisTemplate
                        .opsForValue()
                        .get(key);

        System.out.println(
                "Redis读取结果：" + result
        );
    }
}