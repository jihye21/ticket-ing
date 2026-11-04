package com.example.ticket.controller;

import com.example.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    // 티켓 예매 API 엔드포인트
    // POST http://localhost:8080/api/tickets/1/purchase?userId=123
    @PostMapping("/{ticketId}/purchase")
    public ResponseEntity<String> purchaseTicket(
            @PathVariable Long ticketId,
            @RequestParam Long userId) {
        
        try {
            ticketService.purchaseTicketWithLock(ticketId, userId);
            return ResponseEntity.ok("티켓 예매가 완료되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
