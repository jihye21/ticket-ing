package com.example.ticket;

import com.example.ticket.domain.Ticket;
import com.example.ticket.repository.TicketRepository;
import com.example.ticket.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TicketApplicationTests {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    @DisplayName("100명이 동시에 남은 좌석 1개를 예매할 때, 단 1명만 성공하고 남은 재고는 0이어야 한다")
    void concurrencyTest() throws InterruptedException {
        // 테스트용 티켓 1개 생성 
        Ticket ticket = new Ticket("용산 치이카와 마켓", 1);
        ticketRepository.save(ticket);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 100개의 스레드가 동시에 분산 락 예매 메서드 호출
        for (int i = 0; i < threadCount; i++) {
            final Long userId = (long) i;
            executorService.submit(() -> {
                try {
                    ticketService.purchaseTicketWithLock(ticket.getId(), userId);
                } catch (Exception e) {
                    System.out.println("결과 피드백 [" + userId + "]: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); //대기

        // 재고 검증
        Ticket updatedTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
        System.out.println("=== 최종 남은 티켓 수량: " + updatedTicket.getQuantity() + " ===");

        assertThat(updatedTicket.getQuantity()).isEqualTo(0);
    }
}
