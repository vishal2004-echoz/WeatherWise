package com.weatherwise.service;

public class CityNotFoundException extends RuntimeException {

    public CityNotFoundException(String message) {
        super(message);
    }
}
