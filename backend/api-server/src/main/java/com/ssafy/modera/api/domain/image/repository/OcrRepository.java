package com.ssafy.modera.api.domain.image.repository;

import com.ssafy.modera.api.domain.image.entity.Ocr;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OcrRepository extends JpaRepository<Ocr, Integer> {

    Optional<Ocr> findByImageId(Integer imageId);

    /**
     * 문서 생성 재료용 일괄 조회. 이미지당 한 행(image_id UNIQUE)이라 중복은 없고,
     * 등록되지 않은 이미지는 결과에서 빠진다(OCR 없음으로 취급).
     */
    List<Ocr> findByImageIdIn(List<Integer> imageIds);
}
