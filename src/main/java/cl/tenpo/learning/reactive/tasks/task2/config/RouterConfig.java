package cl.tenpo.learning.reactive.tasks.task2.config;

import cl.tenpo.learning.reactive.tasks.task2.handler.CalculationHandler;
import cl.tenpo.learning.reactive.tasks.task2.handler.UserHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration
public class RouterConfig {
    @Bean
    public RouterFunction<ServerResponse> calculationRoutes(CalculationHandler calculationHandler) {
        return RouterFunctions
                .route(POST("/calculation")
                        .and(accept(MediaType.APPLICATION_JSON)), calculationHandler::calculate);
    }

    @Bean
    public RouterFunction<ServerResponse> userRoutes(UserHandler userHandler) {
        return RouterFunctions
                .route(GET("/users").and(accept(MediaType.APPLICATION_JSON)),
                        userHandler::getAllUsers)
                .andRoute(GET("/users/{id}").and(accept(MediaType.APPLICATION_JSON)),
                        userHandler::getUserById)
                .andRoute(POST("/users").and(accept(MediaType.APPLICATION_JSON)),
                        userHandler::createUser)
                .andRoute(PUT("/users/{id}/deactivate").and(accept(MediaType.APPLICATION_JSON)),
                        userHandler::deactivateUser)
                .andRoute(PUT("/users/{id}/activate").and(accept(MediaType.APPLICATION_JSON)),
                        userHandler::activateUser);
    }
}
