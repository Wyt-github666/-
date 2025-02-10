package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private DishFlavorMapper dflMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        // 向菜品表插入一条数据
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);
        // 通过XML自增回显，返回菜品ID
        Long dishId = dish.getId();
        // 向口味表插入n条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(flavor -> {
                flavor.setDishId(dishId);
            });
            dflMapper.insertBatch(flavors);
        }
    }

    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    @Transactional
    public void deleteBash(List<Long> ids) {
        // 判断菜品是否在销售当中，判断菜品是否关联套餐
        ids.forEach(id -> {
           Dish dish = dishMapper.getById(id);
           if(dish.getStatus() == StatusConstant.ENABLE){
               throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
           }
           List<Long> setmealsIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
           if(setmealsIds != null && setmealsIds.size() > 0){
               throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
           }
        });
        // 如果可以删除，删除菜品表的数据和口味表中关联的数据
//        ids.forEach(id -> {
//            dishMapper.deleteById(id);
//            dishFlavorMapper.deleteBydishId(id);
//        });
        // 根据菜品id批量删除菜品数据
        dishMapper.deleteByIds(ids);
        // 根据菜品id批量删除口味数据
        dishFlavorMapper.deleteBydishIds(ids);
    }

    @Transactional
    public DishVO findByid(Long id) {
        Dish dish = dishMapper.getById(id);
        DishVO dishVO = new DishVO();
        Long categoryId = dish.getCategoryId();
        BeanUtils.copyProperties(dish, dishVO);
        Category category = categoryMapper.getNameByid(categoryId);
        List<DishFlavor> dishFlavorList = dishFlavorMapper.getFlavorByDishid(id);
        dishVO.setFlavors(dishFlavorList);
        dishVO.setCategoryName(category.getName());
        return dishVO;
    }

    @Transactional
    public void update(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 修改菜品的基本信息
        dishMapper.update(dish);
        dishFlavorMapper.deleteBydishId(dishDTO.getId());
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(flavor -> {
                flavor.setDishId(dish.getId());
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    @Override
    public List<DishVO> listWithflavor(Dish dish) {
        List<Dish> dishList = dishMapper.listGetBycategoryId(dish);
        List<DishVO> dishVOList = new ArrayList<>();
        dishList.forEach(d -> {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d, dishVO);
            List<DishFlavor> flavors = dishFlavorMapper.getFlavorByDishid(d.getId());
            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        });
        return dishVOList;
    }

    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }


    @Transactional
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);

        if (status == StatusConstant.DISABLE) {
            // 如果是停售操作，还需要将包含当前菜品的套餐也停售
            List<Long> dishIds = new ArrayList<>();
            dishIds.add(id);
            // select setmeal_id from setmeal_dish where dish_id in (?,?,?)
            List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(dishIds);
            if (setmealIds != null && setmealIds.size() > 0) {
                for (Long setmealId : setmealIds) {
                    Setmeal setmeal = Setmeal.builder()
                            .id(setmealId)
                            .status(StatusConstant.DISABLE)
                            .build();
                    setmealMapper.update(setmeal);
                }
            }
        }
    }

}
