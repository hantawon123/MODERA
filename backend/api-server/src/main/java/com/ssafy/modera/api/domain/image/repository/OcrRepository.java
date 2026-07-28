package com.ssafy.modera.api.domain.image.repository;

import com.ssafy.modera.api.domain.image.entity.Ocr;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OcrRepository extends JpaRepository<Ocr, Integer> {

    Optional<Ocr> findByImageId(Integer imageId);
}
