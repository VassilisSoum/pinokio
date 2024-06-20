package com.truthful.pinokio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.soumakis.control.Either;
import com.soumakis.control.EitherT;
import com.soumakis.control.TryT;
import com.truthful.pinokio.repository.StorageRepositoryFacade;
import com.truthful.pinokio.service.model.UrlShortenerError;
import com.truthful.pinokio.config.model.ApplicationConfigurationProperties;
import com.truthful.pinokio.service.model.HashingAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UrlShortenerServiceTest {

  private static final String LONG_URL = "http://example.com";
  private static final String SHORTEN_BASE_URL = "http://short.com/";
  private static final int MAX_RETRIES = 3;
  private static final int URL_EXPIRATION_IN_DAYS = 30;

  @Mock
  private ApplicationConfigurationProperties applicationConfigurationProperties;

  @Mock
  private StorageRepositoryFacade storageRepositoryFacade;

  @InjectMocks
  private UrlShortenerService urlShortenerService;

  @BeforeEach
  void setUp() {
    lenient().when(applicationConfigurationProperties.baseUrl()).thenReturn(SHORTEN_BASE_URL);
    lenient().when(applicationConfigurationProperties.maxHashRetries()).thenReturn(MAX_RETRIES);
    lenient()
        .when(applicationConfigurationProperties.urlExpirationInDays())
        .thenReturn(URL_EXPIRATION_IN_DAYS);
    lenient()
        .when(applicationConfigurationProperties.algorithm())
        .thenReturn(HashingAlgorithm.MURMUR32);
  }

  @Test
  @DisplayName("shortenUrl should return a shortened URL")
  void shortenUrlShouldReturnShortenedUrl() {
    when(storageRepositoryFacade.saveHash(
        any(), any(), eq(MAX_RETRIES), eq(URL_EXPIRATION_IN_DAYS)))
        .thenReturn(EitherT.right("hash"));

    Either<UrlShortenerError, String> result = urlShortenerService.shortenUrl(LONG_URL)
        .toCompletableFuture().join();

    assertTrue(result.isRight());
    assertThat(result.getRight()).contains(SHORTEN_BASE_URL);
  }

  @Test
  @DisplayName("shortenUrl should return a failure if the URL is not shortened")
  void shortenUrlShouldReturnFailureIfUrlNotShortened() {
    when(storageRepositoryFacade.saveHash(
        any(), any(), eq(MAX_RETRIES), eq(URL_EXPIRATION_IN_DAYS)))
        .thenReturn(EitherT.left(UrlShortenerError.GENERIC_ERROR));

    Either<UrlShortenerError, String> result = urlShortenerService.shortenUrl(LONG_URL)
        .toCompletableFuture().join();

    assertTrue(result.isLeft());
  }

  @Test
  @DisplayName("shortenUrl should retry if the hash already exists")
  void shortenUrlShouldRetryIfHashAlreadyExists() {
    when(storageRepositoryFacade.saveHash(
        any(), any(), eq(MAX_RETRIES), eq(URL_EXPIRATION_IN_DAYS)))
        .thenReturn(EitherT.left(UrlShortenerError.HASH_ALREADY_EXISTS))
        .thenReturn(EitherT.right("hash"));

    Either<UrlShortenerError, String> result = urlShortenerService.shortenUrl(LONG_URL)
        .toCompletableFuture().join();

    assertTrue(result.isRight());
    assertThat(result.getRight()).contains(SHORTEN_BASE_URL);
  }

  @Test
  @DisplayName("getOriginalUrl returns original url")
  public void getOriginalUrl_returnsOriginalUrl() {
    String hash = "abc123";
    when(storageRepositoryFacade.getOriginalUrl(eq(hash), eq(MAX_RETRIES)))
        .thenReturn(EitherT.right(LONG_URL));
    EitherT<UrlShortenerError, String> result = urlShortenerService.getOriginalUrl(
        SHORTEN_BASE_URL + hash);
    assertEquals(LONG_URL, result.toCompletableFuture().join().getRight());
  }

  @Test
  @DisplayName("deleteUrl deletes url")
  public void deleteUrl_deletesUrl() {
    String hash = "abc123";
    when(storageRepositoryFacade.delete(hash, 3)).thenReturn(TryT.of(null));
    TryT<Void> result = urlShortenerService.deleteUrl(SHORTEN_BASE_URL + hash);
    assertTrue(result.toCompletableFuture().join().isSuccess());
  }
}
