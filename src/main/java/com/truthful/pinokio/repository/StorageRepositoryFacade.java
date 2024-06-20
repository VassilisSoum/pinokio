package com.truthful.pinokio.repository;

import static com.truthful.pinokio.util.RetryUtil.retry;
import static com.truthful.pinokio.util.RetryUtil.retryEither;

import com.soumakis.control.Either;
import com.soumakis.control.EitherT;
import com.soumakis.control.TryT;
import com.truthful.pinokio.service.model.UrlShortenerError;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class StorageRepositoryFacade {

  private final DynamoDBRepository dynamoDBRepository;
  private final Clock clock;
  private final Executor virtualTaskExecutor;

  public EitherT<UrlShortenerError, String> saveHash(
      String hash, String longUrl, int maxRetries, int urlExpirationInDays) {
    CompletableFuture<Either<UrlShortenerError, String>> saveToDynamoDb =
        CompletableFuture.supplyAsync(
            () -> saveToStorage(hash, longUrl, maxRetries, urlExpirationInDays),
            virtualTaskExecutor);

    return EitherT.fromFuture(saveToDynamoDb);
  }

  /**
   * Returns the original URL for the given hash. It retries up to a configurable amount of times
   * before giving up.
   *
   * @param hash the hash to fetch the original URL for
   * @param maxRetries the maximum amount of retries
   * @return the original URL if it exists
   */
  public EitherT<UrlShortenerError, String> getOriginalUrl(String hash, int maxRetries) {
    return EitherT.fromFuture(
        CompletableFuture.supplyAsync(
            () -> retryEither(maxRetries, () -> dynamoDBRepository.getOriginalUrl(hash))));
  }

  /**
   * Deletes the entry for the given hash. It retries up to a configurable amount of times before
   * giving up.
   *
   * @param hash the hash to delete
   * @param maxRetries the maximum amount of retries
   * @return success or error
   */
  public TryT<Void> delete(String hash, int maxRetries) {
    return TryT.fromFuture(
        CompletableFuture.supplyAsync(
            () -> retry(maxRetries, () -> dynamoDBRepository.delete(hash))));
  }

  private Either<UrlShortenerError, String> saveToStorage(
      String hash, String longUrl, int maxRetries, int urlExpirationInDays) {
    var currentDateTime = LocalDateTime.now(clock);
    return retryEither(
        maxRetries,
        () ->
            dynamoDBRepository.save(
                hash, longUrl, currentDateTime, currentDateTime.plusDays(urlExpirationInDays)));
  }
}
