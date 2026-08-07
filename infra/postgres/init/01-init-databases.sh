#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER api_admin WITH PASSWORD '${API_DB_PASSWORD}';
    CREATE USER analysis_admin WITH PASSWORD '${ANALYSIS_DB_PASSWORD}';

    CREATE DATABASE modera_api OWNER api_admin;
    CREATE DATABASE modera_analysis OWNER analysis_admin;

    REVOKE CONNECT ON DATABASE modera_api FROM PUBLIC;
    REVOKE CONNECT ON DATABASE modera_analysis FROM PUBLIC;
    GRANT CONNECT ON DATABASE modera_api TO api_admin;
    GRANT CONNECT ON DATABASE modera_analysis TO analysis_admin;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname modera_api <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS vector;
    CREATE EXTENSION IF NOT EXISTS pg_bigm;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname modera_analysis <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS vector;
EOSQL
