package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Component
public class CacheClient {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public <T> Result cacheQuery(String key,
                                 Supplier<T> query,
                                 Function<String, T> converter,
                                 String failInfo,
                                 long expire,
                                 TimeUnit timeUnit) {
        String json = stringRedisTemplate.opsForValue().get(key);

        if (StrUtil.isNotBlank(json))
            return Result.ok(converter.apply(json));

        if (json != null) {
            return Result.fail(failInfo);
        }

        T data = query.get();

        if (data != null) {
            String jsonStr = JSONUtil.toJsonStr(data);
            stringRedisTemplate.opsForValue().set(key, jsonStr, expire, timeUnit);
            return Result.ok(data);
        }

        stringRedisTemplate.opsForValue().set(key, "", expire, timeUnit);
        return Result.fail(failInfo);
    }
}