package com.example.intercompoc.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    // Intercept all methods in the ConversationService
    @Around("execution(* com.example.intercompoc.service.ConversationService.*(..))")
    public Object logExecutionTimeAndCaller(ProceedingJoinPoint joinPoint) throws Throwable {
        String callerId = "SYSTEM";

        // Extract Caller ID directly from the HTTP Request Context, if this is a web request
        if (RequestContextHolder.getRequestAttributes() != null) {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String header = request.getHeader("X-Caller-Id");
            if (header != null && !header.isEmpty()) {
                callerId = header;
            }
        }

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();
            log.info("[Caller: {}] Successfully executed {}.{} in {} ms", 
                    callerId, className, methodName, stopWatch.getTotalTimeMillis());
            return result;
        } catch (Throwable e) {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            log.error("[Caller: {}] Failed {}.{} in {} ms with exception: {}", 
                    callerId, className, methodName, stopWatch.getTotalTimeMillis(), e.getMessage());
            throw e;
        }
    }
}
