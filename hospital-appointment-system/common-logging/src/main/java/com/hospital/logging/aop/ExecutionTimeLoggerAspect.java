package com.hospital.logging.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.slf4j.MDC;

@Aspect
@Component
public class ExecutionTimeLoggerAspect {

    @Around("@annotation(com.hospital.logging.aop.LogExecutionTime) || execution(* com.hospital..service..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        long start = System.currentTimeMillis();

        try {
            return joinPoint.proceed();
        } finally {
            long executionTime = System.currentTimeMillis() - start;
            MDC.put("executionTimeMs", String.valueOf(executionTime));
            MDC.put("method", joinPoint.getSignature().getName());
            MDC.put("class", joinPoint.getTarget().getClass().getSimpleName());
            
            logger.info("Method {} executed in {} ms", joinPoint.getSignature().getName(), executionTime);
            
            MDC.remove("executionTimeMs");
            MDC.remove("method");
            MDC.remove("class");
        }
    }
}
