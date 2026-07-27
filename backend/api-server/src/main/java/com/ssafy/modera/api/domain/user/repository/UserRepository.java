package com.ssafy.modera.api.domain.user.repository;

import com.ssafy.modera.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}
