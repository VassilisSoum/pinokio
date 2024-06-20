package com.truthful.pinokio.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.soumakis.control.Either;
import com.soumakis.control.EitherT;
import com.soumakis.control.Try;
import com.soumakis.control.TryT;
import com.truthful.pinokio.service.model.UrlShortenerError;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageRepositoryFacadeTest {

  @Mock
  private DynamoDBRepository dynamoDBRepository;

  private StorageRepositoryFacade storageRepositoryFacade;

  @BeforeEach
  void setUp() {
    Executor executor = Executors.newSingleThreadExecutor();
    Clock fixedClock = Clock.fixed(Instant.parse("2024-07-02T10:15:30.00Z"), ZoneId.of("UTC"));
    storageRepositoryFacade = new StorageRepositoryFacade(dynamoDBRepository, fixedClock, executor);
  }

  @Test
  @DisplayName("saveHash should return shortened URL on success")
  void saveHashShouldReturnShortenedUrlOnSuccess() {
    when(dynamoDBRepository.save(any(), any(), any(), any())).thenReturn(Either.right("shortUrl"));
    EitherT<UrlShortenerError, String> result =
        storageRepositoryFacade.saveHash("hash", "longUrl", 3, 7);
    assertThat(result.toCompletableFuture().join()).isEqualTo(Either.right("shortUrl"));
  }

  @Test
  @DisplayName("saveHash should return error on failure")
  void saveHashShouldReturnErrorOnFailure() {
    when(dynamoDBRepository.save(any(), any(), any(), any()))
        .thenReturn(Either.left(UrlShortenerError.GENERIC_ERROR));
    EitherT<UrlShortenerError, String> result =
        storageRepositoryFacade.saveHash("hash", "longUrl", 3, 7);
    assertThat(result.toCompletableFuture().join()).isEqualTo(
        Either.left(UrlShortenerError.GENERIC_ERROR));
  }

  @Test
  @DisplayName("getOriginalUrl should return URL if present")
  void getOriginalUrlShouldReturnUrlIfPresent() {
    when(dynamoDBRepository.getOriginalUrl(any()))
        .thenReturn(Either.right("originalUrl"));
    EitherT<UrlShortenerError, String> result = storageRepositoryFacade.getOriginalUrl("hash", 3);
    assertThat(result.toCompletableFuture().join()).isEqualTo(Either.right("originalUrl"));
  }

  @Test
  @DisplayName("getOriginalUrl should return HASH_NOT_FOUND if URL not found")
  void getOriginalUrlShouldReturnHashNotFound() {
    when(dynamoDBRepository.getOriginalUrl(any())).thenReturn(
        Either.left(UrlShortenerError.HASH_NOT_FOUND));
    EitherT<UrlShortenerError, String> result = storageRepositoryFacade.getOriginalUrl("hash", 3);
    assertThat(result.toCompletableFuture().join()).isEqualTo(
        Either.left(UrlShortenerError.HASH_NOT_FOUND));
  }

  @Test
  @DisplayName("delete should return success on successful deletion")
  void deleteShouldReturnSuccessOnSuccessfulDeletion() {
    when(dynamoDBRepository.delete(any())).thenReturn(Try.success(null));
    TryT<Void> result = storageRepositoryFacade.delete("hash", 3);
    assertThat(result.toCompletableFuture().join().isSuccess()).isTrue();
  }

  @Test
  @DisplayName("delete should return failure on unsuccessful deletion")
  void deleteShouldReturnFailureOnUnsuccessfulDeletion() {
    when(dynamoDBRepository.delete(any())).thenReturn(Try.failure(new RuntimeException()));
    TryT<Void> result = storageRepositoryFacade.delete("hash", 3);
    assertThat(result.toCompletableFuture().join().isFailure()).isTrue();
  }
}
