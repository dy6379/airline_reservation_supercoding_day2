package com.github.supercodingspring.repository.payments;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentJdbcTemplateDao implements PaymentRepository {
    private JdbcTemplate template;

    public PaymentJdbcTemplateDao(@Qualifier("jdbcTemplate2") JdbcTemplate jdbcTemplate) {
        this.template = jdbcTemplate;
    }
    @Override
    public Boolean savePayment(Payment payment){
        Integer rowNums = template.update("INSERT INTO payment(passenger_id, reservation_id, pay_at) VALUES (?, ?, ?)",
                payment.getPassengerId(), payment.getReservationId(), payment.getPayTime());

        return rowNums > 0;
    }
}
