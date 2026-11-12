package com.weatherwise.service;

public class CityNotFoundException extends RuntimeException {
    public CityNotFoundException(String city) {
        super("No city found for \"" + city + "\". Check the spelling or try a nearby city.");
    }
}
