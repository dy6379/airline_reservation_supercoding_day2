package com.github.supercodingspring.service;

import com.github.supercodingspring.repository.airlineTicket.AirlineTicket;
import com.github.supercodingspring.repository.airlineTicket.AirlineTicketAndFlightInfo;
import com.github.supercodingspring.repository.airlineTicket.AirlineTicketRepository;
import com.github.supercodingspring.repository.passenger.Passenger;
import com.github.supercodingspring.repository.passenger.PassengerReposiotry;
import com.github.supercodingspring.repository.payments.Payment;
import com.github.supercodingspring.repository.payments.PaymentRepository;
import com.github.supercodingspring.repository.reservations.Reservation;
import com.github.supercodingspring.repository.reservations.ReservationRepository;
import com.github.supercodingspring.repository.users.UserEntity;
import com.github.supercodingspring.repository.users.UserRepository;
import com.github.supercodingspring.web.dto.airline.PaymentsRequest;
import com.github.supercodingspring.web.dto.airline.ReservationRequest;
import com.github.supercodingspring.web.dto.airline.ReservationResult;
import com.github.supercodingspring.web.dto.airline.Ticket;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AirReservationService {

    private UserRepository userRepository;
    private AirlineTicketRepository airlineTicketRepository;

    private PassengerReposiotry passengerReposiotry;
    private ReservationRepository reservationRepository;
    private PaymentRepository paymentRepository;

    public AirReservationService(UserRepository userRepository, AirlineTicketRepository airlineTicketRepository, PassengerReposiotry passengerReposiotry, ReservationRepository reservationRepository, PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.airlineTicketRepository = airlineTicketRepository;
        this.passengerReposiotry = passengerReposiotry;
        this.reservationRepository = reservationRepository;
        this.paymentRepository = paymentRepository;
    }

    public List<Ticket> findUserFavoritePlaceTickets(Integer userId, String ticketType) {
        // 1. 유저를 userId 로 가져와서, 선호하는 여행지 도출
        // 2. 선호하는 여행지와 ticketType으로 AirlineTIcket table 질의 해서 필요한 AirlineTicket
        // 3. 이 둘의 정보를 조합해서 Ticket DTO를 만든다.
        UserEntity userEntity = userRepository.findUserById(userId);
        String likePlace = userEntity.getLikeTravelPlace();

        List<AirlineTicket> airlineTickets
                = airlineTicketRepository.findAllAirlineTicketsWithPlaceAndTicketType(likePlace, ticketType);

        List<Ticket> tickets = airlineTickets.stream().map(Ticket::new).collect(Collectors.toList());
        return tickets;
    }

    @Transactional(transactionManager = "tm2")
    public ReservationResult makeReservation(ReservationRequest reservationRequest) {
        // 1. Reservation Repository, Passenger Repository, Join table ( flight/airline_ticket ),

        // 0. userId,airline_ticke_id
        Integer userId = reservationRequest.getUserId();
        Integer airlineTicketId= reservationRequest.getAirlineTicketId();

        // 1. Passenger I
        Passenger passenger = passengerReposiotry.findPassengerByUserId(userId);
        Integer passengerId= passenger.getPassengerId();

        // 2. price 등의 정보 불러오기
        List<AirlineTicketAndFlightInfo> airlineTicketAndFlightInfos
                = airlineTicketRepository.findAllAirLineTicketAndFlightInfo(airlineTicketId);

        // 3. reservation 생성
        Reservation reservation = new Reservation(passengerId, airlineTicketId);
        Boolean isSuccess = reservationRepository.saveReservation(reservation);

        // TODO: ReservationResult DTO 만들기
        List<Integer> prices = airlineTicketAndFlightInfos.stream().map(AirlineTicketAndFlightInfo::getPrice).collect(Collectors.toList());
        List<Integer> charges = airlineTicketAndFlightInfos.stream().map(AirlineTicketAndFlightInfo::getCharge).collect(Collectors.toList());
        Integer tax = airlineTicketAndFlightInfos.stream().map(AirlineTicketAndFlightInfo::getTax).findFirst().get();
        Integer totalPrice = airlineTicketAndFlightInfos.stream().map(AirlineTicketAndFlightInfo::getTotalPrice).findFirst().get();

        return new ReservationResult(prices, charges, tax, totalPrice, isSuccess);
    }

    @Transactional(transactionManager = "tm2")
    public Integer paymentAirReservation(PaymentsRequest paymentsRequest) {
        // 0. user-id 와 airline-ticket-ids 리스트의 각각 순서는 각각 user-id와 airline-ticket-ids 순서로 되어있으며
        List<Integer> userIds = paymentsRequest.getUserIds();
        List<Integer> airlineTicketIds = paymentsRequest.getAirlineTicketIds();

        // 1. userIds와 airline_ticket_Ids 길이는 같게
        if (userIds.size() != airlineTicketIds.size())
            throw new IllegalArgumentException("userIds and airlineTicketIds must match");

        System.out.println("Step 1: User IDs and Airline Ticket IDs validation successful.");

        // 2. 각 userIds에 해당하는 passengerIds 가져오기
        List<Integer> passengerIds = userIds.stream()
                .map((userId) -> passengerReposiotry.findPassengerByUserId(userId)) // passenger 검색
                .map(Passenger::getPassengerId)
                .collect(Collectors.toList());

        // 3. 예약 초기화, 예약 확인
        List<Reservation> reservationStandByList = new ArrayList<>();
        int successCount = 0;

        for (int i = 0; i < userIds.size(); i++) {
            Integer passengerId = passengerIds.get(i);
            Integer airlineTicketId = airlineTicketIds.get(i);
            System.out.println("Step 3: Checking reservation for passengerId " + passengerId + " and airlineTicketId " + airlineTicketId);

            Reservation reservation = reservationRepository.findReservationIdAndAirlineTicketId(passengerId, airlineTicketId);
            reservationStandByList.add(reservation);
            if (reservation != null) {
                System.out.println("Step 3: Reservation found for passengerId " + passengerId + " and airlineTicketId " + airlineTicketId);
            } else {
                System.out.println("Step 3: No reservation found for passengerId " + passengerId + " and airlineTicketId " + airlineTicketId);
            }
        }

        // 4. 결제 처리 및 예약 상태 업데이트
        for (Reservation reservation : reservationStandByList) {
            if (reservation == null) {
                System.out.println("Skipping reservation because it is null.");
                continue;
            }
            if (reservation.getReservationStatus().equals("확정")) {
                System.out.println("Skipping reservation because it is already confirmed: reservationId " + reservation.getReservationId());
                continue;
            }

            Payment payment = new Payment(reservation.getReservationId(), reservation.getPassengerId());
            Boolean success = paymentRepository.savePayment(payment);
            if (success) {
                successCount++;
                reservationRepository.updateReservationStatus(reservation.getReservationId(), "확정");
                System.out.println("Step 4: Payment successful and reservation confirmed for reservationId " + reservation.getReservationId());
            } else {
                System.out.println("Step 4: Payment failed for reservationId " + reservation.getReservationId());
            }
        }

        System.out.println("Total successful payments: " + successCount);
        return successCount;
    }

}
