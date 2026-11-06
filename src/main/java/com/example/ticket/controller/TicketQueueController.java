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

    //진입 API
    @PostMapping("/join")
    public ResponseEntity<?> joinQueue(@RequestParam("eventId") String eventId,
                                        @RequestParam("userId") Long userId) {
        Long rank = ticketQueueService.registerQueueWithLua(eventId, userId);

        if(rank == null) {
            return ResponseEntity.status(500).body(Map.of("status", "ERROR", "message", "등록에 
            실패하였습니다."));
        }
        if(rank == OL) {
            return ResponseEntity.ok(Map.of("status", "ALLOWED"));
        }
        if(rank < 0) {
            return ResponseEntity.status(500).body(Map.of("status", "ERROR", "message", "내부 오류"
            ));
        }
        return ResponseEntity.ok(Map.of("status", "WAITING", "waitingCount", rank));
    }

    // 대기열 진입 및 순번 조회 API
    @GetMapping("/status")
    public ResponseEntity<?> getQueueStatus(@RequestParam("eventId") String eventId,
                                            @RequestParam("userId") Long userId) {
        Long rank = ticketQueueService.getWaitRankReadOnly(eventId, userId);

        if(rank == null) {
            return ResponseEntity.ok(Map.of("status", "NOT_REGISTERED", "message", "POST /join으로
            등록하세요."));
        }
        if(rank == OL){
            return ResponseEntity.ok(Map.of("status", "ALLOWED", "message", "예매 페이지로 진입
            하세요."));
        }
        return ResponseEntity.ok(Map.of("status", "WAITING", "waitingCount", rank));
    }
}
