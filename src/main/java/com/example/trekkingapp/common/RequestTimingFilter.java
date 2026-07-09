package com.example.trekkingapp.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestTimingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestTimingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        long startNanos = System.nanoTime();
        MDC.put(RequestTracing.REQUEST_ID_KEY, requestId);
        request.setAttribute(RequestTracing.REQUEST_ID_ATTRIBUTE, requestId);

        log.info("REQUEST_START method={} path={} requestId={}",
                request.getMethod(),
                request.getRequestURI(),
                requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.info("REQUEST_END method={} path={} status={} duration={}ms requestId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs,
                    requestId);
            MDC.remove(RequestTracing.REQUEST_ID_KEY);
        }
    }
}
