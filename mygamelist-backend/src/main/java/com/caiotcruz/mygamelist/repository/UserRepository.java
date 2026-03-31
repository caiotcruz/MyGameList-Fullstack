package com.caiotcruz.mygamelist.repository;

import com.caiotcruz.mygamelist.model.User;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

public interface UserRepository extends JpaRepository<User, Long> {
    UserDetails findByEmail(String email);
    List<User> findByNameContainingIgnoreCase(String name);
}