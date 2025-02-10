package com.sky.controller.admin;


import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "店铺相关接口")
public class ShopController {

    public static final String key = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;

    @ApiOperation("设置营业状态")
    @PutMapping("/{status}")
    public Result setStatus(@PathVariable Integer status){
        log.info("设置营业状态为:{}",status == 1 ? "营业中" : "未营业");
        redisTemplate.opsForValue().set(key, status);
        return Result.success();
    }

    @ApiOperation("获取营业状态")
    @GetMapping("/status")
    public Result<Integer> getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get(key);
        log.info("获取营业状态:{}",status == 1 ? "营业中" : "未营业");
        return Result.success(status);
    }

}
