package com.ssafy.modera.api.domain.library.repository;

import com.ssafy.modera.api.domain.library.entity.UserDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDocumentRepository extends JpaRepository<UserDocument, Integer> {
}
