package com.truthful.pinokio.service;

import com.truthful.pinokio.repository.RedisRepository;
import com.truthful.pinokio.repository.StorageRepository;
import com.truthful.pinokio.service.model.ApplicationConfigurationProperties;
import com.truthful.pinokio.service.model.UrlShortenerError;
import io.vavr.control.Either;
import io.vavr.control.Try;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultUrlShortenerService implements UrlShortenerService {

  private final ApplicationConfigurationProperties applicationConfigurationProperties;
  private final StorageRepository storageRepository;
  private final RetryService retryService;
  private final RedisRepository redisRepository;
  private final Clock clock;

  /**
   * Shortens the given URL. If the URL is already shortened, it tries to generate a new hash up to
   * a configurable amount of times before giving up. If the URL is successfully shortened, it saves
   * the hash to Redis and the URL to the storage and then returns the shortened URL.
   *
   * @param longUrl the URL to shorten
   * @return the shortened URL
   */
  @Override
  public Try<String> shortenUrl(String longUrl) {
    return generateHashAndShortenUrl(longUrl)
        .map(
            generatedHash -> applicationConfigurationProperties.getBaseUrl() + "/" + generatedHash);
  }

  /**
   * Fetches the original URL for the given short URL. It first extracts the hash from the URL and
   * then checks if the hash is in use. If the hash is in use, it fetches the original URL from the
   * storage and returns it.
   *
   * @param shortUrl the short URL to fetch the original URL for
   * @return the original URL if it exists
   */
  @Override
  public Try<Optional<String>> getOriginalUrl(String shortUrl) {
    String hash = extractHashFromUrl(shortUrl);
    return checkHashAndFetchUrl(hash);
  }

  /**
   * Deletes the URL for the given short URL. It first extracts the hash from the URL and then
   * checks if the hash is in use. If the hash is in use, it deletes the URL from the storage and
   * Redis.
   *
   * @param shortUrl the short URL to delete
   * @return success or error.
   */
  @Override
  public Try<Void> deleteUrl(String shortUrl) {
    String hash = extractHashFromUrl(shortUrl);
    return checkHashAndDeleteUrl(hash);
  }

  private Try<String> generateHashAndShortenUrl(String longUrl) {
    Either<UrlShortenerError, String> hash = tryGenerateHash(longUrl);
    if (hash.isLeft()) {
      hash = retryOnHashCollision(longUrl, hash.getLeft());
      if (hash.isLeft()) {
        return Try.failure(new RuntimeException("Failed to generate hash. Giving up."));
      }
    }
    return saveHashToRedisAndStorage(hash.get(), longUrl);
  }

  private Either<UrlShortenerError, String> tryGenerateHash(String longUrl) {
    String hash = generateHash(longUrl);
    return checkHashInRedis(hash)
        .toEither()
        .mapLeft(throwable -> UrlShortenerError.INTERNAL_ERROR)
        .flatMap(
            isHashInUse ->
                Boolean.TRUE.equals(isHashInUse)
                    ? Either.left(UrlShortenerError.HASH_ALREADY_IN_USE)
                    : Either.right(hash));
  }

  private String generateHash(String longUrl) {
    return applicationConfigurationProperties
        .getAlgorithm()
        .generateHash(longUrl + UUID.randomUUID());
  }

  private Try<Boolean> checkHashInRedis(String hash) {
    return retryService.retry(
        applicationConfigurationProperties.getMaxHashRetries(),
        () -> redisRepository.isHashAlreadyInUse(hash));
  }

  private Either<UrlShortenerError, String> retryOnHashCollision(
      String longUrl, UrlShortenerError error) {
    if (error == UrlShortenerError.HASH_ALREADY_IN_USE) {
      for (int i = 1; i < applicationConfigurationProperties.getMaxHashRetries(); i++) {
        Either<UrlShortenerError, String> result = tryGenerateHash(longUrl);
        if (result.isRight()) {
          return result;
        }
      }
    }
    log.error(
        "Failed to generate hash for url {} after {} retries",
        longUrl,
        applicationConfigurationProperties.getMaxHashRetries());
    return Either.left(UrlShortenerError.INTERNAL_ERROR);
  }

  private Try<String> saveHashToRedisAndStorage(String hash, String longUrl) {
    return saveHashToRedis(hash).flatMap(success -> saveToStorage(hash, longUrl));
  }

  private Try<Void> saveHashToRedis(String hash) {
    return retryService.retry(
        applicationConfigurationProperties.getMaxHashRetries(),
        () -> redisRepository.saveHash(hash));
  }

  private Try<String> saveToStorage(String hash, String longUrl) {
    var currentDateTime = LocalDateTime.now(clock);
    return retryService.retry(
        applicationConfigurationProperties.getMaxHashRetries(),
        () -> saveToStorage(hash, longUrl, currentDateTime).flatMap(success -> Try.success(hash)));
  }

  private String extractHashFromUrl(String shortUrl) {
    return shortUrl.substring(shortUrl.lastIndexOf('/') + 1);
  }

  private Try<Optional<String>> checkHashAndFetchUrl(String hash) {
    return checkHashInRedis(hash)
        .flatMap(isHashInUse -> isHashInUse ? getUrl(hash) : Try.success(Optional.empty()));
  }

  private Try<Optional<String>> getUrl(String hash) {
    return retryService.retry(
        applicationConfigurationProperties.getMaxHashRetries(),
        () -> storageRepository.getOriginalUrl(hash));
  }

  private Try<Void> checkHashAndDeleteUrl(String hash) {
    return checkHashInRedis(hash)
        .flatMap(
            isHashInUse -> isHashInUse ? deleteUrlFromStorageAndRedis(hash) : Try.success(null));
  }

  private Try<Void> deleteUrlFromStorageAndRedis(String hash) {
    return retryService
        .retry(
            applicationConfigurationProperties.getMaxHashRetries(),
            () -> storageRepository.deleteUrl(hash))
        .flatMap(success -> deleteHashFromRedis(hash));
  }

  private Try<Void> deleteHashFromRedis(String hash) {
    return retryService.retry(
        applicationConfigurationProperties.getMaxHashRetries(),
        () -> redisRepository.deleteHash(hash));
  }

  private Try<Void> saveToStorage(String hash, String longUrl, LocalDateTime creationDateTime) {
    return storageRepository.save(
        hash,
        longUrl,
        creationDateTime,
        creationDateTime.plusDays(applicationConfigurationProperties.getUrlExpirationInDays()));
  }
}
