FROM pgvector/pgvector:pg16

# pg_bigm을 소스에서 빌드해 설치한다. 실패 시 이 파일을 pgvector 베이스만 남기고
# api-db/init/01-init.sql의 pg_bigm CREATE EXTENSION 줄을 주석 처리한 뒤 진행한다.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        build-essential \
        postgresql-server-dev-16 \
        git \
        ca-certificates \
    && git clone --depth 1 https://github.com/pgbigm/pg_bigm.git /tmp/pg_bigm \
    && cd /tmp/pg_bigm \
    && make USE_PGXS=1 \
    && make USE_PGXS=1 install \
    && cd / \
    && rm -rf /tmp/pg_bigm \
    && apt-get purge -y --auto-remove build-essential postgresql-server-dev-16 git \
    && rm -rf /var/lib/apt/lists/*
