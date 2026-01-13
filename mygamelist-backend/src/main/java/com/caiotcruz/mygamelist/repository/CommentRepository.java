package com.caiotcruz.mygamelist.repository;

import com.caiotcruz.mygamelist.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}