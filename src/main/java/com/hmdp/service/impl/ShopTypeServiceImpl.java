package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import org.springframework.stereotype.Service;

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
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    CacheClient cacheClient;

    @Override
    public Result queryTypeList() {
        return cacheClient.cacheQuery(
                RedisConstants.CACHE_SHOP_TYPE_LIST_KEY,
                () -> query().orderByAsc("sort").list(),
                JSONUtil::parseArray,
                "店铺类型不存在！",
                30,
                TimeUnit.MINUTES);
    }
}
