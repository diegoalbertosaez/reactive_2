package cl.tenpo.learning.reactive.tasks.task2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalculationRequestDto {
    @JsonProperty("number_1")
    private Double number1;

    @JsonProperty("number_2")
    private Double number2;
}
