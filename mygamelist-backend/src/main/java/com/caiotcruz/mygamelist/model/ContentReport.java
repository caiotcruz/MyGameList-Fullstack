package com.caiotcruz.mygamelist.model;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
@Entity @Data @Table(name="tb_content_reports",uniqueConstraints=@UniqueConstraint(columnNames={"reporter_id","target_type","target_id"}))
public class ContentReport {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne @JoinColumn(name="reporter_id",nullable=false) private User reporter;
 @ManyToOne @JoinColumn(name="reported_user_id",nullable=false) private User reportedUser;
 @Column(nullable=false,length=20) private String targetType;
 @Column(nullable=false) private Long targetId;
 @Column(nullable=false,length=32) private String reason;
 @Column(nullable=false,length=2000) private String details;
 @Column(nullable=false,length=2000) private String snapshot;
 @Column(nullable=false,length=20) private String status="OPEN";
 @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
 private LocalDateTime reviewedAt;
 private Long reviewerId;
 @Column(length=2000) private String moderatorNote;
}
