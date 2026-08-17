package com.example.activitybookingsystem.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.activitybookingsystem.common.exception.BusinessException;
import com.example.activitybookingsystem.dto.PointProductDTO;
import com.example.activitybookingsystem.entity.PointAccount;
import com.example.activitybookingsystem.entity.PointExchange;
import com.example.activitybookingsystem.entity.PointProduct;
import com.example.activitybookingsystem.entity.PointRecord;
import com.example.activitybookingsystem.entity.User;
import com.example.activitybookingsystem.mapper.PointAccountMapper;
import com.example.activitybookingsystem.mapper.PointExchangeMapper;
import com.example.activitybookingsystem.mapper.PointProductMapper;
import com.example.activitybookingsystem.mapper.PointRecordMapper;
import com.example.activitybookingsystem.mapper.UserMapper;
import com.example.activitybookingsystem.service.PointService;
import com.example.activitybookingsystem.vo.PageVO;
import com.example.activitybookingsystem.vo.PointAccountVO;
import com.example.activitybookingsystem.vo.PointExchangeVO;
import com.example.activitybookingsystem.vo.PointProductVO;
import com.example.activitybookingsystem.vo.PointRecordVO;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PointServiceImpl extends ServiceImpl<PointAccountMapper, PointAccount> implements PointService {

    private static final String POINT_TYPE_REGISTER = "REGISTER";
    private static final String POINT_TYPE_CHECK_IN = "CHECK_IN";
    private static final String POINT_TYPE_RATING = "RATING";
    private static final String POINT_TYPE_EXCHANGE = "EXCHANGE";

    private final PointAccountMapper pointAccountMapper;
    private final PointRecordMapper pointRecordMapper;
    private final PointProductMapper pointProductMapper;
    private final PointExchangeMapper pointExchangeMapper;
    private final UserMapper userMapper;

    public PointServiceImpl(PointAccountMapper pointAccountMapper,
                            PointRecordMapper pointRecordMapper,
                            PointProductMapper pointProductMapper,
                            PointExchangeMapper pointExchangeMapper,
                            UserMapper userMapper) {
        this.pointAccountMapper = pointAccountMapper;
        this.pointRecordMapper = pointRecordMapper;
        this.pointProductMapper = pointProductMapper;
        this.pointExchangeMapper = pointExchangeMapper;
        this.userMapper = userMapper;
    }

    @Override
    public PointAccountVO getMyAccount() {
        User currentUser = getCurrentUserEntity();
        PointAccount account = getOrCreateAccount(currentUser.getId());
        return toPointAccountVO(account);
    }

    @Override
    public PageVO<PointRecordVO> listMyRecords(Long pageNum, Long pageSize) {
        User currentUser = getCurrentUserEntity();
        LambdaQueryWrapper<PointRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PointRecord::getUserId, currentUser.getId())
                .orderByDesc(PointRecord::getId);
        Page<PointRecord> result = pointRecordMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);

        PageVO<PointRecordVO> pageVO = new PageVO<>();
        pageVO.setTotal(result.getTotal());
        pageVO.setPageNum(result.getCurrent());
        pageVO.setPageSize(result.getSize());
        pageVO.setRecords(result.getRecords().stream().map(this::toPointRecordVO).toList());
        return pageVO;
    }

    @Override
    public PageVO<PointProductVO> listProducts(Long pageNum, Long pageSize) {
        LambdaQueryWrapper<PointProduct> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PointProduct::getStatus, 1)
                .orderByAsc(PointProduct::getPointsRequired);
        Page<PointProduct> result = pointProductMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        return toProductPage(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointExchangeVO exchangeProduct(Long productId) {
        if (productId == null) {
            throw new BusinessException("商品ID不能为空");
        }
        User currentUser = getCurrentUserEntity();
        PointProduct product = pointProductMapper.selectById(productId);
        if (product == null || !Integer.valueOf(1).equals(product.getStatus())) {
            throw new BusinessException("商品不存在或已下架");
        }

        PointAccount account = getOrCreateAccount(currentUser.getId());
        if (account.getBalance() < product.getPointsRequired()) {
            throw new BusinessException("积分不足");
        }

        // 先抢商品库存，再扣积分，避免积分扣了库存却不足。
        int updatedStock = pointProductMapper.decreaseStock(productId);
        if (updatedStock != 1) {
            throw new BusinessException("商品库存不足");
        }

        int updatedBalance = pointAccountMapper.decreaseBalance(currentUser.getId(), product.getPointsRequired());
        if (updatedBalance != 1) {
            throw new BusinessException("积分不足，请刷新后重试");
        }

        LocalDateTime now = LocalDateTime.now();
        PointExchange exchange = new PointExchange();
        exchange.setUserId(currentUser.getId());
        exchange.setProductId(product.getId());
        exchange.setProductName(product.getName());
        exchange.setPointsCost(product.getPointsRequired());
        exchange.setCreateTime(now);
        pointExchangeMapper.insert(exchange);

        PointRecord record = new PointRecord();
        record.setUserId(currentUser.getId());
        record.setPoints(-product.getPointsRequired());
        record.setType(POINT_TYPE_EXCHANGE);
        record.setBizId(exchange.getId());
        record.setRemark("兑换「" + product.getName() + "」");
        record.setCreateTime(now);
        try {
            pointRecordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            // 理论上兑换只发生一次，唯一键兜底。
            throw new BusinessException("兑换操作已提交，请勿重复操作");
        }

        return toPointExchangeVO(exchange, currentUser.getUsername());
    }

    @Override
    public PageVO<PointExchangeVO> listMyExchanges(Long pageNum, Long pageSize) {
        User currentUser = getCurrentUserEntity();
        LambdaQueryWrapper<PointExchange> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PointExchange::getUserId, currentUser.getId())
                .orderByDesc(PointExchange::getId);
        Page<PointExchange> result = pointExchangeMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        return toExchangePage(result, Map.of(currentUser.getId(), currentUser.getUsername()));
    }

    @Override
    public PageVO<PointProductVO> listAllProducts(Long pageNum, Long pageSize) {
        LambdaQueryWrapper<PointProduct> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(PointProduct::getId);
        Page<PointProduct> result = pointProductMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        return toProductPage(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointProductVO createProduct(PointProductDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        PointProduct product = new PointProduct();
        product.setName(dto.getName().trim());
        product.setDescription(trimToNull(dto.getDescription()));
        product.setPointsRequired(dto.getPointsRequired());
        product.setStock(dto.getStock());
        product.setStatus(1);
        product.setCreateTime(now);
        product.setUpdateTime(now);
        pointProductMapper.insert(product);
        return toPointProductVO(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointProductVO updateProduct(Long productId, PointProductDTO dto) {
        PointProduct product = pointProductMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        product.setName(dto.getName().trim());
        product.setDescription(trimToNull(dto.getDescription()));
        product.setPointsRequired(dto.getPointsRequired());
        product.setStock(dto.getStock());
        product.setUpdateTime(LocalDateTime.now());
        pointProductMapper.updateById(product);
        return toPointProductVO(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointProductVO toggleProductStatus(Long productId) {
        PointProduct product = pointProductMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        product.setStatus(Integer.valueOf(1).equals(product.getStatus()) ? 0 : 1);
        product.setUpdateTime(LocalDateTime.now());
        pointProductMapper.updateById(product);
        return toPointProductVO(product);
    }

    @Override
    public PageVO<PointExchangeVO> listAllExchanges(Long pageNum, Long pageSize) {
        LambdaQueryWrapper<PointExchange> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(PointExchange::getId);
        Page<PointExchange> result = pointExchangeMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);

        List<Long> userIds = result.getRecords().stream()
                .map(PointExchange::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> usernameMap = userIds.isEmpty()
                ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getUsername));
        return toExchangePage(result, usernameMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void awardPoints(Long userId, String type, Long bizId, int points, String remark) {
        if (userId == null || bizId == null) {
            throw new BusinessException("积分参数不完整");
        }

        LambdaQueryWrapper<PointRecord> existsQuery = new LambdaQueryWrapper<>();
        existsQuery.eq(PointRecord::getUserId, userId)
                .eq(PointRecord::getType, type)
                .eq(PointRecord::getBizId, bizId);
        if (pointRecordMapper.selectCount(existsQuery) > 0) {
            return;
        }

        PointAccount account = getOrCreateAccount(userId);
        if (account.getBalance() == null) {
            account.setBalance(0);
        }

        // 先插入流水（唯一键兜底防重复），再更新账户余额。
        LocalDateTime now = LocalDateTime.now();
        PointRecord record = new PointRecord();
        record.setUserId(userId);
        record.setPoints(points);
        record.setType(type);
        record.setBizId(bizId);
        record.setRemark(remark);
        record.setCreateTime(now);
        try {
            pointRecordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            return;
        }

        pointAccountMapper.increaseBalance(userId, points);
    }

    private PointAccount getOrCreateAccount(Long userId) {
        LambdaQueryWrapper<PointAccount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PointAccount::getUserId, userId);
        PointAccount account = pointAccountMapper.selectOne(queryWrapper);
        if (account != null) {
            return account;
        }
        LocalDateTime now = LocalDateTime.now();
        account = new PointAccount();
        account.setUserId(userId);
        account.setBalance(0);
        account.setTotalEarned(0);
        account.setTotalSpent(0);
        account.setCreateTime(now);
        account.setUpdateTime(now);
        try {
            pointAccountMapper.insert(account);
        } catch (DuplicateKeyException ex) {
            return pointAccountMapper.selectOne(queryWrapper);
        }
        return account;
    }

    private User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException("当前用户未登录");
        }
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, authentication.getName());
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException("当前用户不存在");
        }
        return user;
    }

    private PointAccountVO toPointAccountVO(PointAccount account) {
        PointAccountVO vo = new PointAccountVO();
        vo.setBalance(account.getBalance() == null ? 0 : account.getBalance());
        vo.setTotalEarned(account.getTotalEarned() == null ? 0 : account.getTotalEarned());
        vo.setTotalSpent(account.getTotalSpent() == null ? 0 : account.getTotalSpent());
        return vo;
    }

    private PointRecordVO toPointRecordVO(PointRecord record) {
        PointRecordVO vo = new PointRecordVO();
        vo.setId(record.getId());
        vo.setPoints(record.getPoints());
        vo.setType(record.getType());
        vo.setRemark(record.getRemark());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private PointProductVO toPointProductVO(PointProduct product) {
        PointProductVO vo = new PointProductVO();
        BeanUtils.copyProperties(product, vo);
        return vo;
    }

    private PointExchangeVO toPointExchangeVO(PointExchange exchange, String username) {
        PointExchangeVO vo = new PointExchangeVO();
        vo.setId(exchange.getId());
        vo.setUserId(exchange.getUserId());
        vo.setUsername(username);
        vo.setProductId(exchange.getProductId());
        vo.setProductName(exchange.getProductName());
        vo.setPointsCost(exchange.getPointsCost());
        vo.setCreateTime(exchange.getCreateTime());
        return vo;
    }

    private PageVO<PointProductVO> toProductPage(Page<PointProduct> result) {
        PageVO<PointProductVO> pageVO = new PageVO<>();
        pageVO.setTotal(result.getTotal());
        pageVO.setPageNum(result.getCurrent());
        pageVO.setPageSize(result.getSize());
        pageVO.setRecords(result.getRecords().stream().map(this::toPointProductVO).toList());
        return pageVO;
    }

    private PageVO<PointExchangeVO> toExchangePage(Page<PointExchange> result, Map<Long, String> usernameMap) {
        PageVO<PointExchangeVO> pageVO = new PageVO<>();
        pageVO.setTotal(result.getTotal());
        pageVO.setPageNum(result.getCurrent());
        pageVO.setPageSize(result.getSize());
        pageVO.setRecords(result.getRecords().stream()
                .map(exchange -> toPointExchangeVO(exchange, usernameMap.get(exchange.getUserId())))
                .toList());
        return pageVO;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}