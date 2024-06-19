package com.truthful.pinokio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vavr.control.Try;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RetryServiceTest {

  private final RetryService retryService = new RetryService();

  @Test
  @DisplayName("should return success when supplier succeeds on first attempt")
  void shouldReturnSuccessWhenSupplierSucceedsOnFirstAttempt() {
    Supplier<Try<String>> supplier = Mockito.mock(Supplier.class);
    Mockito.when(supplier.get()).thenReturn(Try.success("Success"));

    Try<String> result = retryService.retry(3, supplier);

    assertTrue(result.isSuccess());
    assertEquals("Success", result.get());
  }

  @Test
  @DisplayName("should return success when supplier succeeds after retries")
  void shouldReturnSuccessWhenSupplierSucceedsAfterRetries() {
    Supplier<Try<String>> supplier = Mockito.mock(Supplier.class);
    Mockito.when(supplier.get())
        .thenReturn(Try.failure(new RuntimeException("Failed")))
        .thenReturn(Try.failure(new RuntimeException("Failed")))
        .thenReturn(Try.success("Success"));

    Try<String> result = retryService.retry(3, supplier);

    assertTrue(result.isSuccess());
    assertEquals("Success", result.get());
  }

  @Test
  @DisplayName("should return failure when supplier fails after max retries")
  void shouldReturnFailureWhenSupplierFailsAfterMaxRetries() {
    Supplier<Try<String>> supplier = Mockito.mock(Supplier.class);
    Mockito.when(supplier.get())
        .thenReturn(Try.failure(new RuntimeException("Failed")))
        .thenReturn(Try.failure(new RuntimeException("Failed")))
        .thenReturn(Try.failure(new RuntimeException("Failed")));

    Try<String> result = retryService.retry(3, supplier);

    assertTrue(result.isFailure());
    assertEquals("Failed to execute after 3 retries", result.getCause().getMessage());
  }
}
