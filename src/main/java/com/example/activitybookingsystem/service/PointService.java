package com.example.activitybookingsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.activitybookingsystem.dto.PointProductDTO;
import com.example.activitybookingsystem.entity.PointAccount;
import com.example.activitybookingsystem.vo.PageVO;
import com.example.activitybookingsystem.vo.PointAccountVO;
import com.example.activitybookingsystem.vo.PointExchangeVO;
import com.example.activitybookingsystem.vo.PointProductVO;
import com.example.activitybookingsystem.vo.PointRecordVO;

public interface PointService extends IService<PointAccount> {

    PointAccountVO getMyAccount();

    PageVO<PointRecordVO> listMyRecords(Long pageNum, Long pageSize);

    PageVO<PointProductVO> listProducts(Long pageNum, Long pageSize);

    PointExchangeVO exchangeProduct(Long productId);

    PageVO<PointExchangeVO> listMyExchanges(Long pageNum, Long pageSize);

    PageVO<PointProductVO> listAllProducts(Long pageNum, Long pageSize);

    PointProductVO createProduct(PointProductDTO dto);

    PointProductVO updateProduct(Long productId, PointProductDTO dto);

    PointProductVO toggleProductStatus(Long productId);

    PageVO<PointExchangeVO> listAllExchanges(Long pageNum, Long pageSize);

    void awardPoints(Long userId, String type, Long bizId, int points, String remark);
}