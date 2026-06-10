package com.training.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Component
public class CertNoGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final Random RANDOM = new Random();

    public String generate() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String randomPart = String.format("%06d", RANDOM.nextInt(1000000));
        return "CERT-" + datePart + "-" + randomPart;
    }
}
