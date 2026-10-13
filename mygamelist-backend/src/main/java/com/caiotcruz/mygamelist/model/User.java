package com.caiotcruz.mygamelist.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Data
@Entity
@org.hibernate.annotations.DynamicUpdate
@Table(name = "tb_users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password; 

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 500) 
    private String bio;
    
    @Column(columnDefinition = "TEXT") 
    private String profilePicture;

    @Column(nullable = false)
    private int level = 1;

    @Column(nullable = false)
    private int experience = 0;

    @Column(nullable = false)
    private int tokenVersion = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, columnDefinition="varchar(32) default 'PEOPLE_I_FOLLOW'")
    private com.caiotcruz.mygamelist.model.enums.MessagePolicy messagePolicy = com.caiotcruz.mygamelist.model.enums.MessagePolicy.PEOPLE_I_FOLLOW;

    private boolean rotatingAvatar = false;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() { return email; }

    @Override
    public String getPassword() { return password; }

    public boolean isRotatingAvatar() { return rotatingAvatar; }

    private boolean enabled = false;
    
    @JsonIgnore
    private String verificationCode;
    @JsonIgnore
    private String resetTokenHash;
    @JsonIgnore
    private LocalDateTime resetTokenExpiry;

    @JsonIgnore
    private LocalDateTime verificationExpiry;

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    
    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
}