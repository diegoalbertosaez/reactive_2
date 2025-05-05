package cl.tenpo.learning.reactive.tasks.task2;

import cl.tenpo.learning.reactive.tasks.task2.dto.CalculationRequestDto;
import cl.tenpo.learning.reactive.tasks.task2.dto.CalculationResponseDto;
import cl.tenpo.learning.reactive.tasks.task2.external.PercentageClient;
import cl.tenpo.learning.reactive.tasks.task2.kafka.ErrorEvent;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Profile("test")
public class CalculationITTest {
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ReactiveKafkaConsumerTemplate<String, ErrorEvent> errorConsumerTemplate;

    @MockBean
    private PercentageClient externalPercentageClient;

    //===== CASOS FELICES =====//

    @Test
    void calculate_HappyPath() {
        when(externalPercentageClient.getRandomPercentage()).thenReturn(Mono.just(15.0));

        CalculationRequestDto request = new CalculationRequestDto();
        request.setNumber1(10.0);
        request.setNumber2(20.0);

        webTestClient.post()
                .uri("/calculation")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CalculationResponseDto.class)
                .value(response -> {
                    // Con el porcentaje de 15%: (10+20)+15% = 34.5
                    assert response.getResult() == 34.5;
                });
    }

    @Test
    void calculate_RetryThenSuccess() {

        when(externalPercentageClient.getRandomPercentage()).thenReturn(Mono.just(15.0));

        CalculationRequestDto request = new CalculationRequestDto();
        request.setNumber1(15.0);
        request.setNumber2(25.0);

        webTestClient.post()
                .uri("/calculation")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CalculationResponseDto.class)
                .value(response -> {
                    // (15+25)+15% = 46
                    assert response.getResult() == 46.0;
                });
    }

    //===== CASOS NO FELICES (Primera parte)=====//

    @Test
    void calculate_InvalidRequest() {
        CalculationRequestDto request = new CalculationRequestDto();

        webTestClient.post()
                .uri("/calculation")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists();
    }

}
