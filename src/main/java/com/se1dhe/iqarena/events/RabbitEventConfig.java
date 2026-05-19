package com.se1dhe.iqarena.events;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Конфигурация RabbitMQ exchange для доменных событий IQ Arena.
@Configuration
@EnableConfigurationProperties(OutboxRabbitProperties.class)
public class RabbitEventConfig {
    @Bean
    public TopicExchange domainEventsExchange(OutboxRabbitProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }
}
