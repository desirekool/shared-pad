package com.syncdocs.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class WebSocketConfigTest {

    @Autowired
    @Qualifier("webSocketConfig")
    private WebSocketConfig webSocketConfig;

    @Test
    void contextLoads_ShouldConfigureWebSocket() {
        assertNotNull(webSocketConfig);
    }
}
