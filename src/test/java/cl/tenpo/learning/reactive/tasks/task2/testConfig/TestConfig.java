package cl.tenpo.learning.reactive.tasks.task2.testConfig;

import cl.tenpo.learning.reactive.tasks.task2.kafka.ErrorEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverOptions;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Configuration
public class TestConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "test.external-api.fail", havingValue = "true")
    public WebClient.Builder failingWebClientBuilder() {
        return WebClient.builder()
                .filter((request, next) -> {
                    if (request.url().getPath().contains("/percentage")) {
                        return Mono.error(new IOException("Connection refused (simulated for test)"));
                    }
                    return next.exchange(request);
                });
    }

    @Bean
    public WebTestClient webTestClient(@Value("${server.port}") final int port,
                                       @Value("${spring.webflux.base-path}") final String contextPath) {
        return WebTestClient.bindToServer()
                .responseTimeout(Duration.ofSeconds(30))
                .baseUrl("http://localhost:" + port + contextPath)
                .build();
    }

    @Bean
    public ReactiveKafkaConsumerTemplate<String, ErrorEvent> reactiveKafkaConsumerTemplate(
            Optional<EmbeddedKafkaBroker> embeddedKafkaBroker) {
        if (embeddedKafkaBroker.isEmpty()) {
            return null;
        }
        EmbeddedKafkaBroker embeddedKafka = embeddedKafkaBroker.get();
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ErrorEvent.class.getName());

        ReceiverOptions<String, ErrorEvent> receiverOptions =
                ReceiverOptions.<String, ErrorEvent>create(consumerProps)
                        .subscription(Collections.singletonList("CR_RETRY_EXHAUSTED"));

        return new ReactiveKafkaConsumerTemplate<>(receiverOptions);
    }
}
