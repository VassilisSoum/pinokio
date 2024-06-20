package com.truthful.pinokio.config;

import com.truthful.pinokio.config.model.DynamoDBConfigurationProperties;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

@Configuration
public class DynamoDbConfig {

  @Bean
  @Profile("!local && !test")
  public DynamoDbClient dynamoDbClient(
      DynamoDBConfigurationProperties dynamoDBConfigurationProperties) {
    DynamoDbClientBuilder builder =
        DynamoDbClient.builder()
            .region(Region.of(dynamoDBConfigurationProperties.region()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(dynamoDBConfigurationProperties.accessKeyId(),
                    dynamoDBConfigurationProperties.secretAccessKey())));
    return builder.build();
  }

  @Bean
  @Profile("test | local")
  public DynamoDbClient testDynamoDbClient(
      DynamoDBConfigurationProperties dynamoDBConfigurationProperties) {
    DynamoDbClientBuilder builder =
        DynamoDbClient.builder()
            .region(Region.of(dynamoDBConfigurationProperties.region()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(dynamoDBConfigurationProperties.accessKeyId(),
                    dynamoDBConfigurationProperties.secretAccessKey())))
            .endpointOverride(URI.create(dynamoDBConfigurationProperties.endpointUrl()));
    return builder.build();
  }

}
