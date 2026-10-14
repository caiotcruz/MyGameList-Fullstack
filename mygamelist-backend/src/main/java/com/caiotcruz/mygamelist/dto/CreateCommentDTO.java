package com.caiotcruz.mygamelist.dto;

public record CreateCommentDTO(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = TextLimits.COMMENT) String text) {}
