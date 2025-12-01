package com.daspawnw.sammelalbum.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * HTTP Access Log Filter - Logs requests in nginx-style format:
 * METHOD /path?query HTTP/1.1 STATUS
 */
@Slf4j
@Component
public class AccessLogFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            long startTime = System.currentTimeMillis();

            try {
                chain.doFilter(request, response);
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                String queryString = httpRequest.getQueryString();
                String uri = httpRequest.getRequestURI() + (queryString != null ? "?" + queryString : "");

                log.info("{} {} {} {} {}ms",
                        httpRequest.getMethod(),
                        uri,
                        httpRequest.getProtocol(),
                        httpResponse.getStatus(),
                        duration);
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
