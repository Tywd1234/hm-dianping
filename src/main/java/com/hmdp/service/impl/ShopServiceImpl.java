package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    CacheClient cacheClient;

    @Resource
    StringRedisTemplate stringRedisTemplate;


    @Override
    public Result queryById(Long id) {
        /*
        return cacheClient.cacheQuery(
                RedisConstants.CACHE_SHOP_KEY + id,
                () -> getById(id),
                json -> JSONUtil.toBean(json, Shop.class),
                "店铺不存在！",
                30,
                TimeUnit.MINUTES);
         */

//        Shop shop = cacheClient.queryWithMutex(
//                RedisConstants.CACHE_SHOP_KEY + id,
//                RedisConstants.LOCK_SHOP_KEY + id,
//                () -> getById(id),
//                json -> JSONUtil.toBean(json, Shop.class),
//                30,
//                TimeUnit.MINUTES);


        Shop shop = cacheClient.queryWithLogicalExpire(
                RedisConstants.CACHE_SHOP_KEY + id,
                RedisConstants.LOCK_SHOP_KEY + id,
                () -> getById(id),
                json -> JSONUtil.toBean(json, Shop.class),
                30,
                TimeUnit.MINUTES);

        if (shop == null)
            return Result.fail("店铺不存在！");
        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        if (shop.getId() == null)
            return Result.fail("id不能为空");
        // 写入数据库
        updateById(shop);
        // 删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());

        return Result.ok();
    }
}
