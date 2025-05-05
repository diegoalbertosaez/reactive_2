package cl.tenpo.learning.reactive.tasks.task2;

import cl.tenpo.learning.reactive.tasks.task2.dto.CalculationRequestDto;
import cl.tenpo.learning.reactive.tasks.task2.dto.CalculationResponseDto;
import cl.tenpo.learning.reactive.tasks.task2.kafka.ErrorEvent;
import cl.tenpo.learning.reactive.tasks.task2.testConfig.TestConfig;
import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.test.StepVerifier;

import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
        "test.external-api.fail=true",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@ActiveProfiles("test")
@Profile("test")
@Import(TestConfig.class)
@Slf4j
@EmbeddedKafka(partitions = 1, topics = {"CR_RETRY_EXHAUSTED"})
public class CalculationRetryKafkaITTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ReactiveKafkaConsumerTemplate<String, ErrorEvent> reactiveKafkaConsumerTemplate;

    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    //===== CASOS NO FELICES (Segunda parte)=====//
    @Test
    void shouldRetryAndSendEventToKafkaWhenExternalServiceFails() {
        double cachedPercentage = 15.0;

        redisTemplate.opsForValue().set("external:percentage", cachedPercentage)
                .then(Mono.delay(Duration.ofSeconds(1)))
                .block();

        CalculationRequestDto request = new CalculationRequestDto();
        request.setNumber1(10.0);
        request.setNumber2(20.0);

        Flux<ReceiverRecord<String, ErrorEvent>> kafkaMessages =
                reactiveKafkaConsumerTemplate.receive()
                        .take(1)
                        .timeout(Duration.ofSeconds(20));

        webTestClient.post()
                .uri("/calculation")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CalculationResponseDto.class)
                .value(response -> {
                    // Cálculo esperado: (10 + 20) + 15% = 30 + 4.5 = 34.5
                    assertEquals(34.5, response.getResult());
                });

        StepVerifier.create(kafkaMessages)
                .assertNext(record -> {
                    log.info("Mensaje Kafka recibido: {}", record.value());
                    assertNotNull(record.value());
                    assertNotNull(record.value().getError());

                    String errorMsg = record.value().getError();
                    assertTrue(errorMsg.contains("Retries exhausted: 3/3"));

                    record.receiverOffset().acknowledge();
                })
                .verifyComplete();
    }


}
