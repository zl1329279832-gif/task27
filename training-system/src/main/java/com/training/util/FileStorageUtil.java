package com.training.util;

import com.training.common.BusinessException;
import com.training.config.FileStorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileStorageUtil {

    private final FileStorageConfig fileStorageConfig;

    /**
     * Store a file and return the relative path (subDir/filename).
     */
    public String store(MultipartFile file, String subDir) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String newFilename = UUID.randomUUID() + extension;
        String relativePath = subDir + "/" + newFilename;

        Path storagePath = Paths.get(fileStorageConfig.getPath()).toAbsolutePath().normalize();
        Path targetDir = storagePath.resolve(subDir);

        try {
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(newFilename);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件存储成功: {}", relativePath);
        } catch (IOException e) {
            log.error("文件存储失败: {}", relativePath, e);
            throw new BusinessException("文件存储失败");
        }

        return relativePath;
    }

    /**
     * Delete a file by its relative path.
     */
    public void delete(String relativePath) {
        Path storagePath = Paths.get(fileStorageConfig.getPath()).toAbsolutePath().normalize();
        Path filePath = storagePath.resolve(relativePath);
        try {
            Files.deleteIfExists(filePath);
            log.info("文件删除成功: {}", relativePath);
        } catch (IOException e) {
            log.error("文件删除失败: {}", relativePath, e);
        }
    }

    /**
     * Get a Resource for the given relative path.
     */
    public Resource getResource(String relativePath) {
        Path storagePath = Paths.get(fileStorageConfig.getPath()).toAbsolutePath().normalize();
        Path filePath = storagePath.resolve(relativePath);
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new BusinessException(404, "文件不存在");
        } catch (MalformedURLException e) {
            log.error("文件路径异常: {}", relativePath, e);
            throw new BusinessException("文件路径异常");
        }
    }
}
