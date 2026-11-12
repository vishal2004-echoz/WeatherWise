package com.weatherwise.model;

import java.util.List;

public record WeatherReport(String city, String country, String timezone,
                            Current current, List<Hour> hourly, List<Day> daily) {

    public record Current(String time, double temperature, double feelsLike, int humidity,
                          double windSpeed, double pressure, String condition, String theme, boolean day) {
    }

    public record Hour(String time, double temperature, String condition, String theme) {
    }

    public record Day(String date, double max, double min, String condition, String theme,
                      String sunrise, String sunset, Integer rainChance) {
    }
}
