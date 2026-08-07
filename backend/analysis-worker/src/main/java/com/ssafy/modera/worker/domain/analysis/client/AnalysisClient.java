package com.ssafy.modera.worker.domain.analysis.client;

import com.ssafy.modera.contract.payload.ImageUploadedPayload;
import com.ssafy.modera.worker.domain.analysis.entity.AnalysisJob;

/**
 * AI 서버한테 분석 요청
 * AI 서버는 요청 접수 (202)
 * -> 결과는 콜백
 * -> 이 호출에는 리턴 없음
 * 결과 처리는 AnalysisCallback Service 담당
 */
public interface AnalysisClient {
    void requestAnalysis(AnalysisJob job, String s3Key, ImageUploadedPayload.ClientOcr clientOcr);
}
