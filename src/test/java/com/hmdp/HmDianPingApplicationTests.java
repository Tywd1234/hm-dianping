package com.hmdp;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@RunWith(SpringRunner.class)
public class HmDianPingApplicationTests {

    @Resource
    RedissonClient redissonClient;

    @Test
    public void testRedisson() throws Exception {
        //获取锁(可重入)，指定锁的名称
        RLock lock = redissonClient.getLock("anyLock");
        //尝试获取锁，参数分别是：获取锁的最大等待时间(期间会重试)，锁自动释放时间，时间单位
        boolean isLock = lock.tryLock(1, 10, TimeUnit.SECONDS);
        //判断获取锁成功
        if (isLock) {
            try {
                System.out.println("执行业务");
            } finally {
                //释放锁
                lock.unlock();
            }

        }
    }

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    IUserService userService;
    // 生成token
    @Test
    public void createTokens() throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream("tokens.txt");
        PrintStream printStream = new PrintStream(fileOutputStream);

        Page<User> page = new Page<>(0, 1000);
        userService.page(page, null);
        page.getRecords().forEach(e -> {
            String token = UUID.randomUUID().toString(true);
            UserDTO userDTO = BeanUtil.copyProperties(e, UserDTO.class);
            Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                    CopyOptions.create()
                            .setIgnoreNullValue(true)
                            .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
            String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
            stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
            printStream.println(token);
        });

        printStream.close();
        fileOutputStream.close();
    }
}
