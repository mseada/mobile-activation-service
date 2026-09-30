package com.telecom.activation.service;

public class ProductOrderNotFoundException extends RuntimeException {

    public ProductOrderNotFoundException(String id) {
        super("ProductOrder not found: " + id);
    }
}
