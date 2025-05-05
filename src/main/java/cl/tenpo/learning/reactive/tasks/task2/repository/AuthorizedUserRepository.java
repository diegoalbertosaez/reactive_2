package cl.tenpo.learning.reactive.tasks.task2.repository;

import cl.tenpo.learning.reactive.tasks.task2.model.AuthorizedUser;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface AuthorizedUserRepository extends ReactiveCrudRepository<AuthorizedUser, Long> {
    Mono<AuthorizedUser> findByUsername(String username);

    Mono<AuthorizedUser> findByEmail(String email);

    @Query("UPDATE authorized_users SET active = :active, updated_at = now() WHERE id = :id")
    Mono<Void> updateActiveStatus(Long id, boolean active);
}
