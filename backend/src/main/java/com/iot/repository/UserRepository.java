package com.iot.repository;

import com.iot.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findFirstByOrderByIdAsc();
}
