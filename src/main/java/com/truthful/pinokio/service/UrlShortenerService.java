package com.truthful.pinokio.service;

import com.soumakis.control.EitherT;
import com.soumakis.control.TryT;
import com.truthful.pinokio.config.model.ApplicationConfigurationProperties;
import com.truthful.pinokio.repository.StorageRepositoryFacade;
import com.truthful.pinokio.service.model.UrlShortenerError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

  private final ApplicationConfigurationProperties applicationConfigurationProperties;
  private final StorageRepositoryFacade storageRepositoryFacade;

  /**
   * Shortens the given URL. If the URL is already shortened, it tries to generate a new hash up to
   * a configurable amount of times before giving up. If the URL is successfully shortened, it saves
   * the hash to Redis and the URL to the storage and then returns the shortened URL.
   *
   * @param longUrl the URL to shorten
   * @return the shortened URL
   */
  public EitherT<UrlShortenerError, String> shortenUrl(String longUrl) {
    return generateHashWithRetry(longUrl, 0)
        .map(generatedHash -> applicationConfigurationProperties.baseUrl() + "/" + generatedHash);
  }

  /**
   * Fetches the original URL for the given short URL. It first extracts the hash from the URL and
   * then checks if the hash is in use. If the hash is in use, it fetches the original URL from the
   * storage and returns it.
   *
   * @param shortUrl the short URL to fetch the original URL for
   * @return the original URL if it exists
   */
  public EitherT<UrlShortenerError, String> getOriginalUrl(String shortUrl) {
    String hash = extractHashFromUrl(shortUrl);
    return getUrl(hash);
  }

  /**
   * Deletes the URL for the given short URL. It first extracts the hash from the URL and then
   * checks if the hash is in use. If the hash is in use, it deletes the URL from the storage and
   * Redis.
   *
   * @param shortUrl the short URL to delete
   * @return success or error.
   */
  public TryT<Void> deleteUrl(String shortUrl) {
    String hash = extractHashFromUrl(shortUrl);
    return storageRepositoryFacade.delete(
        hash, applicationConfigurationProperties.maxHashRetries());
  }

  private EitherT<UrlShortenerError, String> generateHashWithRetry(
      String longUrl, int currentRetries) {
    if (currentRetries >= applicationConfigurationProperties.maxHashRetries()) {
      return EitherT.left(UrlShortenerError.GENERIC_ERROR);
    }
    String hash = generateHash(longUrl);
    return saveHashAndReturnShortUrl(hash, longUrl, currentRetries);
  }

  private EitherT<UrlShortenerError, String> saveHashAndReturnShortUrl(
      String hash, String longUrl, int currentRetries) {
    return storageRepositoryFacade
        .saveHash(
            hash,
            longUrl,
            applicationConfigurationProperties.maxHashRetries(),
            applicationConfigurationProperties.urlExpirationInDays())
        .recoverWith(error -> handleError(error, longUrl, currentRetries));
  }

  private EitherT<UrlShortenerError, String> handleError(
      UrlShortenerError error, String longUrl, int currentRetries) {
    if (UrlShortenerError.HASH_ALREADY_EXISTS.equals(error)
        && currentRetries < applicationConfigurationProperties.maxHashRetries()) {
      return generateHashWithRetry(longUrl, currentRetries + 1);
    } else {
      return EitherT.left(error);
    }
  }

  private String generateHash(String longUrl) {
    return applicationConfigurationProperties
        .algorithm()
        .generateHash(longUrl);
  }

  private String extractHashFromUrl(String shortUrl) {
    return shortUrl.substring(shortUrl.lastIndexOf('/') + 1);
  }

  private EitherT<UrlShortenerError, String> getUrl(String hash) {
    return storageRepositoryFacade.getOriginalUrl(
        hash, applicationConfigurationProperties.maxHashRetries());
  }
}
