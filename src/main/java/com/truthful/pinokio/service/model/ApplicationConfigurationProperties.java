package com.truthful.pinokio.service.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application")
@Getter
@Setter
public class ApplicationConfigurationProperties {

  @NotEmpty
  private HashingAlgorithm algorithm;

  @NotEmpty
  private String redisFilterName;

  @NotNull
  @Positive
  private Long redisFilterCapacity;

  @NotNull
  @Positive
  private Integer redisFilterBuckets;

  @NotNull
  @Positive
  private Integer redisFilterMaxIterations;

  @NotNull
  @Positive
  private Integer maxHashRetries;

  @NotNull
  @NotEmpty
  private String baseUrl;

  @NotNull
  @Positive
  private Integer urlExpirationInDays;
}
