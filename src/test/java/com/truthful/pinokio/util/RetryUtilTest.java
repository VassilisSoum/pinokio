package com.truthful.pinokio.util;

import com.soumakis.control.Failure;
import com.soumakis.control.Try;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.function.Supplier;

public class RetryUtilTest {

  @Test
  public void retrySuccessfullyOnFirstAttempt() {
    Supplier<Try<String>> supplier = () -> Try.success("Success");
    Try<String> result = RetryUtil.retry(3, supplier);
    Assertions.assertTrue(result.isSuccess());
    Assertions.assertEquals("Success", result.get());
  }

  @Test
  public void retrySuccessfullyOnSubsequentAttempt() {
    Supplier<Try<String>> supplier = new Supplier<>() {
      private int count = 0;

      @Override
      public Try<String> get() {
        if (count < 2) {
          count++;
          return Try.failure(new RuntimeException("Failure"));
        } else {
          return Try.success("Success");
        }
      }
    };
    Try<String> result = RetryUtil.retry(3, supplier);
    Assertions.assertTrue(result.isSuccess());
    Assertions.assertEquals("Success", result.get());
  }

  @Test
  public void retryExceedsMaxRetries() {
    Supplier<Try<String>> supplier = () -> Try.failure(new RuntimeException("Failure"));
    Try<String> result = RetryUtil.retry(3, supplier);
    Assertions.assertTrue(result.isFailure());
    Assertions.assertEquals("Failed to execute after 3 retries",
        ((Failure<String>) result).getCause().getMessage());
  }

  @Test
  public void retryWithZeroMaxRetries() {
    Supplier<Try<String>> supplier = () -> Try.failure(new RuntimeException("Failure"));
    Try<String> result = RetryUtil.retry(0, supplier);
    Assertions.assertTrue(result.isFailure());
    Assertions.assertEquals("Failed to execute after 0 retries",
        ((Failure<String>) result).getCause().getMessage());
  }
}