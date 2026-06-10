package com.documentintelligenceapplication.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RagRequest {

    @NotBlank(message = "Question cannot be blank")
    private String question;
}
