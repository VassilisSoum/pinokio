package com.truthful.pinokio.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.soumakis.control.EitherT;
import com.soumakis.control.TryT;
import com.truthful.pinokio.controller.dto.ShortenedUrlRequestDto;
import com.truthful.pinokio.controller.dto.ShortenedUrlResponseDto;
import com.truthful.pinokio.service.model.UrlShortenerError;
import com.truthful.pinokio.service.UrlShortenerService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class UrlShortenerControllerTest {

  @Mock
  private UrlShortenerService urlShortenerService;

  @InjectMocks
  private UrlShortenerController urlShortenerController;

  @Test
  @DisplayName("should return shortened url response when shorten url service is successful")
  void shouldReturnShortenedUrlResponseWhenShortenUrlServiceIsSuccessful()
      throws ExecutionException, InterruptedException {
    ShortenedUrlRequestDto requestDto = new ShortenedUrlRequestDto("http://longurl.com");
    when(urlShortenerService.shortenUrl(requestDto.longUrl()))
        .thenReturn(EitherT.right("http://shorturl.com"));

    CompletableFuture<ResponseEntity<?>> response =
        urlShortenerController.shortenUrl(requestDto);

    assertEquals(HttpStatus.OK, response.get().getStatusCode());
    assertEquals("http://shorturl.com",
        ((ShortenedUrlResponseDto) response.get().getBody()).shortUrl());
    assertEquals("http://longurl.com",
        ((ShortenedUrlResponseDto) response.get().getBody()).originalUrl());
  }

  @Test
  @DisplayName("should return not found when original url is not found")
  void shouldReturnNotFoundWhenOriginalUrlIsNotFound()
      throws ExecutionException, InterruptedException {
    when(urlShortenerService.getOriginalUrl("shortUrl")).thenReturn(
        EitherT.left(UrlShortenerError.HASH_NOT_FOUND));

    CompletableFuture<ResponseEntity<?>> response =
        urlShortenerController.redirectToOriginalUrl("shortUrl");

    assertEquals(HttpStatus.NOT_FOUND, response.get().getStatusCode());
  }

  @Test
  @DisplayName("should return moved permanently when original url is found")
  void shouldReturnMovedPermanentlyWhenOriginalUrlIsFound()
      throws ExecutionException, InterruptedException {
    when(urlShortenerService.getOriginalUrl("shortUrl"))
        .thenReturn(EitherT.right("http://longurl.com"));

    CompletableFuture<ResponseEntity<?>> response =
        urlShortenerController.redirectToOriginalUrl("shortUrl");

    assertEquals(HttpStatus.MOVED_PERMANENTLY, response.get().getStatusCode());
    assertEquals("http://longurl.com", response.get().getHeaders().getLocation().toString());
  }

  @Test
  @DisplayName("should return ok when delete url service is successful")
  void shouldReturnOkWhenDeleteUrlServiceIsSuccessful()
      throws ExecutionException, InterruptedException {
    when(urlShortenerService.deleteUrl("shortUrl")).thenReturn(TryT.of(null));

    CompletableFuture<ResponseEntity<Void>> response = urlShortenerController.deleteUrl("shortUrl");

    assertEquals(HttpStatus.OK, response.get().getStatusCode());
  }

  @Test
  @DisplayName("should return internal server error when shorten url service fails")
  void shouldReturnInternalServerErrorWhenShortenUrlServiceFails()
      throws ExecutionException, InterruptedException {
    ShortenedUrlRequestDto requestDto = new ShortenedUrlRequestDto("http://longurl.com");
    when(urlShortenerService.shortenUrl(requestDto.longUrl()))
        .thenReturn(EitherT.left(UrlShortenerError.GENERIC_ERROR));

    CompletableFuture<ResponseEntity<?>> response =
        urlShortenerController.shortenUrl(requestDto);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.get().getStatusCode());
  }

  @Test
  @DisplayName("should return internal server error when get original url service fails")
  void shouldReturnInternalServerErrorWhenGetOriginalUrlServiceFails()
      throws ExecutionException, InterruptedException {
    when(urlShortenerService.getOriginalUrl("shortUrl"))
        .thenReturn(EitherT.left(UrlShortenerError.GENERIC_ERROR));

    CompletableFuture<ResponseEntity<?>> response =
        urlShortenerController.redirectToOriginalUrl("shortUrl");

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.get().getStatusCode());
  }

  @Test
  @DisplayName("should return internal server error when delete url service fails")
  void shouldReturnInternalServerErrorWhenDeleteUrlServiceFails()
      throws ExecutionException, InterruptedException {
    when(urlShortenerService.deleteUrl("shortUrl"))
        .thenReturn(TryT.ofFailure(new RuntimeException("Failed to delete URL")));

    CompletableFuture<ResponseEntity<Void>> response = urlShortenerController.deleteUrl("shortUrl");

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.get().getStatusCode());
  }
}
