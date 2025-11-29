package com.daspawnw.sammelalbum.repository;

import com.daspawnw.sammelalbum.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByMail(String mail);
}
