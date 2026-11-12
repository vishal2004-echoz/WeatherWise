package com.weatherwise.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weatherwise.model.WeatherReport;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String GEO = """
            {"results":[{"name":"Visakhapatnam","country":"India","admin1":"Andhra Pradesh",
            "latitude":17.69,"longitude":83.21}]}""";

    private static final String FORECAST = """
            {"timezone":"Asia/Kolkata",
             "current":{"time":"2026-11-11T10:15","temperature_2m":29.4,"relative_humidity_2m":70,
               "apparent_temperature":33.1,"is_day":1,"weather_code":2,"pressure_msl":1008.2,
               "wind_speed_10m":12.5},
             "hourly":{"time":["2026-11-11T09:00","2026-11-11T10:00","2026-11-11T11:00"],
               "temperature_2m":[28.0,29.0,30.0],"weather_code":[1,2,61]},
             "daily":{"time":["2026-11-11"],"weather_code":[2],"temperature_2m_max":[31.0],
               "temperature_2m_min":[24.0],"sunrise":["2026-11-11T06:05"],"sunset":["2026-11-11T17:30"],
               "precipitation_probability_max":[40]}}""";

    @Mock
    private RestTemplate rest;

    private WeatherService service() {
        return new WeatherService(rest, "http://geo.test/search", "http://forecast.test/v1");
    }

    private static JsonNode json(String s) throws Exception {
        return JSON.readTree(s);
    }

    @Test
    void buildsReportFromProviderData() throws Exception {
        when(rest.getForObject(any(URI.class), eq(JsonNode.class)))
                .thenReturn(json(GEO), json(FORECAST));

        WeatherReport r = service().getWeather("Visakhapatnam");

        assertEquals("Visakhapatnam", r.city());
        assertEquals(29.4, r.current().temperature(), 0.001);
        assertEquals("Partly cloudy", r.current().condition());
        assertEquals("cloudy", r.current().theme());
        assertEquals(2, r.hourly().size());                    // starts at the current hour
        assertEquals("2026-11-11T10:00", r.hourly().get(0).time());
        assertEquals(1, r.daily().size());
        assertEquals(40, r.daily().get(0).rainChance());
    }

    @Test
    void throwsWhenCityIsUnknown() throws Exception {
        when(rest.getForObject(any(URI.class), eq(JsonNode.class))).thenReturn(json("{}"));
        assertThrows(CityNotFoundException.class, () -> service().getWeather("Nowhereville"));
    }
}
