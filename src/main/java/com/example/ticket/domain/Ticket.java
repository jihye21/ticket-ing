package com.example.ticket.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title; // 티켓 이름
    private Integer quantity; // 남은 좌석

    public Ticket(String title, Integer quantity) {
        this.title = title;
        this.quantity = quantity;
    }

    // 재고 차감
    public void decreaseQuantity() {
        if (this.quantity <= 0) {
            throw new IllegalStateException("남은 좌석이 없습니다. 매진되었습니다.");
        }
        this.quantity -= 1;
    }
}
