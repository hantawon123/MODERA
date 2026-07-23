package com.ssafy.modera.contract.payload;

/**
 * {@link com.ssafy.modera.contract.EventTypes#IMAGE_UPLOADED} 이벤트 payload.
 */
public record ImageUploadedPayload(
        int imageId,
        int userId,
        String s3Key,
        ClientOcr clientOcr
) {

    /**
     * 클라이언트가 업로드 시점에 미리 수행한 OCR 결과. 없으면 null.
     */
    public record ClientOcr(
            String rawText,
            String lang,
            Double confidence
    ) {
    }
}
