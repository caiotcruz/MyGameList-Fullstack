package com.caiotcruz.mygamelist.repository;

import com.caiotcruz.mygamelist.model.ReviewVote;
import com.caiotcruz.mygamelist.model.UserGameList;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.enums.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReviewVoteRepository extends JpaRepository<ReviewVote, Long> {
    Optional<ReviewVote> findByUserAndReview(User user, UserGameList review);
    
    long countByReviewAndType(UserGameList review, VoteType type);
}