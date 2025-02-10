package com.sky.service;


import com.github.pagehelper.Page;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.beans.Transient;
import java.util.List;

public interface DishService {

    /**
     * 新增菜品和对应的口味
     * @param dishDTO
     */
    void saveWithFlavor(DishDTO dishDTO);

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    void deleteBash(List<Long> ids);

    DishVO findByid(Long id);

    void update(DishDTO dishDTO);

    List<DishVO> listWithflavor(Dish dish);

    List<Dish> list(Long categoryId);

    void startOrStop(Integer status, Long id);
}
