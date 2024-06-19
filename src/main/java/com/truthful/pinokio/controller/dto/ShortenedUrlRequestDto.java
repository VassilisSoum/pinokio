package com.truthful.pinokio.controller.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record ShortenedUrlRequestDto(@NotEmpty @Size(min = 9, max = 1000) String longUrl) {}
