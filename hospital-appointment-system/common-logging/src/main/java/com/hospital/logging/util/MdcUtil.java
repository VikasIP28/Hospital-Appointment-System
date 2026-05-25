package com.hospital.logging.util;

import org.slf4j.MDC;
import java.util.UUID;

public class MdcUtil {
    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String REQUEST_ID_KEY = "requestId";

    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID_KEY);
    }

    public static void putCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(CORRELATION_ID_KEY, correlationId);
    }

    public static void putRequestId() {
        MDC.put(REQUEST_ID_KEY, UUID.randomUUID().toString());
    }

    public static void clear() {
        MDC.clear();
    }
}
