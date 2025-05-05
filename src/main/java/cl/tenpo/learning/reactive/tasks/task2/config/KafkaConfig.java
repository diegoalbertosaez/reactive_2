package cl.tenpo.learning.reactive.tasks.task2.config;

import cl.tenpo.learning.reactive.tasks.task2.kafka.ErrorEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.SenderOptions;

import java.util.Collections;
import java.util.Map;

@Configuration
@Profile("!test")
public class KafkaConfig {
    private static final String RETRY_EXHAUSTED_TOPIC = "CR_RETRY_EXHAUSTED";

    @Bean
    public ReactiveKafkaProducerTemplate<String, ErrorEvent> errorProducerTemplate(KafkaProperties kafkaProperties) {
        Map<String, Object> options = kafkaProperties.buildProducerProperties(null);
        options.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        options.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new ReactiveKafkaProducerTemplate<>(SenderOptions.create(options));
    }

    @Bean
    public ReactiveKafkaConsumerTemplate<String, ErrorEvent> errorConsumerTemplate(KafkaProperties kafkaProperties) {
        Map<String, Object> options = kafkaProperties.buildConsumerProperties(null);
        options.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        options.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        options.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ErrorEvent.class.getName());

        ReceiverOptions<String, ErrorEvent> receiverOptions = ReceiverOptions.<String, ErrorEvent>create(options)
                .subscription(Collections.singletonList(RETRY_EXHAUSTED_TOPIC));

        return new ReactiveKafkaConsumerTemplate<>(receiverOptions);
    }
}
