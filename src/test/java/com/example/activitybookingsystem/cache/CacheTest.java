package com.example.activitybookingsystem.cache;

import com.example.activitybookingsystem.service.ActivityService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class CacheTest {
    @Autowired
    private ActivityService activityService;

    @Test
    public void testCache() {
        log.info(activityService.listPopularActivityRanking(5).toString());
    }
}
