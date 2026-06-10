package com.training.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CertNoGenerator {

    private final StringRedisTemplate stringRedisTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Generate a certificate number in the format: CERT-{courseId}-{yyyyMMdd}-{4digitSeq}
     */
    public String generate(Long courseId) {
        String dateStr = LocalDate.now().format(DATE_FORMATTER);
        String redisKey = "cert:seq:" + courseId + ":" + dateStr;

        Long seq = stringRedisTemplate.opsForValue().increment(redisKey);
        if (seq != null && seq == 1L) {
            // First use of this key — set TTL of 2 days
            stringRedisTemplate.expire(redisKey, 2, TimeUnit.DAYS);
        }

        String seqStr = String.format("%04d", seq);
        String certNo = "CERT-" + courseId + "-" + dateStr + "-" + seqStr;
        log.info("生成证书编号: {}", certNo);
        return certNo;
    }
}
