package com.github.supercodingspring.repository.reservations;

public interface ReservationRepository {
    Boolean saveReservation(Reservation reservation);

    Reservation findReservationIdAndAirlineTicketId(Integer passengerId, Integer airlineTicketId);

    void updateReservationStatus(Integer reservationId, String status);
}
