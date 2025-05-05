package cl.tenpo.learning.reactive.tasks.task2;

import cl.tenpo.learning.reactive.tasks.task2.dto.CreateUserRequestDto;
import cl.tenpo.learning.reactive.tasks.task2.dto.UserResponseDto;
import cl.tenpo.learning.reactive.tasks.task2.model.AuthorizedUser;
import cl.tenpo.learning.reactive.tasks.task2.repository.AuthorizedUserRepository;
import cl.tenpo.learning.reactive.tasks.task2.service.AuthorizedUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Profile;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AuthorizedUserServiceTest {
    @Mock
    private AuthorizedUserRepository userRepository;

    @InjectMocks
    private AuthorizedUserService userService;

    private CreateUserRequestDto validRequest;
    private AuthorizedUser savedUser;

    @BeforeEach
    void setUp() {
        validRequest = new CreateUserRequestDto();
        validRequest.setUsername("testuser");
        validRequest.setEmail("test@example.com");

        savedUser = AuthorizedUser.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createUser_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Mono.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Mono.empty());
        when(userRepository.save(any(AuthorizedUser.class))).thenReturn(Mono.just(savedUser));

        Mono<UserResponseDto> result = userService.createUser(validRequest);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    return response.getId().equals(1L) &&
                            response.getUsername().equals("testuser") &&
                            response.getEmail().equals("test@example.com") &&
                            response.isActive();
                })
                .verifyComplete();

        verify(userRepository).findByUsername("testuser");
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).save(any(AuthorizedUser.class));
    }

    @Test
    void createUser_UsernameAlreadyExists() {
        when(userRepository.findByUsername("testuser")).thenReturn(Mono.just(savedUser));

        Mono<UserResponseDto> result = userService.createUser(validRequest);

        StepVerifier.create(result)
                .expectErrorMatches(error ->
                        error instanceof ResponseStatusException &&
                                ((ResponseStatusException) error).getStatusCode().value() == 400 &&
                                ((ResponseStatusException) error).getReason().equals("Username already exists")
                )
                .verify();

        verify(userRepository).findByUsername("testuser");
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(AuthorizedUser.class));
    }

    @Test
    void createUser_EmailAlreadyExists() {
        when(userRepository.findByUsername("testuser")).thenReturn(Mono.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Mono.just(savedUser));

        Mono<UserResponseDto> result = userService.createUser(validRequest);

        StepVerifier.create(result)
                .expectErrorMatches(error ->
                        error instanceof ResponseStatusException &&
                                ((ResponseStatusException) error).getStatusCode().value() == 400 &&
                                Objects.equals(((ResponseStatusException) error).getReason(), "Email already exists")
                )
                .verify();

        verify(userRepository).findByUsername("testuser");
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository, never()).save(any(AuthorizedUser.class));
    }

    @Test
    void createUser_RepositorySaveError() {
        when(userRepository.findByUsername("testuser")).thenReturn(Mono.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Mono.empty());
        when(userRepository.save(any(AuthorizedUser.class))).thenReturn(Mono.error(new RuntimeException("Database error")));

        Mono<UserResponseDto> result = userService.createUser(validRequest);

        StepVerifier.create(result)
                .expectErrorMatches(error -> error instanceof RuntimeException &&
                        error.getMessage().equals("Database error"))
                .verify();

        verify(userRepository).findByUsername("testuser");
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).save(any(AuthorizedUser.class));
    }

}
