package com.truthful.pinokio.repository;

import com.soumakis.control.Either;
import com.soumakis.control.Try;
import com.truthful.pinokio.config.model.DynamoDBConfigurationProperties;
import com.truthful.pinokio.service.model.UrlShortenerError;
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
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;

@Repository
@RequiredArgsConstructor
@Slf4j
public class DynamoDBRepository {

  private static final String URL_HASH = "UrlHash";
  private static final String ORIGINAL_URL = "OriginalUrl";
  private static final String CREATED_AT = "CreatedAt";
  private static final String EXPIRES_AT = "ExpiresAt";
  private final DynamoDBConfigurationProperties dynamoDBConfigurationProperties;
  private final DynamoDbClient dynamoDbClient;

  public Either<UrlShortenerError, String> save(
      String hash, String originalUrl, LocalDateTime createdAt, LocalDateTime expiresAt) {
    Map<String, AttributeValue> item = new HashMap<>();
    item.put(URL_HASH, createStringAttribute(hash));
    item.put(ORIGINAL_URL, createStringAttribute(originalUrl));
    item.put(CREATED_AT, createNumberAttribute(createdAt));
    item.put(EXPIRES_AT, createNumberAttribute(expiresAt));

    return Try.of(() -> dynamoDbClient.putItem(constructPutItemRequest(item)))
        .peek(throwable -> log.error("Failed to save to DynamoDB with hash {}", hash, throwable),
            success -> log.info("Saved item to DynamoDB with hash {}", hash))
        .fold(throwable -> handleError(throwable, hash), savedHash -> Either.right(hash));
  }

  public Either<UrlShortenerError, String> getOriginalUrl(String hash) {
    GetItemRequest getItemRequest = createGetItemRequest(hash);

    return Try.of(() -> dynamoDbClient.getItem(getItemRequest))
        .map(DynamoDBRepository::handleGetItemResponse)
        .peek(maybeOriginalUrl -> {
          if (maybeOriginalUrl.isPresent()) {
            log.info("Found item in DynamoDB for hash {}", hash);
          } else {
            log.info("No item found in DynamoDB for hash {}", hash);
          }
        })
        .onFailure(throwable -> log.error("Failed to get {} from DynamoDB", hash, throwable))
        .toEither()
        .leftMap(throwable -> UrlShortenerError.GENERIC_ERROR)
        .flatMap(maybeOriginalUrl -> maybeOriginalUrl
            .map(Either::<UrlShortenerError, String>right)
            .orElse(Either.left(UrlShortenerError.HASH_NOT_FOUND)));
  }

  private static Optional<String> handleGetItemResponse(GetItemResponse response) {
    if (response.hasItem()) {
      return Optional.of(response.item().get(ORIGINAL_URL).s());
    }
    return Optional.empty();
  }

  public Try<Void> delete(String hash) {
    DeleteItemRequest deleteItemRequest = createDeleteItemRequest(hash);

    return Try.of(() -> dynamoDbClient.deleteItem(deleteItemRequest))
        .peek(s -> log.info("Deleted {} from DynamoDB", hash))
        .onFailure(throwable -> log.error("Failed to delete {} from DynamoDB", hash, throwable))
        .map(s -> null);
  }

  private GetItemRequest createGetItemRequest(String shortUrl) {
    return GetItemRequest.builder()
        .tableName(dynamoDBConfigurationProperties.table())
        .key(Map.of(URL_HASH, AttributeValue.builder().s(shortUrl).build()))
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
        .tableName(dynamoDBConfigurationProperties.table())
        .key(Map.of(URL_HASH, AttributeValue.builder().s(shortUrl).build()))
        .build();
  }

  private PutItemRequest constructPutItemRequest(Map<String, AttributeValue> item) {
    return PutItemRequest.builder()
        .tableName(dynamoDBConfigurationProperties.table())
        .conditionExpression("attribute_not_exists(" + URL_HASH + ")")
        .returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.ALL_OLD)
        .item(item)
        .build();
  }

  private Either<UrlShortenerError, String> handleError(Throwable throwable, String hash) {
    log.error("Failed to save to DynamoDB with hash {}", hash, throwable);
    if (throwable instanceof ConditionalCheckFailedException) {
      return Either.left(UrlShortenerError.HASH_ALREADY_EXISTS);
    } else {
      return Either.left(UrlShortenerError.GENERIC_ERROR);
    }
  }
}
