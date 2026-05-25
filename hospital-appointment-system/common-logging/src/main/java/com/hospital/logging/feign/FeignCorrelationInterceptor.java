package com.hospital.logging.feign;

import com.hospital.logging.util.MdcUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class FeignCorrelationInterceptor implements RequestInterceptor {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public void apply(RequestTemplate template) {
        String correlationId = MdcUtil.getCorrelationId();
        if (correlationId != null) {
            template.header(CORRELATION_ID_HEADER, correlationId);
            log.debug("Propagating Correlation ID {} via Feign to {}", correlationId, template.url());
        }
    }
}
