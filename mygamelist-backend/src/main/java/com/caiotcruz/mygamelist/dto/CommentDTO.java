package com.caiotcruz.mygamelist.dto;

import java.time.LocalDateTime;

import com.caiotcruz.mygamelist.model.Comment;

public record CommentDTO(Long id, String text, LocalDateTime createdAt, ActivityUserDTO user) {
    public static CommentDTO from(Comment comment) {
        return new CommentDTO(
                comment.getId(), 
                comment.getText(), 
                comment.getCreatedAt(),
                ActivityUserDTO.from(comment.getUser())
        );
    }
}