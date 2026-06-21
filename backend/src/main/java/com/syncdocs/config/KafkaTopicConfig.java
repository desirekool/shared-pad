package com.syncdocs.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic documentEditTopic() {
        return TopicBuilder.name("document.edit")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic documentSaveTopic() {
        return TopicBuilder.name("document.save")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic documentCreatedTopic() {
        return TopicBuilder.name("document.created")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic documentDeletedTopic() {
        return TopicBuilder.name("document.deleted")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic documentDownloadTopic() {
        return TopicBuilder.name("document.download")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic documentUploadTopic() {
        return TopicBuilder.name("document.upload")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userPresenceTopic() {
        return TopicBuilder.name("user.presence")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name("audit.events")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
