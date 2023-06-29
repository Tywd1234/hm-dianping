package com.hmdp.controller;


import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.service.IShopTypeService;
import com.hmdp.utils.CacheClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    @Resource
    CacheClient cacheClient;

    @GetMapping("list")
    public Result queryTypeList() {
        String key = "cache:shop-type:list";
        return cacheClient.cacheQuery(key,
                () -> typeService.query().orderByAsc("sort").list(),
                JSONUtil::parseArray,
                "店铺类型不存在！");
    }
}
