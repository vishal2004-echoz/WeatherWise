package com.weatherwise.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weatherwise.service.CityNotFoundException;
import com.weatherwise.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WeatherController.class)
class WeatherControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private WeatherService service;

    @Test
    void returns404ForUnknownCity() throws Exception {
        when(service.getWeather("Nowhereville")).thenThrow(new CityNotFoundException("Nowhereville"));
        mvc.perform(get("/api/weather").param("city", "Nowhereville"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void returns400WhenNoCityGiven() throws Exception {
        mvc.perform(get("/api/weather"))
                .andExpect(status().isBadRequest());
    }
}
