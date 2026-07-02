package com.caiotcruz.mygamelist.model;

import com.caiotcruz.mygamelist.model.enums.VoteType;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tb_review_votes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "review_id"})
})
public class ReviewVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "review_id") 
    private UserGameList review;

    @Enumerated(EnumType.STRING)
    private VoteType type; 

    public ReviewVote() {}

    public ReviewVote(User user, UserGameList review, VoteType type) {
        this.user = user;
        this.review = review;
        this.type = type;
    }

}