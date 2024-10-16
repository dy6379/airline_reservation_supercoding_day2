package com.github.supercodingspring.web.dto.airline;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PaymentsRequest {
    private List<Integer> userIds;
    private List<Integer> airlineTicketIds;

    public PaymentsRequest() {
    }

    public List<Integer> getUserIds() {
        return userIds;
    }

    public List<Integer> getAirlineTicketIds() {
        return airlineTicketIds;
    }
}