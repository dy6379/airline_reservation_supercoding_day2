package com.github.supercodingspring.repository.payments;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "paymentId")
public class Payment {
    private Integer paymentId;
    private Integer passengerId;
    private Integer reservationId;
    private LocalDateTime payTime;

    public Payment(Integer reservationId, Integer passengerId) {
        this.reservationId = reservationId;
        this.passengerId = passengerId;
        this.payTime = LocalDateTime.now();
    }
}
