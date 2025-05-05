package cl.tenpo.learning.reactive.tasks.task2.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;

@Service
@Slf4j
public class KafkaProducerService {
    private static final String RETRY_EXHAUSTED_TOPIC = "CR_RETRY_EXHAUSTED";
    private final ReactiveKafkaProducerTemplate<String, ErrorEvent> errorProducerTemplate;

    public KafkaProducerService(ReactiveKafkaProducerTemplate<String, ErrorEvent> errorProducerTemplate) {
        this.errorProducerTemplate = errorProducerTemplate;
    }

    public Mono<SenderResult<Void>> sendErrorEvent(String errorDetail) {
        ErrorEvent errorEvent = new ErrorEvent(errorDetail);
        log.info("Sending error event to topic {}: {}", RETRY_EXHAUSTED_TOPIC, errorEvent);

        return errorProducerTemplate.send(RETRY_EXHAUSTED_TOPIC, errorEvent)
                .doOnSuccess(result -> log.info("Error event sent successfully to topic {}", RETRY_EXHAUSTED_TOPIC))
                .doOnError(error -> log.error("Failed to send error event to topic {}: {}",
                        RETRY_EXHAUSTED_TOPIC, error.getMessage()));
    }
}
