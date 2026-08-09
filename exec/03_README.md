# DB 덤프 안내

이 폴더(exec/db/)에는 제출용 DB 덤프 최신본을 둔다. 파일명 규칙: db_dump_YYYYMMDD.sql.gz

## 덤프 생성

DB가 3개(modera_api, modera_analysis, modera)이므로 클러스터 전체를 뜨는 pg_dumpall을 사용한다. roles와 3개 DB가 전부 포함된다.

```bash
docker exec infra-postgres-1 sh -c 'pg_dumpall -U "$POSTGRES_USER"' | gzip > db_dump_$(date +%Y%m%d).sql.gz
```

개별 DB가 필요하면 custom format으로 선택 복원이 가능하다.

```bash
docker exec infra-postgres-1 pg_dump -U admin -d modera_api -F c -f /tmp/modera_api.dump
docker exec infra-postgres-1 pg_dump -U admin -d modera_analysis -F c -f /tmp/modera_analysis.dump
docker cp infra-postgres-1:/tmp/modera_api.dump ./
docker cp infra-postgres-1:/tmp/modera_analysis.dump ./
```

## 검증

```bash
# 헤더 확인 (PostgreSQL database cluster dump 문구)
zcat db_dump_*.sql.gz | head -20

# 테이블 수 확인 (35개 내외)
zcat db_dump_*.sql.gz | grep -c "CREATE TABLE"

# 확장 포함 확인: vector, pg_bigm 이 보여야 함
zcat db_dump_*.sql.gz | grep -i "create extension"
```

## 복원 (신규 환경)

커스텀 이미지(pgvector+pg_bigm 포함, 레포 infra/postgres/Dockerfile)로 기동한 PostgreSQL이어야 확장 생성이 성공한다.

```bash
# dumpall 방식
zcat db_dump_YYYYMMDD.sql.gz | docker exec -i <postgres컨테이너> psql -U admin -d postgres

# custom 방식
docker exec -i <postgres컨테이너> pg_restore -U admin -d modera_api --clean --if-exists < modera_api.dump
```

복원되는 DB 계정 비밀번호는 덤프 시점 서버 값(SCRAM 해시) 기준이다. 신규 환경에서 비밀번호를 바꾸려면 복원 후 ALTER ROLE로 재설정하고 .env 값을 맞춘다.
