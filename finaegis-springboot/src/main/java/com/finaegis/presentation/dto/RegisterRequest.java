package com.finaegis.presentation.dto;

public record RegisterRequest(
    String name,
    String email,
    String password
) {}
