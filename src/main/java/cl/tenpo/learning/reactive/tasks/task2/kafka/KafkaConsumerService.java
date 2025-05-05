package cl.tenpo.learning.reactive.tasks.task2.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

@Slf4j
@Service
public class KafkaConsumerService {
    private final ReactiveKafkaConsumerTemplate<String, ErrorEvent> errorConsumerTemplate;
    private Disposable consumerDisposable;

    public KafkaConsumerService(ReactiveKafkaConsumerTemplate<String, ErrorEvent> errorConsumerTemplate) {
        this.errorConsumerTemplate = errorConsumerTemplate;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void consumeMessages() {
        log.info("Starting reactive Kafka consumer for CR_RETRY_EXHAUSTED topic");
        this.consumerDisposable = errorConsumerTemplate.receive()
                .doOnNext(record -> {
                    ErrorEvent errorEvent = record.value();
                    log.error("Received error event: {}", errorEvent);
                    record.receiverOffset().acknowledge();
                })
                .doOnError(error -> log.error("Error consuming Kafka message: {}", error.getMessage()))
                .retry()
                .subscribe();
    }

    public void stopConsumer() {
        if (consumerDisposable != null) {
            consumerDisposable.dispose();
        }
    }

}
