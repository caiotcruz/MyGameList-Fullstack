package com.caiotcruz.mygamelist.model;

import com.caiotcruz.mygamelist.model.enums.VoteType;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_review_votes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "review_id"}) // Um voto por pessoa por review
})
public class ReviewVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "review_id") // A review é representada pelo UserGameList
    private UserGameList review;

    @Enumerated(EnumType.STRING)
    private VoteType type; // LIKE ou DISLIKE

    // Construtores, Getters e Setters
    public ReviewVote() {}

    public ReviewVote(User user, UserGameList review, VoteType type) {
        this.user = user;
        this.review = review;
        this.type = type;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public UserGameList getReview() { return review; }
    public void setReview(UserGameList review) { this.review = review; }
    public VoteType getType() { return type; }
    public void setType(VoteType type) { this.type = type; }
}