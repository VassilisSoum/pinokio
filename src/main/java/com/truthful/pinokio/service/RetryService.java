package com.truthful.pinokio.service;

import io.vavr.control.Try;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RetryService {

  public <T> Try<T> retry(int maxRetries, Supplier<Try<T>> supplier) {
    for (int i = 0; i < maxRetries; i++) {
      Try<T> result = supplier.get();
      if (result.isSuccess()) {
        return result;
      }
      log.warn("Failed to execute, retrying");
    }
    return Try.failure(new RuntimeException("Failed to execute after " + maxRetries + " retries"));
  }
}
