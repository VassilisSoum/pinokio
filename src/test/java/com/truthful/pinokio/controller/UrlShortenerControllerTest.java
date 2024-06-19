package com.truthful.pinokio.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.truthful.pinokio.controller.dto.ShortenedUrlRequestDto;
import com.truthful.pinokio.controller.dto.ShortenedUrlResponseDto;
import com.truthful.pinokio.service.UrlShortenerService;
import io.vavr.control.Try;
import java.util.Optional;
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

  @Mock private UrlShortenerService urlShortenerService;

  @InjectMocks private UrlShortenerController urlShortenerController;

  @Test
  @DisplayName("should return shortened url response when shorten url service is successful")
  void shouldReturnShortenedUrlResponseWhenShortenUrlServiceIsSuccessful() {
    ShortenedUrlRequestDto requestDto = new ShortenedUrlRequestDto("http://longurl.com");
    when(urlShortenerService.shortenUrl(requestDto.longUrl()))
        .thenReturn(Try.success("http://shorturl.com"));

    ResponseEntity<ShortenedUrlResponseDto> response =
        urlShortenerController.shortenUrl(requestDto);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("http://shorturl.com", response.getBody().shortUrl());
    assertEquals("http://longurl.com", response.getBody().originalUrl());
  }

  @Test
  @DisplayName("should return not found when original url is not found")
  void shouldReturnNotFoundWhenOriginalUrlIsNotFound() {
    when(urlShortenerService.getOriginalUrl("shortUrl")).thenReturn(Try.success(Optional.empty()));

    ResponseEntity<?> response = urlShortenerController.redirectToOriginalUrl("shortUrl");

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  @DisplayName("should return moved permanently when original url is found")
  void shouldReturnMovedPermanentlyWhenOriginalUrlIsFound() {
    when(urlShortenerService.getOriginalUrl("shortUrl"))
        .thenReturn(Try.success(Optional.of("http://longurl.com")));

    ResponseEntity<?> response = urlShortenerController.redirectToOriginalUrl("shortUrl");

    assertEquals(HttpStatus.MOVED_PERMANENTLY, response.getStatusCode());
    assertEquals("http://longurl.com", response.getHeaders().getLocation().toString());
  }

  @Test
  @DisplayName("should return ok when delete url service is successful")
  void shouldReturnOkWhenDeleteUrlServiceIsSuccessful() {
    when(urlShortenerService.deleteUrl("shortUrl")).thenReturn(Try.success(null));

    ResponseEntity<Void> response = urlShortenerController.deleteUrl("shortUrl");

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  @DisplayName("should return internal server error when shorten url service fails")
  void shouldReturnInternalServerErrorWhenShortenUrlServiceFails() {
    ShortenedUrlRequestDto requestDto = new ShortenedUrlRequestDto("http://longurl.com");
    when(urlShortenerService.shortenUrl(requestDto.longUrl()))
        .thenReturn(Try.failure(new RuntimeException("Failed to shorten URL")));

    ResponseEntity<ShortenedUrlResponseDto> response =
        urlShortenerController.shortenUrl(requestDto);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  @Test
  @DisplayName("should return internal server error when get original url service fails")
  void shouldReturnInternalServerErrorWhenGetOriginalUrlServiceFails() {
    when(urlShortenerService.getOriginalUrl("shortUrl"))
        .thenReturn(Try.failure(new RuntimeException("Failed to get original URL")));

    ResponseEntity<?> response = urlShortenerController.redirectToOriginalUrl("shortUrl");

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  @DisplayName("should return internal server error when delete url service fails")
  void shouldReturnInternalServerErrorWhenDeleteUrlServiceFails() {
    when(urlShortenerService.deleteUrl("shortUrl"))
        .thenReturn(Try.failure(new RuntimeException("Failed to delete URL")));

    ResponseEntity<Void> response = urlShortenerController.deleteUrl("shortUrl");

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }
}
