package com.syncdocs.service;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    public void putObject(String objectKey, byte[] data, String mimeType) {
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(data), data.length, -1)
                    .contentType(mimeType != null ? mimeType : "application/octet-stream")
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload to MinIO: " + e.getMessage(), e);
        }
    }

    public byte[] getObject(String objectKey) {
        try {
            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {
                return stream.readAllBytes();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read from MinIO: " + e.getMessage(), e);
        }
    }

    public void deleteObject(String objectKey) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception e) {
            log.error("Failed to delete object from MinIO: {}", objectKey, e);
        }
    }

    public boolean objectExists(String objectKey) {
        try {
            minioClient.statObject(
                StatObjectArgs.builder().bucket(bucket).object(objectKey).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
