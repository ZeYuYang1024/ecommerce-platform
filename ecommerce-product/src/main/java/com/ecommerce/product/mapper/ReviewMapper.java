package com.ecommerce.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.product.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    @Select("SELECT id FROM spu WHERE id = #{spuId} AND deleted = 0 FOR UPDATE")
    Long lockProduct(Long spuId);

    @Update("""
            UPDATE spu SET
                review_count = (SELECT COUNT(*) FROM review WHERE spu_id = #{spuId} AND deleted = 0),
                avg_rating = COALESCE((SELECT AVG(rating) FROM review WHERE spu_id = #{spuId} AND deleted = 0), 0)
            WHERE id = #{spuId} AND deleted = 0
            """)
    void refreshRating(Long spuId);
}
