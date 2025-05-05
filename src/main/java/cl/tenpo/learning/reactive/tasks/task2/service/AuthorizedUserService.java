package cl.tenpo.learning.reactive.tasks.task2.service;

import cl.tenpo.learning.reactive.tasks.task2.repository.AuthorizedUserRepository;
import cl.tenpo.learning.reactive.tasks.task2.dto.CreateUserRequestDto;
import cl.tenpo.learning.reactive.tasks.task2.dto.UserResponseDto;
import cl.tenpo.learning.reactive.tasks.task2.model.AuthorizedUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AuthorizedUserService {
    private final AuthorizedUserRepository userRepository;

    public AuthorizedUserService(AuthorizedUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<UserResponseDto> createUser(CreateUserRequestDto request) {
        return userRepository.findByUsername(request.getUsername())
                .flatMap(existingUser -> Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Username already exists")))
                .switchIfEmpty(Mono.defer(() ->
                        userRepository.findByEmail(request.getEmail())
                                .flatMap(existingUser -> Mono.error(new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST, "Email already exists")))
                                .switchIfEmpty(Mono.defer(() -> {
                                    LocalDateTime now = LocalDateTime.now();
                                    AuthorizedUser newUser = AuthorizedUser.builder()
                                            .username(request.getUsername())
                                            .email(request.getEmail())
                                            .active(true)
                                            .createdAt(now)
                                            .updatedAt(now)
                                            .build();
                                    return userRepository.save(newUser);
                                }))
                ))
                .cast(AuthorizedUser.class)
                .map(this::mapToResponse)
                .doOnSuccess(user -> log.info("Created new authorized user: {}", user.getUsername()));
    }

    public Mono<UserResponseDto> deactivateUser(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found")))
                .flatMap(user -> {
                    if (!user.isActive()) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "User is already inactive"));
                    }
                    return userRepository.updateActiveStatus(id, false)
                            .then(Mono.defer(() -> userRepository.findById(id)));
                })
                .map(this::mapToResponse)
                .doOnSuccess(user -> log.info("Deactivated user: {}", user.getUsername()));
    }

    public Mono<UserResponseDto> activateUser(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found")))
                .flatMap(user -> {
                    if (user.isActive()) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "User is already active"));
                    }
                    return userRepository.updateActiveStatus(id, true)
                            .then(Mono.defer(() -> userRepository.findById(id)));
                })
                .map(this::mapToResponse)
                .doOnSuccess(user -> log.info("Activated user: {}", user.getUsername()));
    }

    public Flux<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .map(this::mapToResponse);
    }

    public Mono<UserResponseDto> getUserById(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found")))
                .map(this::mapToResponse);
    }

    private UserResponseDto mapToResponse(AuthorizedUser user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .active(user.isActive())
                .build();
    }

}
