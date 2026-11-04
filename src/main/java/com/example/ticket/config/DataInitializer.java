package com.example.ticket.config;

import com.example.ticket.domain.Ticket;
import com.example.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final TicketRepository ticketRepository;

    @Override
    public void run(String... args) throws Exception {
        // 테스트용 타겟
        if (ticketRepository.count() == 0) {
            ticketRepository.save(new Ticket("선착순 티켓", 100));
            System.out.println("====== 테스트용 선착순 티켓 100개 적재 완료 ======");
        }
    }
}
