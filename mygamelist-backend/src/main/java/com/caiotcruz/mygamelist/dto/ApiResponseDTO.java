package com.caiotcruz.mygamelist.dto;

public record ApiResponseDTO<T>(
    String message,
    T data
) {}