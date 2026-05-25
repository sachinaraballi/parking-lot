package com.parkinglot.exception;

public class TicketNotFoundException extends ParkingException {
    public TicketNotFoundException(String message) {
        super(message);
    }
}
