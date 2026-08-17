package com.example.activitybookingsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.activitybookingsystem.dto.CreateRatingDTO;
import com.example.activitybookingsystem.entity.ActivityRating;
import com.example.activitybookingsystem.vo.ActivityRatingVO;
import com.example.activitybookingsystem.vo.PageVO;

public interface RatingService extends IService<ActivityRating> {

    ActivityRatingVO createRating(CreateRatingDTO dto);

    PageVO<ActivityRatingVO> listMyRatings(Long pageNum, Long pageSize);

    PageVO<ActivityRatingVO> listActivityRatings(Long activityId, Long pageNum, Long pageSize);
}