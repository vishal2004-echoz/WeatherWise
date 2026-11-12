package com.weatherwise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.weatherwise.model.Place;
import com.weatherwise.model.WeatherReport;
import com.weatherwise.model.WeatherReport.Current;
import com.weatherwise.model.WeatherReport.Day;
import com.weatherwise.model.WeatherReport.Hour;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class WeatherService {

    private final RestTemplate rest;
    private final String geocodingUrl;
    private final String forecastUrl;

    public WeatherService(RestTemplate rest,
                          @Value("${weatherwise.geocoding-url}") String geocodingUrl,
                          @Value("${weatherwise.forecast-url}") String forecastUrl) {
        this.rest = rest;
        this.geocodingUrl = geocodingUrl;
        this.forecastUrl = forecastUrl;
    }

    public List<Place> searchPlaces(String query, int count) {
        URI uri = UriComponentsBuilder.fromUriString(geocodingUrl)
                .queryParam("name", query)
                .queryParam("count", count)
                .queryParam("language", "en")
                .build().encode().toUri();
        JsonNode root = rest.getForObject(uri, JsonNode.class);
        List<Place> places = new ArrayList<>();
        if (root == null) return places;
        for (JsonNode r : root.path("results")) {
            places.add(new Place(
                    r.path("name").asText(),
                    r.path("country").asText(""),
                    r.path("admin1").asText(""),
                    r.path("latitude").asDouble(),
                    r.path("longitude").asDouble()));
        }
        return places;
    }

    public WeatherReport getWeather(String city) {
        List<Place> places = searchPlaces(city, 1);
        if (places.isEmpty()) throw new CityNotFoundException(city);
        return getWeather(places.get(0));
    }

    public WeatherReport getWeather(Place place) {
        URI uri = UriComponentsBuilder.fromUriString(forecastUrl)
                .queryParam("latitude", place.latitude())
                .queryParam("longitude", place.longitude())
                .queryParam("current", "temperature_2m,relative_humidity_2m,apparent_temperature,"
                        + "is_day,weather_code,pressure_msl,wind_speed_10m")
                .queryParam("hourly", "temperature_2m,weather_code")
                .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,"
                        + "sunrise,sunset,precipitation_probability_max")
                .queryParam("timezone", "auto")
                .queryParam("forecast_days", 7)
                .build().encode().toUri();
        JsonNode root = rest.getForObject(uri, JsonNode.class);
        if (root == null || root.path("current").isMissingNode()) {
            throw new WeatherProviderException("The weather service returned no data. Try again shortly.");
        }
        return parse(place, root);
    }

    private WeatherReport parse(Place place, JsonNode root) {
        JsonNode c = root.path("current");
        int code = c.path("weather_code").asInt();
        Current current = new Current(
                c.path("time").asText(),
                c.path("temperature_2m").asDouble(),
                c.path("apparent_temperature").asDouble(),
                c.path("relative_humidity_2m").asInt(),
                c.path("wind_speed_10m").asDouble(),
                c.path("pressure_msl").asDouble(),
                WeatherCodes.describe(code),
                WeatherCodes.theme(code),
                c.path("is_day").asInt(1) == 1);

        // next 24 hours, starting from the current hour
        JsonNode h = root.path("hourly");
        JsonNode times = h.path("time");
        String nowHour = current.time().substring(0, 13);
        List<Hour> hours = new ArrayList<>();
        for (int i = 0; i < times.size() && hours.size() < 24; i++) {
            if (times.get(i).asText().substring(0, 13).compareTo(nowHour) < 0) continue;
            int hc = h.path("weather_code").get(i).asInt();
            hours.add(new Hour(times.get(i).asText(),
                    h.path("temperature_2m").get(i).asDouble(),
                    WeatherCodes.describe(hc), WeatherCodes.theme(hc)));
        }

        JsonNode d = root.path("daily");
        JsonNode dates = d.path("time");
        List<Day> days = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            int dc = d.path("weather_code").get(i).asInt();
            JsonNode rain = d.path("precipitation_probability_max").get(i);
            days.add(new Day(dates.get(i).asText(),
                    d.path("temperature_2m_max").get(i).asDouble(),
                    d.path("temperature_2m_min").get(i).asDouble(),
                    WeatherCodes.describe(dc), WeatherCodes.theme(dc),
                    d.path("sunrise").get(i).asText(),
                    d.path("sunset").get(i).asText(),
                    rain == null || rain.isNull() ? null : rain.asInt()));
        }
        return new WeatherReport(place.name(), place.country(),
                root.path("timezone").asText(), current, hours, days);
    }
}
