package com.hmdp;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Voucher;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest
public class HmDianPingApplicationTests {

    @Test
    public void test() {
        Voucher voucher = new Voucher()
                .setShopId(1L)
                .setTitle("100元代金券")
                .setSubTitle("周一至周日均可使用")
                .setRules("全场通用\n无需预约\n可无限叠加")
                .setPayValue(8000L)
                .setActualValue(10000L)
                .setType(1)
                .setStock(100)
                .setBeginTime(LocalDateTime.now())
                .setEndTime(LocalDateTime.now().plusDays(2));
        System.out.println(JSONUtil.toJsonStr(voucher));
    }
}
