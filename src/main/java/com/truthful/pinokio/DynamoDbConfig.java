package com.truthful.pinokio;

import com.truthful.pinokio.repository.model.DynamoDBConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

@Configuration
public class DynamoDbConfig {

  @Bean
  public DynamoDbClient dynamoDbClient(
      DynamoDBConfigurationProperties dynamoDBConfigurationProperties) {
    DynamoDbClientBuilder builder =
        DynamoDbClient.builder()
            .region(Region.of(dynamoDBConfigurationProperties.getRegion()))
            .credentialsProvider(DefaultCredentialsProvider.create());
    return builder.build();
  }
}
