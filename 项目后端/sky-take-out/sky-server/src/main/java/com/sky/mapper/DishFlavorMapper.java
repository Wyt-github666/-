package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    void insertBatch(List<DishFlavor> flavors);

    @Delete("delete from dish_flavor where dish_id = #{id}")
    void deleteBydishId(Long id);

    void deleteBydishIds(List<Long> dishIds);

    @Select("select * from dish_flavor where dish_id = #{id}")
    List<DishFlavor> getFlavorByDishid(Long id);
}
