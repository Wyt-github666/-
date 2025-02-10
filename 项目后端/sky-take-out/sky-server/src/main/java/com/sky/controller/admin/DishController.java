package com.sky.controller.admin;


import com.github.pagehelper.Page;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.License;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Api(tags = "菜品相关的接口")
public class DishController {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DishService dishService;

    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        String key = "dish_" + dishDTO.getCategoryId();
        cleanChache(key);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO queryDTO) {
        log.info("菜品分页查询：{}", queryDTO);
        PageResult pageResult = dishService.pageQuery(queryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping
    @ApiOperation("菜品批量删除")
    public Result delete(@RequestParam List<Long> ids) {
        log.info("批量删除菜品：{}",ids);
        dishService.deleteBash(ids);
        cleanChache("dish_*");
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据ID查询菜品")
    public Result<DishVO> findById(@PathVariable Long id) {
        log.info("根据ID查询菜品：{}",id);
        DishVO dishVO = dishService.findByid(id);
        return Result.success(dishVO);
    }

    @PutMapping
    @ApiOperation("修改菜品数据")
    public Result update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品数据:{}",dishDTO);
        dishService.update(dishDTO);
        cleanChache("dish_*");
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List< Dish>> list(Long categoryId){
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }

    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售停售")
    public Result<String> startOrStop(@PathVariable Integer status, Long id){
        dishService.startOrStop(status,id);
        cleanChache("dish_*");
        return Result.success();
    }

    private void cleanChache(String patten){
        Set keys = redisTemplate.keys(patten);
        redisTemplate.delete(keys);
    }

}
