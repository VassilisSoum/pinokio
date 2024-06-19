package com.truthful.pinokio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.truthful.pinokio.repository.RedisRepository;
import com.truthful.pinokio.repository.StorageRepository;
import com.truthful.pinokio.service.model.ApplicationConfigurationProperties;
import com.truthful.pinokio.service.model.HashingAlgorithm;
import io.vavr.control.Try;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultUrlShortenerServiceTest {

  @Mock private ApplicationConfigurationProperties applicationConfigurationProperties;
  @Mock private StorageRepository storageRepository;
  @Mock private RedisRepository redisRepository;

  private final Clock clock =
      Clock.fixed(LocalDateTime.now().toInstant(ZoneOffset.UTC), ZoneId.systemDefault());
  private DefaultUrlShortenerService defaultUrlShortenerService;

  @BeforeEach
  void setUp() {
    defaultUrlShortenerService =
        new DefaultUrlShortenerService(
            applicationConfigurationProperties,
            storageRepository,
            new RetryService(),
            redisRepository,
            clock);
  }

  @Test
  @DisplayName("should return shortened url when shorten url service is successful")
  void shouldReturnShortenedUrlWhenShortenUrlServiceIsSuccessful() {
    String longUrl = "http://longurl.com";
    String baseUrl = "http://shorturl.com";
    int maxRetries = 3;
    int urlExpirationInDays = 7;
    ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
    when(applicationConfigurationProperties.getBaseUrl()).thenReturn(baseUrl);
    when(applicationConfigurationProperties.getAlgorithm()).thenReturn(HashingAlgorithm.MURMUR32);
    when(applicationConfigurationProperties.getMaxHashRetries()).thenReturn(maxRetries);
    when(applicationConfigurationProperties.getUrlExpirationInDays())
        .thenReturn(urlExpirationInDays);
    when(redisRepository.isHashAlreadyInUse(hashCaptor.capture())).thenReturn(Try.success(false));
    when(redisRepository.saveHash(any())).thenReturn(Try.success(null));
    when(storageRepository.save(
            any(), eq(longUrl), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(Try.success(null));

    Try<String> result = defaultUrlShortenerService.shortenUrl(longUrl);

    assertTrue(result.isSuccess());
    assertTrue(result.get().startsWith(baseUrl));

    verify(redisRepository).isHashAlreadyInUse(hashCaptor.getValue());
    verify(redisRepository).saveHash(hashCaptor.getValue());
    verify(storageRepository)
        .save(
            eq(hashCaptor.getValue()),
            eq(longUrl),
            any(LocalDateTime.class),
            any(LocalDateTime.class));
  }

  @Test
  @DisplayName(
      "should return error when shorten url service fails when saving the hash to redis "
          + "and exceeding max retries")
  void shouldReturnErrorWhenSavingToRedisFails() {
    String longUrl = "http://longurl.com";
    int maxRetries = 3;
    ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
    when(applicationConfigurationProperties.getAlgorithm()).thenReturn(HashingAlgorithm.MURMUR32);
    when(applicationConfigurationProperties.getMaxHashRetries()).thenReturn(maxRetries);
    when(redisRepository.isHashAlreadyInUse(hashCaptor.capture())).thenReturn(Try.success(false));
    when(redisRepository.saveHash(any())).thenReturn(Try.failure(new RuntimeException("error")));

    Try<String> result = defaultUrlShortenerService.shortenUrl(longUrl);

    assertTrue(result.isFailure());

    verify(redisRepository).isHashAlreadyInUse(hashCaptor.getValue());
    verify(redisRepository, times(3)).saveHash(hashCaptor.getValue());
  }

  @Test
  @DisplayName(
      "should return error when shorten url service fails when saving the hash to storage "
          + "and exceeding max retries")
  void shouldReturnErrorWhenSavingToStorageFails() {
    String longUrl = "http://longurl.com";
    int maxRetries = 3;
    int urlExpirationInDays = 7;
    ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
    when(applicationConfigurationProperties.getAlgorithm()).thenReturn(HashingAlgorithm.MURMUR32);
    when(applicationConfigurationProperties.getMaxHashRetries()).thenReturn(maxRetries);
    when(applicationConfigurationProperties.getUrlExpirationInDays())
        .thenReturn(urlExpirationInDays);
    when(redisRepository.isHashAlreadyInUse(hashCaptor.capture())).thenReturn(Try.success(false));
    when(redisRepository.saveHash(any())).thenReturn(Try.success(null));
    when(storageRepository.save(
            any(), eq(longUrl), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(Try.failure(new RuntimeException("error")));

    Try<String> result = defaultUrlShortenerService.shortenUrl(longUrl);

    assertTrue(result.isFailure());

    verify(redisRepository).isHashAlreadyInUse(hashCaptor.getValue());
    verify(redisRepository).saveHash(hashCaptor.getValue());
    verify(storageRepository, times(3))
        .save(
            eq(hashCaptor.getValue()),
            eq(longUrl),
            any(LocalDateTime.class),
            any(LocalDateTime.class));
  }

  @Test
  @DisplayName("should return original url when get url service is successful")
  void shouldReturnOriginalUrlWhenGetUrlServiceIsSuccessful() {
    String shortUrl = "http://shorturl.com/hash";
    String longUrl = "http://longurl.com";
    int maxRetries = 3;

    when(applicationConfigurationProperties.getMaxHashRetries()).thenReturn(maxRetries);
    when(redisRepository.isHashAlreadyInUse("hash")).thenReturn(Try.success(true));
    when(storageRepository.getOriginalUrl("hash")).thenReturn(Try.success(Optional.of(longUrl)));

    Try<Optional<String>> result = defaultUrlShortenerService.getOriginalUrl(shortUrl);

    assertTrue(result.isSuccess());
    assertTrue(result.get().isPresent());
    assertThat(result.get().get()).isEqualTo(longUrl);

    verify(storageRepository).getOriginalUrl("hash");
    verify(redisRepository).isHashAlreadyInUse("hash");
  }

  @Test
  @DisplayName(
      "should return error when get url service fails when fetching the url from storage "
          + "and exceeding max retries")
  void shouldReturnErrorWhenFetchingUrlFromStorageFails() {
    String shortUrl = "http://shorturl.com/hash";
    int maxRetries = 3;

    when(applicationConfigurationProperties.getMaxHashRetries()).thenReturn(maxRetries);
    when(redisRepository.isHashAlreadyInUse("hash")).thenReturn(Try.success(true));
    when(storageRepository.getOriginalUrl("hash"))
        .thenReturn(Try.failure(new RuntimeException("error")));

    Try<Optional<String>> result = defaultUrlShortenerService.getOriginalUrl(shortUrl);

    assertTrue(result.isFailure());

    verify(storageRepository, times(3)).getOriginalUrl("hash");
    verify(redisRepository).isHashAlreadyInUse("hash");
  }

  @Test
  @DisplayName(
      "should return error when get url service fails when checking the url against redis "
          + "and exceeding max retries")
  void shouldReturnErrorWhenCheckingUrlAgainstRedisFails() {
    String shortUrl = "http://shorturl.com/hash";
    int maxRetries = 3;

    when(applicationConfigurationProperties.getMaxHashRetries()).thenReturn(maxRetries);
    when(redisRepository.isHashAlreadyInUse("hash"))
        .thenReturn(Try.failure(new RuntimeException("error")));

    Try<Optional<String>> result = defaultUrlShortenerService.getOriginalUrl(shortUrl);

    assertTrue(result.isFailure());

    verify(redisRepository, times(3)).isHashAlreadyInUse("hash");
  }

  @Test
  @DisplayName("should return an empty optional when the hash does not exist in redis")
  void shouldReturnEmptyOptionalWhenHashDoesNotExistInRedis() {
    String shortUrl = "http://shorturl.com/hash";
    int maxRetries = 3;

    when(applicationConfigurationProperties.getMaxHashRetries()).thenReturn(maxRetries);
    when(redisRepository.isHashAlreadyInUse("hash")).thenReturn(Try.success(false));

    Try<Optional<String>> result = defaultUrlShortenerService.getOriginalUrl(shortUrl);

    assertTrue(result.isSuccess());
    assertTrue(result.get().isEmpty());

    verify(redisRepository).isHashAlreadyInUse("hash");
  }

  @Test
  @DisplayName("should return success when deleting the url is successful")
  void shouldReturnSuccessWhenDeletingUrlIsSuccessful() {
    String shortUrl = "http://shorturl.com/hash";
    int maxRetries = 3;

    when(applicationConfigurationProperties.getMaxHashRetries()).thenReturn(maxRetries);
    when(redisRepository.isHashAlreadyInUse("hash")).thenReturn(Try.success(true));
    when(redisRepository.deleteHash("hash")).thenReturn(Try.success(null));
    when(storageRepository.deleteUrl("hash")).thenReturn(Try.success(null));

    Try<Void> result = defaultUrlShortenerService.deleteUrl(shortUrl);

    assertTrue(result.isSuccess());

    verify(storageRepository).deleteUrl("hash");
    verify(redisRepository).isHashAlreadyInUse("hash");
  }

  @Test
  @DisplayName(
      "should return error when delete url service fails when deleting the url from storage "
          + "and exceeding max retries")
  void shouldReturnErrorWhenDeletingUrlFromStorageFails() {
    String shortUrl = "http://shorturl.com/hash";
    int maxRetries = 3;

    when(applicationConfigurationProperties.getMaxHashRetries()).thenReturn(maxRetries);
    when(redisRepository.isHashAlreadyInUse("hash")).thenReturn(Try.success(true));
    when(storageRepository.deleteUrl("hash"))
        .thenReturn(Try.failure(new RuntimeException("error")));

    Try<Void> result = defaultUrlShortenerService.deleteUrl(shortUrl);

    assertTrue(result.isFailure());

    verify(storageRepository, times(3)).deleteUrl("hash");
    verify(redisRepository).isHashAlreadyInUse("hash");
  }

  @Test
  @DisplayName(
      "should return error when delete url service fails when deleting the url against redis "
          + "and exceeding max retries")
  void shouldReturnErrorWhenCheckingUrlAgainstRedisFailsWhenDeletingUrl() {
    String shortUrl = "http://shorturl.com/hash";
    int maxRetries = 3;

    when(applicationConfigurationProperties.getMaxHashRetries()).thenReturn(maxRetries);
    when(redisRepository.isHashAlreadyInUse("hash")).thenReturn(Try.success(true));
    when(storageRepository.deleteUrl("hash")).thenReturn(Try.success(null));
    when(redisRepository.deleteHash("hash")).thenReturn(Try.failure(new RuntimeException("error")));

    Try<Void> result = defaultUrlShortenerService.deleteUrl(shortUrl);

    assertTrue(result.isFailure());

    verify(storageRepository).deleteUrl("hash");
    verify(redisRepository).isHashAlreadyInUse("hash");
    verify(redisRepository, times(3)).deleteHash("hash");
  }

  @Test
  @DisplayName("should retry the hash generation when the hash is already in use")
  void shouldRetryHashGenerationWhenHashIsAlreadyInUse() {
    String longUrl = "http://longurl.com";
    int maxRetries = 3;
    when(applicationConfigurationProperties.getAlgorithm()).thenReturn(HashingAlgorithm.MURMUR32);
    when(applicationConfigurationProperties.getMaxHashRetries()).thenReturn(maxRetries);
    when(redisRepository.isHashAlreadyInUse(any()))
        .thenReturn(Try.success(true))
        .thenReturn(Try.success(false));
    when(redisRepository.saveHash(any())).thenReturn(Try.success(null));
    when(storageRepository.save(
            any(), eq(longUrl), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(Try.success(null));

    Try<String> result = defaultUrlShortenerService.shortenUrl(longUrl);

    assertTrue(result.isSuccess());

    verify(redisRepository, times(2)).isHashAlreadyInUse(any());
    verify(redisRepository).saveHash(any());
    verify(storageRepository)
        .save(
            any(),
            eq(longUrl),
            any(LocalDateTime.class),
            any(LocalDateTime.class));
  }

  @Test
  @DisplayName("should return error when hash generation fails after max retries")
  void shouldReturnErrorWhenHashGenerationFailsAfterMaxRetries() {
    String longUrl = "http://longurl.com";
    int maxRetries = 3;
    when(applicationConfigurationProperties.getAlgorithm()).thenReturn(HashingAlgorithm.MURMUR32);
    when(applicationConfigurationProperties.getMaxHashRetries()).thenReturn(maxRetries);
    when(redisRepository.isHashAlreadyInUse(any())).thenReturn(Try.success(true));

    Try<String> result = defaultUrlShortenerService.shortenUrl(longUrl);

    assertTrue(result.isFailure());

    verify(redisRepository, times(3)).isHashAlreadyInUse(any());
  }
}
