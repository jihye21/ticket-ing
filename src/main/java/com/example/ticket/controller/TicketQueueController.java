package com.example.ticket.controller;

import com.example.ticket.service.TicketQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class TicketQueueController {

    private final TicketQueueService ticketQueueService;

    // 대기열 진입 및 순번 조회 API
    @GetMapping("/status")
    public ResponseEntity<?> getQueueStatus(@RequestParam("userId") Long userId) {
        Long rank = ticketQueueService.getWaitRank(userId);
        
        if (rank == 0L) {
            return ResponseEntity.ok(Map.of("status", "ALLOWED", "message", "예매 페이지로 진입하세요."));
        }
        return ResponseEntity.ok(Map.of("status", "WAITING", "waitingCount", rank));
    }
}
