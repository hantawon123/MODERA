--liquibase formatted sql

--changeset analysis-worker:100-convert-image-id-to-integer
--comment Keep the analysis service contract aligned with the API service integer image identifier.
--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:0 SELECT count(*) FROM analysis_job
--precondition-sql-check expectedResult:0 SELECT count(*) FROM analysis_result

ALTER TABLE analysis_job
    ALTER COLUMN image_id TYPE INTEGER USING NULL::INTEGER;
ALTER TABLE analysis_result
    ALTER COLUMN image_id TYPE INTEGER USING NULL::INTEGER;
