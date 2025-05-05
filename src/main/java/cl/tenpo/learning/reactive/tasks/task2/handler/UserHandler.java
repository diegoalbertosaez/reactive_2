package cl.tenpo.learning.reactive.tasks.task2.handler;

import cl.tenpo.learning.reactive.tasks.task2.dto.CreateUserRequestDto;
import cl.tenpo.learning.reactive.tasks.task2.dto.UserResponseDto;
import cl.tenpo.learning.reactive.tasks.task2.model.ErrorResponse;
import cl.tenpo.learning.reactive.tasks.task2.service.AuthorizedUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class UserHandler {

    private final AuthorizedUserService userService;

    public UserHandler(AuthorizedUserService userService) {
        this.userService = userService;
    }

    public Mono<ServerResponse> createUser(ServerRequest request) {
        return request.bodyToMono(CreateUserRequestDto.class)
                .flatMap(this::validateCreateRequest)
                .flatMap(userService::createUser)
                .flatMap(user -> ServerResponse.created(request.uriBuilder().path("/{id}").build(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> deactivateUser(ServerRequest request) {
        Long userId = Long.parseLong(request.pathVariable("id"));
        return userService.deactivateUser(userId)
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> activateUser(ServerRequest request) {
        Long userId = Long.parseLong(request.pathVariable("id"));
        return userService.activateUser(userId)
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> getAllUsers(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(userService.getAllUsers(), UserResponseDto.class)
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> getUserById(ServerRequest request) {
        Long userId = Long.parseLong(request.pathVariable("id"));
        return userService.getUserById(userId)
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .onErrorResume(this::handleError);
    }

    private Mono<CreateUserRequestDto> validateCreateRequest(CreateUserRequestDto request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Username cannot be empty"));
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Email cannot be empty"));
        }
        if (!request.getEmail().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            return Mono.error(new IllegalArgumentException("Invalid email format"));
        }
        return Mono.just(request);
    }

    private Mono<ServerResponse> handleError(Throwable error) {
        log.error("Error processing user request: {}", error.getMessage());

        return switch (error) {
            case IllegalArgumentException illegalArgumentException -> ServerResponse.badRequest()
                    .bodyValue(new ErrorResponse("400 BAD_REQUEST", error.getMessage()));
            case ResponseStatusException rse -> ServerResponse.status(rse.getStatusCode())
                    .bodyValue(new ErrorResponse(rse.getStatusCode().toString(), rse.getReason()));
            case DataIntegrityViolationException dataIntegrityViolationException -> ServerResponse.badRequest()
                    .bodyValue(new ErrorResponse("400 BAD_REQUEST", "Username or email already exists"));
            default -> ServerResponse.status(500)
                    .bodyValue(new ErrorResponse("500 INTERNAL_SERVER_ERROR", "An unexpected error occurred"));
        };

    }
}
