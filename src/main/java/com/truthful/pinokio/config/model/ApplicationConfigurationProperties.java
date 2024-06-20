package com.truthful.pinokio.config.model;

import com.truthful.pinokio.service.model.HashingAlgorithm;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "application")
@Validated
public record ApplicationConfigurationProperties(
    @NotNull HashingAlgorithm algorithm,
    @NotNull @Positive Integer maxHashRetries,
    @NotNull @NotEmpty String baseUrl,
    @NotNull @Positive Integer urlExpirationInDays) {

}
