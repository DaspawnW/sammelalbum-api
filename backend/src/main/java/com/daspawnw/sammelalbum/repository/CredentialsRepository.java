package com.daspawnw.sammelalbum.repository;

import com.daspawnw.sammelalbum.model.Credentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CredentialsRepository extends JpaRepository<Credentials, Long> {
    Optional<Credentials> findByUsername(String username);
}
