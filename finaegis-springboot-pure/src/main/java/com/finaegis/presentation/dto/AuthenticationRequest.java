package com.finaegis.presentation.dto;

public record AuthenticationRequest(
    String email,
    String password
) {}
