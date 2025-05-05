package cl.tenpo.learning.reactive.tasks.task2.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
public class PercentageCacheService {
    private static final String PERCENTAGE_CACHE_KEY = "external:percentage";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    public PercentageCacheService(ReactiveRedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Double> getPercentage() {
        return redisTemplate.opsForValue().get(PERCENTAGE_CACHE_KEY)
                .cast(Double.class)
                .doOnSuccess(cachedValue -> {
                    if (cachedValue != null) {
                        log.info("Retrieved percentage from cache: {}", cachedValue);
                    } else {
                        log.info("No percentage found in cache");
                    }
                });
    }

    public Mono<Boolean> savePercentage(Double percentage) {
        log.info("Saving percentage to cache: {}", percentage);
        return redisTemplate.opsForValue()
                .set(PERCENTAGE_CACHE_KEY, percentage, CACHE_TTL)
                .doOnSuccess(result -> log.info("Percentage saved to cache: {}", result));
    }
}
