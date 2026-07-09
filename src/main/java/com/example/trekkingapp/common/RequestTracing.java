package com.example.trekkingapp.common;

import org.slf4j.MDC;

public final class RequestTracing {

    public static final String REQUEST_ID_KEY = "requestId";
    public static final String REQUEST_ID_ATTRIBUTE = "requestId";

    private RequestTracing() {
    }

    public static String getRequestId() {
        String requestId = MDC.get(REQUEST_ID_KEY);
        return requestId == null || requestId.isBlank() ? "N/A" : requestId;
    }
}
