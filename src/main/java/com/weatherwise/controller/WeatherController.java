package com.weatherwise.controller;

import com.weatherwise.model.Place;
import com.weatherwise.model.WeatherReport;
import com.weatherwise.service.WeatherService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WeatherController {

    private final WeatherService service;

    public WeatherController(WeatherService service) {
        this.service = service;
    }

    /** Weather by city name, or by coordinates when the user picked a suggestion. */
    @GetMapping("/weather")
    public WeatherReport weather(@RequestParam(required = false) String city,
                                 @RequestParam(required = false) Double lat,
                                 @RequestParam(required = false) Double lon,
                                 @RequestParam(required = false) String name,
                                 @RequestParam(required = false) String country) {
        if (lat != null && lon != null) {
            return service.getWeather(new Place(name == null ? "Selected location" : name,
                    country == null ? "" : country, "", lat, lon));
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("Enter a city name to see its weather.");
        }
        return service.getWeather(city.trim());
    }

    /** City suggestions for the search box. */
    @GetMapping("/places")
    public List<Place> places(@RequestParam String q) {
        if (q.trim().length() < 2) return List.of();
        return service.searchPlaces(q.trim(), 5);
    }
}
