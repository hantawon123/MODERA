package com.ssafy.modera.api.domain.document.repository;

import com.ssafy.modera.api.domain.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Integer> {
}
