package com.example.trekkingapp.route;

import java.util.ArrayList;
import java.util.List;

final class PolylineCodec {

    private static final double PRECISION = 1e5;

    private PolylineCodec() {
    }

    static List<Coordinate> decode(String polyline) {
        List<Coordinate> coordinates = new ArrayList<>();
        int index = 0;
        int latitude = 0;
        int longitude = 0;

        while (index < polyline.length()) {
            DecodeResult latitudeResult = decodeValue(polyline, index);
            latitude += latitudeResult.delta();
            index = latitudeResult.nextIndex();

            DecodeResult longitudeResult = decodeValue(polyline, index);
            longitude += longitudeResult.delta();
            index = longitudeResult.nextIndex();

            coordinates.add(new Coordinate(latitude / PRECISION, longitude / PRECISION));
        }

        return coordinates;
    }

    static String encode(List<Coordinate> coordinates) {
        StringBuilder encoded = new StringBuilder();
        long previousLatitude = 0;
        long previousLongitude = 0;

        for (Coordinate coordinate : coordinates) {
            long latitude = Math.round(coordinate.latitude() * PRECISION);
            long longitude = Math.round(coordinate.longitude() * PRECISION);

            encodeValue(latitude - previousLatitude, encoded);
            encodeValue(longitude - previousLongitude, encoded);

            previousLatitude = latitude;
            previousLongitude = longitude;
        }

        return encoded.toString();
    }

    private static DecodeResult decodeValue(String polyline, int startIndex) {
        int result = 0;
        int shift = 0;
        int index = startIndex;
        int current;

        do {
            current = polyline.charAt(index++) - 63;
            result |= (current & 0x1f) << shift;
            shift += 5;
        } while (current >= 0x20);

        int delta = (result & 1) == 1 ? ~(result >> 1) : result >> 1;
        return new DecodeResult(delta, index);
    }

    private static void encodeValue(long value, StringBuilder encoded) {
        long shifted = value < 0 ? ~(value << 1) : value << 1;

        while (shifted >= 0x20) {
            encoded.append((char) ((0x20 | (shifted & 0x1f)) + 63));
            shifted >>= 5;
        }

        encoded.append((char) (shifted + 63));
    }

    record Coordinate(double latitude, double longitude) {
    }

    private record DecodeResult(int delta, int nextIndex) {
    }
}
