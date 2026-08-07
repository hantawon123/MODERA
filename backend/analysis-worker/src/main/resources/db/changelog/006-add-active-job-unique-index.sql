--liquibase formatted sql

--changeset analysis-worker:500-add-active-job-unique-index
-- 같은 이미지에 활성 job이 동시에 두 개 생기는 경합을 DB에서 막는다.
--
-- 애플리케이션에도 같은 목적의 조회 가드(existsByImageIdAndStatusIn)가 있지만, 조회와
-- INSERT 사이에 다른 스레드가 끼어들면 둘 다 통과한다. 메인 컨슈머(image-analysis-consumer
-- 스레드)와 PelReclaimScanner(scheduling 스레드)가 같은 이미지를 동시에 처리할 수 있어
-- 실제로 가능한 경합이다. 인스턴스를 늘리면 확률은 더 올라간다.
--
-- 조건에 COMPLETED를 넣지 않는 이유: 넣으면 한 번 분석된 이미지는 영영 재분석할 수 없다.
-- 이 인덱스는 "동시에 활성 job 두 개"만 막고, "이미 분석된 이미지를 또 돌리지 않는다"는
-- 정책은 애플리케이션의 COMPLETED 체크가 담당한다 — 목적이 다른 별개 장치다.
-- FAILED도 제외하므로 실패한 이미지의 재시도는 그대로 열려 있다.
--
-- precondition: 활성 job이 중복된 이미지가 이미 있으면 인덱스 생성이 실패한다.
-- Postgres 오류보다 먼저 걸러 원인을 명확히 남긴다(중복을 정리한 뒤 다시 기동할 것).
--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:0 SELECT count(*) FROM (SELECT image_id FROM analysis_job WHERE status IN ('PENDING', 'PROCESSING') GROUP BY image_id HAVING count(*) > 1) duplicated
CREATE UNIQUE INDEX uq_analysis_job_active ON analysis_job (image_id)
    WHERE status IN ('PENDING', 'PROCESSING');
--rollback DROP INDEX uq_analysis_job_active;
