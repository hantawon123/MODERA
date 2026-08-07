package com.ssafy.modera.api.domain.library.repository;

import com.ssafy.modera.api.domain.library.entity.ImageDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageDocumentRepository extends JpaRepository<ImageDocument, Integer> {
}
