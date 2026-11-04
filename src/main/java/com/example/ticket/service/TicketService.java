package com.example.ticket.service;

import com.example.ticket.domain.Ticket;
import com.example.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final RedissonClient redissonClient;
    private final TicketRepository ticketRepository;

    // 분산 락
    public void purchaseTicketWithLock(Long ticketId, Long userId) {
        String lockKey = "ticket_lock_" + ticketId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 최대 5초간 락 획득 대기, 획득 시 3초간 임대 후 자동 해제
            boolean available = lock.tryLock(5, 3, TimeUnit.SECONDS);

            if (!available) {
                log.warn("[락 획득 실패] 유저 {} - 접속 대기 초과", userId);
                throw new RuntimeException("현재 대기자가 많습니다. 잠시 후 다시 시도해 주세요.");
            }

            // 락 획득 성공 시 DB 재고 차감
            log.info("[락 획득 성공] 유저 {} - 예매 비즈니스 로직 진입", userId);
            decreaseTicketQuantity(ticketId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("예매 진행 중 오류가 발생했습니다.");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[락 해제 완료] 유저 {} - 락 반납", userId);
            }
        }
    }

    // 데이터베이스 트랜잭션
    @Transactional
    public void decreaseTicketQuantity(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("선택하신 티켓의 잔여 수량이 부족합니다."));

        ticket.decreaseQuantity();
        ticketRepository.save(ticket);
        log.info("잔여 재고 반영 완료 -> 현재 남은 좌석수: {}", ticket.getQuantity());
    }
}
