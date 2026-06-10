package com.training.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String requestUri = getRequestUri();
        String userId = getUserId();
        String redisKey = "idempotent:" + idempotent.key() + ":" + requestUri + ":" + userId;

        Boolean setResult = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "1", idempotent.expireTime(), TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(setResult)) {
            log.warn("重复请求被拦截: key={}", redisKey);
            throw new BusinessException(idempotent.message());
        }

        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            // On failure, remove the key so the request can be retried
            redisTemplate.delete(redisKey);
            throw e;
        }
    }

    private String getRequestUri() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return request.getRequestURI();
        }
        return "unknown";
    }

    private String getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            return authentication.getPrincipal().toString();
        }
        return "anonymous";
    }
}
