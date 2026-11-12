package com.weatherwise.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WeatherCodesTest {

    @Test
    void mapsCodesToThemes() {
        assertEquals("clear", WeatherCodes.theme(0));
        assertEquals("cloudy", WeatherCodes.theme(3));
        assertEquals("fog", WeatherCodes.theme(45));
        assertEquals("rain", WeatherCodes.theme(63));
        assertEquals("snow", WeatherCodes.theme(73));
        assertEquals("storm", WeatherCodes.theme(95));
    }

    @Test
    void describesCodes() {
        assertEquals("Clear sky", WeatherCodes.describe(0));
        assertEquals("Thunderstorm", WeatherCodes.describe(99));
        assertEquals("Unknown", WeatherCodes.describe(1234));
    }
}
