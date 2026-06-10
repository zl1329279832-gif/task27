package com.training.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileStorageConfig {

    @Value("${file.storage.base-path}")
    private String basePath;

    @PostConstruct
    public void init() throws IOException {
        Path storagePath = Paths.get(basePath);
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }
    }
}
