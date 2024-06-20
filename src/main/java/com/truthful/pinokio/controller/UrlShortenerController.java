package com.truthful.pinokio.controller;

import com.truthful.pinokio.controller.dto.ErrorDto;
import com.truthful.pinokio.controller.dto.ErrorType;
import com.truthful.pinokio.controller.dto.ShortenedUrlRequestDto;
import com.truthful.pinokio.controller.dto.ShortenedUrlResponseDto;
import com.truthful.pinokio.service.UrlShortenerService;
import com.truthful.pinokio.service.model.UrlShortenerError;
import jakarta.validation.Valid;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/url")
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerController {

  private final UrlShortenerService urlShortenerService;

  @PostMapping
  @Async
  public CompletableFuture<ResponseEntity<?>> shortenUrl(
      @RequestBody @Valid ShortenedUrlRequestDto shortenedUrlRequestDto) {
    return urlShortenerService
        .shortenUrl(shortenedUrlRequestDto.longUrl())
        .fold(
            urlShortenerError -> handleShortenUrlFailure(),
            shortUrl -> new ResponseEntity<>(new ShortenedUrlResponseDto(shortUrl,
                shortenedUrlRequestDto.longUrl()), HttpStatus.OK))
        .toCompletableFuture()
        .exceptionally(ex -> {
          log.error("Failed to shorten URL", ex);
          return new ResponseEntity<>(
              new ErrorDto("Internal Server Error", ErrorType.GENERIC_ERROR),
              HttpStatus.INTERNAL_SERVER_ERROR);
        });
  }

  @GetMapping("/{shortUrl}")
  @Async
  public CompletableFuture<ResponseEntity<?>> redirectToOriginalUrl(
      @PathVariable String shortUrl) {
    return urlShortenerService.getOriginalUrl(shortUrl)
        .fold(UrlShortenerController::handleRedirectOriginalUrlError,
            UrlShortenerController::originalUrlSuccessfulResponse)
        .exceptionally(ex -> {
          log.error("Failed to redirect to original URL", ex);
          return ResponseEntity.internalServerError()
              .body(new ErrorDto("Internal Server Error", ErrorType.GENERIC_ERROR));
        });
  }

  @DeleteMapping("/{shortUrl}")
  @Async
  public CompletableFuture<ResponseEntity<Void>> deleteUrl(@PathVariable String shortUrl) {
    return urlShortenerService
        .deleteUrl(shortUrl)
        .map(ResponseEntity::ok)
        .toCompletableFuture()
        .thenApply(
            responseEntityTry -> responseEntityTry.getOrElse(
                () -> ResponseEntity.internalServerError().build()));
  }

  private static ResponseEntity<?> originalUrlSuccessfulResponse(String originalUrl) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Location", originalUrl);
    return new ResponseEntity<>(headers,
        HttpStatus.MOVED_PERMANENTLY);
  }

  private static ResponseEntity<? extends Record> handleRedirectOriginalUrlError(
      UrlShortenerError urlShortenerError) {
    ResponseEntity<? extends Record> responseEntity;
    if (urlShortenerError == UrlShortenerError.HASH_NOT_FOUND) {
      responseEntity = ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new ErrorDto("Hash not found", ErrorType.HASH_NOT_FOUND));
    } else {
      responseEntity = ResponseEntity.internalServerError().build();
    }
    return responseEntity;
  }

  private static ResponseEntity<?> handleShortenUrlFailure() {
    return new ResponseEntity<>(new ErrorDto("Failed to shorten URL",
        ErrorType.GENERIC_ERROR), HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
