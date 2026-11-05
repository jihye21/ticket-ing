package com.example.ticket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketQueueService {

    private final RedissonClient redissonClient;
    
    private static final String WAIT_QUEUE_KEY = "ticket_wait_queue";
    private static final String PROCEED_SET_KEY = "ticket_proceed_users";

    // 대기열 등록
    public Long registerQueue(Long userId) {
        if (isAllowed(userId)) {
            return 0L;
        }

        RScoredSortedSet<Long> waitQueue = redissonClient.getScoredSortedSet(WAIT_QUEUE_KEY);
        long now = System.currentTimeMillis();
        
        waitQueue.add(now, userId);
        
        Integer rank = waitQueue.rank(userId);
        return (rank != null) ? rank + 1L : 1L;
    }

    // 대기 번호 조회
    public Long getWaitRank(Long userId) {
        if (isAllowed(userId)) {
            return 0L;
        }

        RScoredSortedSet<Long> waitQueue = redissonClient.getScoredSortedSet(WAIT_QUEUE_KEY);
        Integer rank = waitQueue.rank(userId);
        
        return (rank != null) ? rank + 1L : registerQueue(userId);
    }

    public void allowEntry(int userCount) {
        RScoredSortedSet<Long> waitQueue = redissonClient.getScoredSortedSet(WAIT_QUEUE_KEY);
        RSet<Long> proceedUsers = redissonClient.getSet(PROCEED_SET_KEY);

        Collection<Long> standardLine = waitQueue.pollFirst(userCount);
        
        if (!standardLine.isEmpty()) {
            proceedUsers.addAll(standardLine);
            log.info("[대기열] {}명 통과 (예매 가능 수: {}명)", 
                    standardLine.size(), proceedUsers.size());
        }
    }

    public boolean isAllowed(Long userId) {
        RSet<Long> proceedUsers = redissonClient.getSet(PROCEED_SET_KEY);
        return proceedUsers.contains(userId);
    }
}
