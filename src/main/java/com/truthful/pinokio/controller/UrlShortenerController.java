package com.truthful.pinokio.controller;

import com.truthful.pinokio.controller.dto.ShortenedUrlRequestDto;
import com.truthful.pinokio.controller.dto.ShortenedUrlResponseDto;
import com.truthful.pinokio.service.UrlShortenerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/url")
@RequiredArgsConstructor
public class UrlShortenerController {

  private final UrlShortenerService urlShortenerService;

  public ResponseEntity<ShortenedUrlResponseDto> shortenUrl(
      @RequestBody ShortenedUrlRequestDto shortenedUrlRequestDto) {
    return urlShortenerService
        .shortenUrl(shortenedUrlRequestDto.longUrl())
        .map(shortUrl -> new ShortenedUrlResponseDto(shortUrl, shortenedUrlRequestDto.longUrl()))
        .map(ResponseEntity::ok)
        .getOrElseGet(throwable -> ResponseEntity.internalServerError().build());
  }

  @GetMapping("/{shortUrl}")
  public ResponseEntity<?> redirectToOriginalUrl(@PathVariable String shortUrl) {
    return urlShortenerService
        .getOriginalUrl(shortUrl)
        .map(
            originalUrl -> {
              if (originalUrl.isEmpty()) {
                return ResponseEntity.notFound().build();
              }
              HttpHeaders headers = new HttpHeaders();
              headers.add("Location", originalUrl.get());
              return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            })
        .getOrElseGet(throwable -> ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{shortUrl}")
  public ResponseEntity<Void> deleteUrl(@PathVariable String shortUrl) {
    return urlShortenerService
        .deleteUrl(shortUrl)
        .map(ResponseEntity::ok)
        .getOrElseGet(throwable -> ResponseEntity.internalServerError().build());
  }
}
