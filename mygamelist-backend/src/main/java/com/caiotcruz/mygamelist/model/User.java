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