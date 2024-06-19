package com.truthful.pinokio.repository;

import com.truthful.pinokio.repository.model.DynamoDBConfigurationProperties;
import io.vavr.control.Try;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

@Repository
@RequiredArgsConstructor
@Slf4j
public class DynamoDBRepository implements StorageRepository {

  private static final String HASH = "Hash";
  private static final String ORIGINAL_URL = "OriginalUrl";
  private static final String CREATED_AT = "CreatedAt";
  private static final String EXPIRES_AT = "ExpiresAt";
  private final DynamoDBConfigurationProperties dynamoDBConfigurationProperties;
  private final DynamoDbClient dynamoDbClient;

  @Override
  public Try<Void> save(
      String hash, String originalUrl, LocalDateTime createdAt, LocalDateTime expiresAt) {
    Map<String, AttributeValue> item = new HashMap<>();
    item.put(HASH, createStringAttribute(hash));
    item.put(ORIGINAL_URL, createStringAttribute(originalUrl));
    item.put(CREATED_AT, createNumberAttribute(createdAt));
    item.put(EXPIRES_AT, createNumberAttribute(expiresAt));

    PutItemRequest putItemRequest =
        PutItemRequest.builder()
            .tableName(dynamoDBConfigurationProperties.getTable())
            .item(item)
            .build();

    return Try.run(() -> dynamoDbClient.putItem(putItemRequest))
        .onFailure(throwable -> log.error("Failed to save item to DynamoDB", throwable));
  }

  @Override
  public Try<Optional<String>> getOriginalUrl(String hash) {
    GetItemRequest getItemRequest = createGetItemRequest(hash);

    return Try.ofCallable(
            () -> {
              var response = dynamoDbClient.getItem(getItemRequest);
              if (response.hasItem()) {
                log.info("Found item in DynamoDB for hash {}", hash);
                return Optional.of(response.item().get(ORIGINAL_URL).s());
              } else {
                log.info("No item found in DynamoDB for hash {}", hash);
                return Optional.<String>empty();
              }
            })
        .onFailure(throwable -> log.error("Failed to get {} from DynamoDB", hash, throwable));
  }

  @Override
  public Try<Void> deleteUrl(String hash) {
    DeleteItemRequest deleteItemRequest = createDeleteItemRequest(hash);

    return Try.run(() -> dynamoDbClient.deleteItem(deleteItemRequest))
        .peek(s -> log.info("Deleted {} from DynamoDB", hash))
        .onFailure(throwable -> log.error("Failed to delete {} from DynamoDB", hash, throwable));
  }

  private GetItemRequest createGetItemRequest(String shortUrl) {
    return GetItemRequest.builder()
        .tableName(dynamoDBConfigurationProperties.getTable())
        .key(Map.of(HASH, AttributeValue.builder().s(shortUrl).build()))
        .build();
  }

  private AttributeValue createStringAttribute(String value) {
    return AttributeValue.builder().s(value).build();
  }

  private AttributeValue createNumberAttribute(LocalDateTime dateTime) {
    long epochMilli = ZonedDateTime.of(dateTime, ZoneId.of("UTC")).toInstant().toEpochMilli();
    return AttributeValue.builder().n(String.valueOf(epochMilli)).build();
  }

  private DeleteItemRequest createDeleteItemRequest(String shortUrl) {
    return DeleteItemRequest.builder()
        .tableName(dynamoDBConfigurationProperties.getTable())
        .key(Map.of(HASH, AttributeValue.builder().s(shortUrl).build()))
        .build();
  }
}
