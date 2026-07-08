package com.example.trekkingapp.route;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MapboxDirectionsClient {

    private static final String MAPBOX_URL_TEMPLATE =
            "https://api.mapbox.com/directions/v5/mapbox/walking/%s?geometries=polyline&overview=full&steps=false&access_token=%s";

    private final RestClient restClient;
    private final String accessToken;

    public MapboxDirectionsClient(RestClient.Builder restClientBuilder,
                                  @Value("${mapbox.access-token:}") String accessToken) {
        this.restClient = restClientBuilder.build();
        this.accessToken = accessToken;
    }

    public MapboxDirectionsResponse getWalkingRoute(RoutePointRequest start,
                                                    List<RoutePointRequest> checkpoints,
                                                    RoutePointRequest end) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("Mapbox access token is not configured. Set MAPBOX_ACCESS_TOKEN.");
        }

        String coordinates = Stream.concat(
                        Stream.concat(Stream.of(start), checkpoints.stream()),
                        Stream.of(end)
                )
                .map(this::toCoordinatePair)
                .collect(Collectors.joining(";"));

        String url = MAPBOX_URL_TEMPLATE.formatted(coordinates, accessToken);

        try {
            return restClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .body(MapboxDirectionsResponse.class);
        } catch (RestClientException exception) {
            throw new IllegalArgumentException("Failed to generate route from Mapbox Directions API");
        }
    }

    public String getRequestUrlTemplate() {
        return MAPBOX_URL_TEMPLATE;
    }

    private String toCoordinatePair(RoutePointRequest point) {
        return point.longitude() + "," + point.latitude();
    }
}
