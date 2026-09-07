package com.finaegis.presentation.dto;

public record AuthenticationResponse(
    String accessToken,
    String refreshToken,
    String userId
) {}
