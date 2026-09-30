package com.telecom.activation.service;

public class InvalidProductOrderException extends RuntimeException {

    public InvalidProductOrderException(String message) {
        super(message);
    }
}
