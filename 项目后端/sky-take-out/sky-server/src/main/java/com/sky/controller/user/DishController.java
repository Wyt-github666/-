package com.sky.controller.user;


import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/user/dish")
@RestController("userDishController")
@Slf4j
@Api("C端-菜品浏览接口")
public class DishController {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DishService dishService;

    @ApiOperation("根据分类ID查询菜品")
    @GetMapping("/list")
    public Result<List<DishVO>> list(Long categoryId) {

        // 查询redis是否存在菜品数据
        String key = "dish_" + categoryId;
        List<DishVO> dishVOList = (List<DishVO>)redisTemplate.opsForValue().get(key);
        if (dishVOList != null && !dishVOList.isEmpty()) {
            // 如果存在，直接返回查询数据库
            return Result.success(dishVOList);
        }
        // 如果不存在，查询数据库并且将数据防暑redis当中
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);
        dishVOList = dishService.listWithflavor(dish);
        redisTemplate.opsForValue().set(key, dishVOList);
        return Result.success(dishVOList);
    }

}
