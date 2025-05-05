package cl.tenpo.learning.reactive.tasks.task2.external;

import cl.tenpo.learning.reactive.tasks.task2.kafka.KafkaProducerService;
import cl.tenpo.learning.reactive.tasks.task2.service.PercentageCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class PercentageClient {

    private final WebClient webClient;
    private final PercentageCacheService cacheService;
    private final KafkaProducerService kafkaProducerService;
    private static final String BASE_URL = "http://localhost:8083/learning-reactive/external-api";

    public PercentageClient(WebClient.Builder webClientBuilder,
                                    PercentageCacheService cacheService,
                                    KafkaProducerService kafkaProducerService) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
        this.cacheService = cacheService;
        this.kafkaProducerService = kafkaProducerService;
    }

    public Mono<Double> getRandomPercentage() {
        return fetchFromExternalApiWithRetry()
                .flatMap(percentage -> cacheService.savePercentage(percentage).thenReturn(percentage))
                .onErrorResume(error -> {
                    log.error("Error fetching from external API after retries: {}", error.getMessage());
                    return kafkaProducerService.sendErrorEvent(error.getMessage())
                            .then(
                                    cacheService.getPercentage()
                                            .switchIfEmpty(Mono.error(new ResponseStatusException(
                                                    HttpStatus.SERVICE_UNAVAILABLE,
                                                    "External service unavailable after retries and no cached value found")))
                            );
                });
    }

    protected Mono<Double> fetchFromExternalApiWithRetry() {
        return fetchFromExternalApi()
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .doBeforeRetry(retrySignal ->
                                log.warn("Retrying external API call, attempt: {}", retrySignal.totalRetries() + 1))
                );
    }

    private Mono<Double> fetchFromExternalApi() {
        return webClient.get()
                .uri("/percentage")
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(70))
                .doOnSubscribe(subscription -> log.info("Requesting random percentage from external API"))
                .doOnSuccess(response -> log.info("Received response: {}", response))
                .map(response -> {
                    if (response.containsKey("percentage")) {
                        return Double.parseDouble(response.get("percentage").toString());
                    } else {
                        throw new IllegalStateException("Response does not contain percentage key");
                    }
                })
                .doOnError(error -> log.error("Error retrieving percentage: {}", error.getMessage()));
    }
}
