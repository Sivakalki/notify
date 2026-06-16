package com.notify.backend.repository;

import com.notify.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByExternalUserId(String externalUserId);

    boolean existsByExternalUserId(String externalUserId);
}