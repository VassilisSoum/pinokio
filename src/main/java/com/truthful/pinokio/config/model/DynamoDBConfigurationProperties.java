package com.truthful.pinokio.config.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "application.dynamodb")
@Validated
public record DynamoDBConfigurationProperties(
    @NotNull @NotEmpty String table,
    @NotNull @NotEmpty String region,
    @NotNull @NotEmpty String accessKeyId,
    @NotNull @NotEmpty String secretAccessKey,
    String endpointUrl) {

}
