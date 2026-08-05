package com.syncdocs.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class KafkaProducerConfigTest {

    @Autowired private KafkaTemplate<?, ?> kafkaTemplate;
    @Autowired private ProducerFactory<?, ?> producerFactory;

    @Test
    void contextLoads_ShouldCreateKafkaBeans() {
        assertNotNull(kafkaTemplate);
        assertNotNull(producerFactory);
    }
}
