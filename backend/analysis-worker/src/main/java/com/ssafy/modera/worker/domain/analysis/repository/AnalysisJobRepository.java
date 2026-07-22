package com.ssafy.modera.worker.domain.analysis.repository;

import com.ssafy.modera.worker.domain.analysis.entity.AnalysisJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, Long> {
}
