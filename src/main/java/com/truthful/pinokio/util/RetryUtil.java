package com.truthful.pinokio.util;

import com.soumakis.control.Either;
import com.soumakis.control.Failure;
import com.soumakis.control.Success;
import com.soumakis.control.Try;
import com.truthful.pinokio.service.model.UrlShortenerError;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RetryUtil {

  private RetryUtil() {
  }

  public static <T> Try<T> retry(int maxRetries, Supplier<Try<T>> supplier) {
    for (int i = 0; i < maxRetries; i++) {
      Try<T> result = supplier.get();
      switch (result) {
        case Success<T> ignored:
          return result;
        case Failure<T> ignored:
          log.warn("Failed to execute, retrying");
      }
    }
    return Try.failure(new RuntimeException("Failed to execute after " + maxRetries + " retries"));
  }

  public static <T> Either<UrlShortenerError, T> retryEither(
      int maxRetries, Supplier<Either<UrlShortenerError, T>> supplier) {
    for (int i = 0; i < maxRetries; i++) {
      Either<UrlShortenerError, T> result = supplier.get();
      if (result.isRight()) {
        return result;
      }

      if (List.of(UrlShortenerError.HASH_ALREADY_EXISTS, UrlShortenerError.HASH_NOT_FOUND)
          .contains(result.getLeft())) {
        return result;
      }

      log.warn("Failed to execute, retrying");
    }
    return Either.left(UrlShortenerError.GENERIC_ERROR);
  }
}
