package com.syncdocs.service;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioServiceTest {

    @Mock private MinioClient minioClient;
    private MinioService minioService;

    @Captor private ArgumentCaptor<PutObjectArgs> putCaptor;

    @BeforeEach
    void setUp() {
        minioService = new MinioService(minioClient);
        // Direct field injection since @Value won't resolve in unit tests
        java.lang.reflect.Field bucketField;
        try {
            bucketField = MinioService.class.getDeclaredField("bucket");
            bucketField.setAccessible(true);
            bucketField.set(minioService, "test-bucket");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void putObject_ShouldUploadToMinio() throws Exception {
        byte[] data = "hello".getBytes();
        minioService.putObject("doc/1", data, "text/plain");

        verify(minioClient).putObject(putCaptor.capture());
        assertEquals("test-bucket", putCaptor.getValue().bucket());
        assertEquals("doc/1", putCaptor.getValue().object());
        assertEquals("text/plain", putCaptor.getValue().contentType());
    }

    @Test
    void putObject_ShouldThrowOnFailure() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("Connection failed"));

        assertThrows(RuntimeException.class,
                () -> minioService.putObject("doc/1", "test".getBytes(), null));
    }

    @Test
    void getObject_ShouldReturnBytes() throws Exception {
        GetObjectResponse mockResponse = mock(GetObjectResponse.class);
        when(mockResponse.readAllBytes()).thenReturn("response".getBytes());
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenReturn(mockResponse);

        byte[] result = minioService.getObject("doc/1");

        assertArrayEquals("response".getBytes(), result);
    }

    @Test
    void getObject_ShouldThrowOnFailure() throws Exception {
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new RuntimeException("Not found"));

        assertThrows(RuntimeException.class,
                () -> minioService.getObject("doc/missing"));
    }

    @Test
    void deleteObject_ShouldCallRemove() throws Exception {
        minioService.deleteObject("doc/1");

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void deleteObject_ShouldSwallowException() throws Exception {
        doThrow(new RuntimeException("error"))
                .when(minioClient).removeObject(any(RemoveObjectArgs.class));

        assertDoesNotThrow(() -> minioService.deleteObject("doc/1"));
    }

    @Test
    void objectExists_ShouldReturnTrueWhenFound() throws Exception {
        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenReturn(null);

        assertTrue(minioService.objectExists("doc/1"));
    }

    @Test
    void objectExists_ShouldReturnFalseWhenMissing() throws Exception {
        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenThrow(mock(io.minio.errors.ErrorResponseException.class));

        assertFalse(minioService.objectExists("doc/missing"));
    }
}
