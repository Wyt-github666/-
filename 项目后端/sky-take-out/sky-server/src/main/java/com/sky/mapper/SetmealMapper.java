package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SetmealMapper {

    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    @AutoFill(OperationType.INSERT)
    void insert(Setmeal setmeal);

    Page<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    @Select("select * from setmeal where id = #{id}")
    Setmeal getById(Long id);

    @Delete("delete from setmeal where id = #{id}")
    void deleteById(Long setmealId);

    void update(Setmeal setmeal);

    @Select("select * from setmeal where category_id = #{categoryId} and status = #{status}")
    List<Setmeal> list(Setmeal setmeal);

    @Select("select sd.name,sd.copies,d.image,d.description from setmeal_dish sd left join dish d on sd.dish_id = d.id where sd.setmeal_id = #{id}")
    List<DishItemVO> getDishItemBySetmealId(Long id);

    Integer countByMap(Map map);
}
