package com.sky.controller.user;


import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.PublicKey;

@Slf4j
@RestController("userShopController")
@RequestMapping("/user/shop")
@Api(tags = "C端店铺相关接口")
public class ShopController {

    public static final String key = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;

    @ApiOperation("用户获取店铺状态")
    @GetMapping("/status")
    public Result<Integer> getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get(key);
        log.info("获取营业状态:{}",status == 1 ? "营业中" : "未营业");
        return Result.success(status);
    }

}
