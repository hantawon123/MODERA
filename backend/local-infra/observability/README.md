# 로컬 부하테스트 관측 스택

호스트에서 직접 띄운 api-server(관리 포트 9080)·analysis-worker(8082)의 actuator
Prometheus 메트릭을 로컬 Grafana로 보는 스택이다. **원격 서버의 `ops/grafana/`
스택과는 완전히 별개**이며 로컬 개발머신에서만 쓴다.

```bash
cd backend/local-infra/observability
docker compose up -d
```

| 주소 | 용도 | 계정 |
|---|---|---|
| http://localhost:3000 | Grafana — `Modera Local` 폴더의 `Modera 로컬 부하테스트` 대시보드 | 조회는 익명, 편집은 `admin` / `localdev` |
| http://localhost:9090 | Prometheus (타깃 상태: /targets) | - |

## 대시보드가 답하는 질문

- **커넥션 풀 대기가 언제 시작되나** — `pending(대기)` > 0
- **커넥션 획득 실패(= `CannotCreateTransactionException`)가 언제 나나** — 획득 실패/분
- **요청이 커넥션을 얼마나 오래 무나** — 점유시간(usage). 로그인 bcrypt-in-tx,
  similar의 worker HTTP-in-tx 검증용
- **Tomcat이 아니라 DB가 병목인가** — in-flight 요청 수는 여유인데 Hikari pending이
  치솟으면 확정

## 주의

- 앱 잡 스크레이프 간격은 2초다. 수 초짜리 로그인 버스트를 보려면 이 정도가 필요하다.
- `p95` 패널과 worker 패널은 **percentiles-histogram 설정이 포함된 8/3 이후 빌드로
  앱을 재시작해야** 데이터가 나온다(평균 지연 패널은 구버전에서도 동작).
- k6를 docker로 돌리는 원격 방식과 달리 로컬 k6는 호스트 바이너리라 이 스택과
  포트 충돌이 없다.
