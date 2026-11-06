package com.example.ticket.scheduler;

import com.example.ticket.service.TicketQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class QueueScheduler {

    private final TicketQueueService ticketQueueService;

    // 1초마다 대기열 상위 유저 100명씩 통과
    @Scheduled(fixedDelay = 1000)
    public void processQueueBatch() {
        String eventId = "event001";
        ticketQueueService.allowEntry(eventId, 100);
    }
}
