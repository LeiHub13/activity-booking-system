package com.example.activitybookingsystem.cache;

import com.example.activitybookingsystem.vo.ActivityRegistrationStatsVO;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class ActivityCacheService {
    private static final String POPULAR_RANKING_KEY = "activity:popular:ranking:top20";
    private static final Duration DURATION_RANKING_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public ActivityCacheService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    //从缓存中取活动
    public List<ActivityRegistrationStatsVO> getPopularActivityRanking(){
        String json = stringRedisTemplate.opsForValue().get(POPULAR_RANKING_KEY);
        if (StringUtils.isEmpty(json)){
            return null;
        }
        try{
            return objectMapper.readValue(json, new TypeReference<List<ActivityRegistrationStatsVO>>() {});
        }catch (JsonProcessingException e){
            log.warn("热门活动缓存解析失败，已删除缓存", e);
            evictPopularRankingCache();
            return null;
        }
    }

    //把活动存入缓存
    public void setPopularRankingCache(List<ActivityRegistrationStatsVO> rankingList){
        try{
            String json = objectMapper.writeValueAsString(rankingList);
            stringRedisTemplate.opsForValue().set(POPULAR_RANKING_KEY, json, DURATION_RANKING_TTL);
        }catch (JsonProcessingException e){
            log.warn("热门活动写入失败", e);
        }
    }

    private void evictPopularRankingCache() {
        stringRedisTemplate.delete(POPULAR_RANKING_KEY);
    }
}
