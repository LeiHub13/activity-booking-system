package com.example.activitybookingsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.activitybookingsystem.entity.PointProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PointProductMapper extends BaseMapper<PointProduct> {

    @Update("""
            update point_product
            set stock = stock - 1,
                update_time = now()
            where id = #{productId}
              and status = 1
              and (stock > 0 or stock = -1)
            """)
    int decreaseStock(@Param("productId") Long productId);
}