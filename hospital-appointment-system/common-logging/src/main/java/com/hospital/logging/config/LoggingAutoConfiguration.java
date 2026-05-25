package com.hospital.logging.config;

import com.hospital.logging.aop.ExecutionTimeLoggerAspect;
import com.hospital.logging.exception.GlobalExceptionHandler;
import com.hospital.logging.feign.FeignCorrelationInterceptor;
import com.hospital.logging.filter.CorrelationIdFilter;
import com.hospital.logging.filter.RequestResponseLoggingFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingAutoConfiguration {

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public RequestResponseLoggingFilter requestResponseLoggingFilter() {
        return new RequestResponseLoggingFilter();
    }

    @Bean
    public ExecutionTimeLoggerAspect executionTimeLoggerAspect() {
        return new ExecutionTimeLoggerAspect();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public FeignCorrelationInterceptor feignCorrelationInterceptor() {
        return new FeignCorrelationInterceptor();
    }
}
