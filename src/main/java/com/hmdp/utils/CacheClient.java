package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Component
public class CacheClient {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        // 设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

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

    public <T> T queryWithMutex(
            String queryKey,
            String lockKey,
            Supplier<T> query,
            Function<String, T> converter,
            long expire,
            TimeUnit timeUnit
    ) {
        String json = stringRedisTemplate.opsForValue().get(queryKey);

        if (StrUtil.isNotBlank(json))
            return converter.apply(json);

        if (json != null) {
            return null;
        }

        // 重新查询
        boolean success = tryLock(lockKey);

        if (success) {
            try {
                T data = query.get();
                if (data != null) {
                    String jsonStr = JSONUtil.toJsonStr(data);
                    stringRedisTemplate.opsForValue().set(queryKey, jsonStr, expire, timeUnit);
                    return data;
                }

                stringRedisTemplate.opsForValue().set(queryKey, "", expire, timeUnit);
                return null;
            } finally {
                unlock(lockKey);
            }
        } else {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return queryWithMutex(
                    queryKey,
                    lockKey,
                    query,
                    converter,
                    expire,
                    timeUnit
            );
        }
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public <T> T queryWithLogicalExpire(
            String queryKey,
            String lockKey,
            Supplier<T> query,
            Function<String, T> converter,
            long expire,
            TimeUnit timeUnit
    ) {
        String json = stringRedisTemplate.opsForValue().get(queryKey);


        if (json == null) {
            while ((json = stringRedisTemplate.opsForValue().get(queryKey)) == null) {
                boolean success = tryLock(lockKey);
                if (success) {
                    try {
                        T data = query.get();
                        if (data == null) {
                            stringRedisTemplate.opsForValue().set(queryKey, "", expire, timeUnit);
                            return null;
                        }
                        setWithLogicalExpire(queryKey, data, expire, timeUnit);
                        return data;
                    } finally {
                        unlock(lockKey);
                    }
                }
            }
        }

        // 空对象
        if (StrUtil.isBlank(json)) {
            return null;
        }

        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        //过期了，重建缓存
        if (redisData.getExpireTime().isBefore(LocalDateTime.now())) {
            boolean success = tryLock(lockKey);
            if (success) {
                CACHE_REBUILD_EXECUTOR.submit(() -> {
                    try {
                        T data = query.get();
                        if (data == null) {
                            stringRedisTemplate.opsForValue().set(queryKey, "", expire, timeUnit);
                        }
                        setWithLogicalExpire(queryKey, data, expire, timeUnit);
                    } finally {
                        unlock(lockKey);
                    }
                });
            }
        }
        // 未过期 / 获取锁未成功 / 返回旧数据
        return converter.apply(JSONUtil.toJsonStr(redisData.getData()));
    }
}