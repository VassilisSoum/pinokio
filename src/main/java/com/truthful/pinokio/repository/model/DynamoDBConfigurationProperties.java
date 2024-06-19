package com.truthful.pinokio.repository.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.dynamodb")
@Getter
@Setter
public class DynamoDBConfigurationProperties {

  @NotNull
  @NotEmpty
  private String table;

  @NotNull
  @NotEmpty
  private String region;
}
