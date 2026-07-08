package com.example.trekkingapp.route;

import java.util.List;

public record MapboxDirectionsResponse(
        String code,
        List<MapboxRouteData> routes,
        String message
) {

    public record MapboxRouteData(
            String geometry,
            Double distance,
            Double duration
    ) {
    }
}
