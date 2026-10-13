package com.caiotcruz.mygamelist.model;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
@Entity @Data @Table(name="tb_user_blocks",uniqueConstraints=@UniqueConstraint(columnNames={"blocker_id","blocked_id"}))
public class UserBlock {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne @JoinColumn(name="blocker_id",nullable=false) private User blocker;
 @ManyToOne @JoinColumn(name="blocked_id",nullable=false) private User blocked;
 @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
}
