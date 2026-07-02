package com.caiotcruz.mygamelist.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tb_likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "activity_id"}) 
})
public class ActivityLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "activity_id")
    private Activity activity;

    public ActivityLike() {}

    public ActivityLike(User user, Activity activity) {
        this.user = user;
        this.activity = activity;
    }

}