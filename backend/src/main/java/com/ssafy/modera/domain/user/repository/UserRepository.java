package com.ssafy.modera.domain.user.repository;

import com.ssafy.modera.domain.user.entity.Provider;
import com.ssafy.modera.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByLoginIdAndProvider(String loginId, Provider provider);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);
}
