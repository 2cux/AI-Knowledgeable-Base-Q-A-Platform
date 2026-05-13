package com.example.aikb.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

    private final AppDocumentProcessMqProperties properties;
    private final AppDocumentEmbeddingMqProperties embeddingProperties;

    @Bean
    public DirectExchange documentProcessExchange() {
        return new DirectExchange(properties.getExchange(), true, false);
    }

    @Bean
    public DirectExchange documentEmbeddingExchange() {
        return new DirectExchange(embeddingProperties.getExchange(), true, false);
    }

    @Bean
    public DirectExchange documentProcessDeadLetterExchange() {
        return new DirectExchange(properties.getDeadLetterExchange(), true, false);
    }

    @Bean
    public DirectExchange documentEmbeddingDeadLetterExchange() {
        return new DirectExchange(embeddingProperties.getDeadLetterExchange(), true, false);
    }

    @Bean
    public Queue documentProcessQueue() {
        return QueueBuilder.durable(properties.getQueue())
                .deadLetterExchange(properties.getDeadLetterExchange())
                .deadLetterRoutingKey(properties.getDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Queue documentProcessDeadLetterQueue() {
        return QueueBuilder.durable(properties.getDeadLetterQueue()).build();
    }

    @Bean
    public Queue documentEmbeddingQueue() {
        return QueueBuilder.durable(embeddingProperties.getQueue())
                .deadLetterExchange(embeddingProperties.getDeadLetterExchange())
                .deadLetterRoutingKey(embeddingProperties.getDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Queue documentEmbeddingDeadLetterQueue() {
        return QueueBuilder.durable(embeddingProperties.getDeadLetterQueue()).build();
    }

    @Bean
    public Binding documentProcessBinding() {
        return BindingBuilder.bind(documentProcessQueue())
                .to(documentProcessExchange())
                .with(properties.getRoutingKey());
    }

    @Bean
    public Binding documentProcessDeadLetterBinding() {
        return BindingBuilder.bind(documentProcessDeadLetterQueue())
                .to(documentProcessDeadLetterExchange())
                .with(properties.getDeadLetterRoutingKey());
    }

    @Bean
    public Binding documentEmbeddingBinding() {
        return BindingBuilder.bind(documentEmbeddingQueue())
                .to(documentEmbeddingExchange())
                .with(embeddingProperties.getRoutingKey());
    }

    @Bean
    public Binding documentEmbeddingDeadLetterBinding() {
        return BindingBuilder.bind(documentEmbeddingDeadLetterQueue())
                .to(documentEmbeddingDeadLetterExchange())
                .with(embeddingProperties.getDeadLetterRoutingKey());
    }

    @Bean
    public MessageConverter rabbitJsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(@NonNull ConnectionFactory connectionFactory,
            @NonNull MessageConverter rabbitJsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitJsonMessageConverter);
        rabbitTemplate.setMandatory(true);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            @NonNull SimpleRabbitListenerContainerFactoryConfigurer configurer,
            @NonNull ConnectionFactory connectionFactory,
            @NonNull MessageConverter rabbitJsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(rabbitJsonMessageConverter);
        factory.setPrefetchCount(1);
        return factory;
    }
}
