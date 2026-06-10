package com.training.util;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class FileStorageUtil {

    @Value("${file.storage.base-path}")
    private String basePath;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(basePath));
        } catch (IOException e) {
            throw new RuntimeException("无法创建文件存储目录: " + basePath, e);
        }
    }

    public String store(MultipartFile file, String subDir) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        Path dirPath = Paths.get(basePath, subDir);
        Files.createDirectories(dirPath);

        Path filePath = dirPath.resolve(uniqueFilename);
        file.transferTo(filePath.toFile());

        return subDir + "/" + uniqueFilename;
    }

    public byte[] load(String relativePath) throws IOException {
        Path filePath = Paths.get(basePath, relativePath);
        return Files.readAllBytes(filePath);
    }

    public boolean delete(String relativePath) {
        try {
            Path filePath = Paths.get(basePath, relativePath);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }
}
