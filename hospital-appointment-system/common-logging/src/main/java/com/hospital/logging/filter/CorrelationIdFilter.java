package com.hospital.logging.filter;

import com.hospital.logging.util.MdcUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        MdcUtil.putCorrelationId(correlationId);
        MdcUtil.putRequestId();
        
        // Add it to the response header as well
        response.addHeader(CORRELATION_ID_HEADER, MdcUtil.getCorrelationId());

        try {
            filterChain.doFilter(request, response);
        } finally {
            MdcUtil.clear();
        }
    }
}
