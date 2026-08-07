--liquibase formatted sql

--changeset analysis-worker:300-add-client-ocr-to-job
-- 클라이언트가 업로드 시점에 수행한 온디바이스 OCR 결과.
--
-- IMAGE_UPLOADED 이벤트(ImageUploadedPayload.clientOcr)로 들어오지만, 이 값을 쓰는
-- analysis_result 저장은 한참 뒤 별도 HTTP 콜백에서 일어난다. AI 콜백은 refined 텍스트만
-- 돌려주고 raw/lang/confidence는 echo하지 않으므로, 그 사이를 이 컬럼이 잇는다.
--
-- 메모리·Redis 대신 job 행에 두는 이유: worker 재시작·재배포·다중 인스턴스에서도
-- 유실 창이 없다. 이 값은 "분석 작업의 입력"이라 job 레코드의 일부로 보는 것이 자연스럽다.
ALTER TABLE analysis_job ADD COLUMN client_ocr_raw_text TEXT;
ALTER TABLE analysis_job ADD COLUMN client_ocr_lang VARCHAR(10);
ALTER TABLE analysis_job ADD COLUMN client_ocr_confidence REAL;
--rollback ALTER TABLE analysis_job DROP COLUMN client_ocr_raw_text; ALTER TABLE analysis_job DROP COLUMN client_ocr_lang; ALTER TABLE analysis_job DROP COLUMN client_ocr_confidence;
