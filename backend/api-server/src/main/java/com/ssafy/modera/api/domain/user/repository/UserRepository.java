package com.ssafy.modera.api.domain.user.repository;

import com.ssafy.modera.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
