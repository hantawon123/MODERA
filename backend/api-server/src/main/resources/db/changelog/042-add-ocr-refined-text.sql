--liquibase formatted sql

--changeset modera-api:042-add-ocr-refined-text
--comment Keep the AI-refined OCR text that ANALYSIS_COMPLETED carries. 원문(image_schema.ocr.content)과 달리 정제본은 AI 산출물이라 read model에 둔다.
ALTER TABLE query_schema.user_image_view
    ADD COLUMN ocr_refined_text TEXT;

--rollback ALTER TABLE query_schema.user_image_view DROP COLUMN ocr_refined_text;
