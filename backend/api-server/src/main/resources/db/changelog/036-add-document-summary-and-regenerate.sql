--liquibase formatted sql

--changeset modera-api:036-add-document-summary
--comment Persist the AI document summary shown on the document list card and the detail summary block.
-- DOCUMENT_COMPLETED.summary는 여태 수신만 하고 버려졌다. 목록(8-1) 카드 설명과
-- 상세(8-3) "문서 요약"이 같은 값을 쓰므로 원본과 조회 모델 양쪽에 둔다.
-- NULL 허용: 이 changeset 이전에 만들어진 문서는 요약이 없고, AI 재호출 말고는
-- 채울 방법이 없어 빈 값을 강제하지 않는다.
ALTER TABLE document_schema.document
    ADD COLUMN summary TEXT;

ALTER TABLE query_schema.user_document_view
    ADD COLUMN summary TEXT;

--rollback ALTER TABLE query_schema.user_document_view DROP COLUMN summary;
--rollback ALTER TABLE document_schema.document DROP COLUMN summary;

--changeset modera-api:036-add-regenerate-operation
--comment Allow REGENERATE requests that update an existing document in place instead of creating a new one.
-- 재분석은 새 문서를 만들고 기존 문서를 지우는 게 아니라 같은 document_id를 갱신한다
-- (upsert). 그래서 CREATE와 달리 source_document_id가 반드시 있어야 하고, 완료되면
-- result_document_id도 같은 값이 된다.
ALTER TABLE document_schema.document_generation_request
    DROP CONSTRAINT ck_document_generation_request_operation;

ALTER TABLE document_schema.document_generation_request
    ADD CONSTRAINT ck_document_generation_request_operation
        CHECK (
            operation_type IN (
                'CREATE',
                'REGENERATE',
                'ADD_IMAGES',
                'EXCLUDE_IMAGES'
            )
        );

ALTER TABLE document_schema.document_generation_request
    DROP CONSTRAINT ck_document_generation_request_source;

ALTER TABLE document_schema.document_generation_request
    ADD CONSTRAINT ck_document_generation_request_source
        CHECK (
            (operation_type = 'CREATE' AND source_document_id IS NULL)
            OR
            (
                operation_type IN ('REGENERATE', 'ADD_IMAGES', 'EXCLUDE_IMAGES')
                AND source_document_id IS NOT NULL
            )
        );

--rollback ALTER TABLE document_schema.document_generation_request DROP CONSTRAINT ck_document_generation_request_source;
--rollback ALTER TABLE document_schema.document_generation_request ADD CONSTRAINT ck_document_generation_request_source CHECK ((operation_type = 'CREATE' AND source_document_id IS NULL) OR (operation_type IN ('ADD_IMAGES', 'EXCLUDE_IMAGES') AND source_document_id IS NOT NULL));
--rollback ALTER TABLE document_schema.document_generation_request DROP CONSTRAINT ck_document_generation_request_operation;
--rollback ALTER TABLE document_schema.document_generation_request ADD CONSTRAINT ck_document_generation_request_operation CHECK (operation_type IN ('CREATE', 'ADD_IMAGES', 'EXCLUDE_IMAGES'));

--changeset modera-api:036-single-inflight-regeneration
--comment One in-flight regeneration per document so concurrent requests cannot overwrite each other.
-- 같은 문서에 재분석이 두 번 걸리면 나중에 도착한 완료 이벤트가 앞선 결과를 덮어써
-- 사용자가 무엇을 보게 될지 예측할 수 없다. 접수 단계에서 조회로도 막지만(409),
-- 동시 요청은 조회만으로 못 막으므로 부분 유니크 인덱스를 최종 방어선으로 둔다.
CREATE UNIQUE INDEX uq_document_generation_request_inflight
    ON document_schema.document_generation_request (source_document_id)
    WHERE status = 'QUEUED' AND del_yn = 'N' AND source_document_id IS NOT NULL;

--rollback DROP INDEX document_schema.uq_document_generation_request_inflight;
