package cl.tenpo.learning.reactive.tasks.task2.service;

import cl.tenpo.learning.reactive.tasks.task2.external.PercentageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class CalculationService {
    private final PercentageClient externalPercentageClient;

    public CalculationService(PercentageClient externalPercentageClient) {
        this.externalPercentageClient = externalPercentageClient;
    }

    public Mono<Double> calculateWithPercentage(Double number1, Double number2) {
        double sum = number1 + number2;
        return externalPercentageClient.getRandomPercentage()
                .map(percentage -> {
                    double percentageAmount = sum * (percentage / 100.0);
                    double result = sum + percentageAmount;
                    log.info("Input numbers: {} + {} = {}", number1, number2, sum);
                    log.info("Applied percentage: {}%, amount: {}", percentage, percentageAmount);
                    log.info("Final result: {}", result);
                    return result;
                });
    }
}
