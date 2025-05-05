package cl.tenpo.learning.reactive.tasks.task2.handler;

import cl.tenpo.learning.reactive.tasks.task2.dto.CalculationRequestDto;
import cl.tenpo.learning.reactive.tasks.task2.dto.CalculationResponseDto;
import cl.tenpo.learning.reactive.tasks.task2.model.ErrorResponse;
import cl.tenpo.learning.reactive.tasks.task2.service.CalculationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CalculationHandler {

    private final CalculationService calculationService;

    public CalculationHandler(CalculationService calculationService) {
        this.calculationService = calculationService;
    }

    public Mono<ServerResponse> calculate(ServerRequest request) {
        return request.bodyToMono(CalculationRequestDto.class)
                .flatMap(this::validateRequest)
                .flatMap(req -> calculationService.calculateWithPercentage(req.getNumber1(), req.getNumber2()))
                .map(CalculationResponseDto::new)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .onErrorResume(this::handleError);
    }

    private Mono<CalculationRequestDto> validateRequest(CalculationRequestDto request) {
        if (request.getNumber1() == null || request.getNumber2() == null) {
            return Mono.error(new IllegalArgumentException("Both numbers are required"));
        }
        return Mono.just(request);
    }

    private Mono<ServerResponse> handleError(Throwable error) {
        log.error("Error processing calculation: {}", error.getMessage());

        if (error instanceof IllegalArgumentException) {
            return ServerResponse.badRequest()
                    .bodyValue(new ErrorResponse("400 BAD_REQUEST", error.getMessage()));
        }

        return ServerResponse.status(500)
                .bodyValue(new ErrorResponse("500 INTERNAL_SERVER_ERROR", "An unexpected error occurred"));
    }
}
