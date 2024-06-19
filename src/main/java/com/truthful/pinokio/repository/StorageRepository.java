package com.truthful.pinokio.repository;

import io.vavr.control.Try;
import java.time.LocalDateTime;
import java.util.Optional;

public interface StorageRepository {

  Try<Void> save(
      String hash, String originalUrl, LocalDateTime createdAt, LocalDateTime expiresAt);

  Try<Optional<String>> getOriginalUrl(String hash);

  Try<Void> deleteUrl(String hash);
}
