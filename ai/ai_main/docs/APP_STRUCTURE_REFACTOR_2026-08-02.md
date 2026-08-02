# app/ 구조 정리 (2026-08-02)

> 동작 변경 0. 매 단계 `openapi.json` byte 동일 + 기존 테스트 통과로 검증.
> 새 기능·삭제·로직 수정 없음. 중복 제거와 파일 이동만.

`develop/ai` 의 `app/` 는 이미 구조가 좋다 — 순환 import 0(DAG), 클라이언트
격리(`gemini_client`/`storage`/`spring_client`/`embedder`), 라우터 분리 완료
([MAIN_ROUTER_SPLIT.md](MAIN_ROUTER_SPLIT.md)). 그래서 `routers/ services/`
같은 하위 폴더 재패키징은 **하지 않았다** — 21개 파일 + 깨끗한 DAG 에서는 import
경로만 깨지고 얻는 게 없다(YAGNI). 실제로 크기·응집이 어긋난 지점만 손봤다.

## 한 일

### 1. 중복 헬퍼 단일 출처화 (`3a003f0`)

- **`app/timeutil.py` 신설**(leaf, 아무것도 import 안 함). 명세 1.2 시간 형식
  `now_iso()` 를 한 곳에서만 정의.
  - 같은 4줄이 `deps`·`stages`·`document`·`responses`·`jobs` 에 5개 사본으로
    흩어져 있던 것을 제거. (`document.py` 에 이미 "5개 파일로 빼라"는 `ponytail:`
    주석이 달려 있었다 — 그 지시를 실행.)
- **`related.error_response` 삭제 → `deps._error` 로 통합.** 두 함수가 출력이
  동일한 내부 API 에러 빌더(`{"error", "message", "detail"}`)였다. `related`·
  `doc_selection`·`reanalyze` 가 `_error` 를 직접 쓴다.
  - `reanalyze` 의 `related` import 는 `error_response` 전용이었어서 함께 제거.
- 부수: 이동으로 미사용이 된 `datetime` import 정리(`deps`·`document`·`responses`).

### 2. 카테고리 벡터 저장소 분리 (`69dc083`)

`search.py`(1309줄)가 OpenSearch **키워드 검색**과 무관한 **카테고리 대표 벡터
저장소**를 함께 들고 있었다. 인덱스도 벡터 공간도 소비자도 다르다:

| | 본문 검색 | 카테고리 벡터 |
| --- | --- | --- |
| 인덱스 | `{index}` | `{index}_categories` |
| 벡터 | 로컬 bge-m3 1024차원 (kNN) | Gemini 768차원 (전량 스캔 코사인) |
| 쓰는 곳 | `/internal/v1/search`, 목록·집계 | `stages`·`reanalyze` 판정 경로 |

- **`app/category_store.py` 신설**: `ensure_category_index` /
  `load_category_vectors` / `put_seed_category_vectors` / `upsert_category_vector`
  / `_get_category_doc` / `_status_code` / `_category_index` + 상수
  (`SEED_USER_ID`, `_CATEGORY_LOAD_LIMIT`).
- 공유 원시함수(`_client`, `_create_index_with_template`, `stable_id`)는
  `search` 에 남기고 `category_store` 가 import 한다. **`search` 는
  `category_store` 를 되부르지 않아 순환이 없다**(단방향).
- 호출자(`stages`, `reanalyze`)를 `category_store.*` 로 갱신.
- `test_reanalyze` 의 mock 대상(`search.load_category_vectors` 등)을 이동 위치로 갱신.
- 결과: **`search.py` 1309 → 1063**, `category_store.py` 260.

## 안 한 일 — pipeline 분리 (보류)

원래 계획의 3단계는 `stages.py`(1115줄)에서 `_run_full_pipeline`(218줄) +
`execute_stage` + `run_app_analysis` 를 `pipeline.py` 로 빼는 것이었다.
**하지 않았다.** 이유:

1. **깨끗한 seam 이 아니다.** `_run_full_pipeline` 은 `stages` 내부 함수를 10개+
   호출하는 거미줄의 중심이다(`run_agent_core`·`build_candidates`·
   `_index_for_search`·`_index_as_other`·`_empty_callback_result`·
   `_text_from_image_analysis`·`_timed`·세마포어…). 파일만 빼면 `pipeline.py`
   가 `stages` 의 밑줄 붙은 private 를 대량 import 하게 된다 — 한 파일이 큰 것보다
   나쁜, 결합을 가로지르는 절단이다.
2. **세마포어 재진입 위험.** `asyncio.Semaphore` 는 재진입 불가라 진입점
   (`execute_stage`/`run_app_analysis`)에서만 잡고 `_run_full_pipeline` 본체는
   안 잡는 계약이다(README §구조 경고). 모듈 전역 `_stage_semaphore` 를 파일에
   걸쳐 나누면 이 계약이 조용히 깨질 수 있다.
3. **로컬 런타임 검증 불가.** 이 환경엔 Gemini·OpenSearch 가 없어 분석 파이프라인을
   실제로 돌려볼 수 없다. `category_store` 이동은 순수 이동 + 정적 검증(undefined
   name 0, import, openapi diff, 테스트)으로 안전을 담보했지만, pipeline 은
   결합이 깊어 정적 검증만으로는 "요청이 들어올 때만 터지는" 구멍
   (MAIN_ROUTER_SPLIT.md 가 겪은 지역 import 함정과 같은 종류)을 못 막는다.

`stages.py` 는 크지만 **응집**돼 있다(하나의 파이프라인). 줄 수만 맞추려 응집된
orchestrator 를 파일로 쪼개는 건 over-engineering 이다.

**하려면**: 실제 OpenSearch·Gemini(도커 `docker-compose.yml`) 대상 통합 테스트
— FULL 동시 요청 + 콜백 수신(`scripts/full_stage_test.py`, 세마포어 회귀 겸용)을
스냅샷으로 떠 두고, 다음을 함께 하는 편이 낫다:
- 세마포어 전역을 소유할 모듈을 하나로 확정
- `_index_for_search`/`_index_as_other` 의 IO(색인)와 orchestration(`job_store`
  상태 전이)을 분리 — 지금은 한 함수에 섞여 있어 그대로는 search 층으로 못 내린다

## 검증 (매 단계 반복)

```bash
cd ai/ai_main
PY=<venv python>

# ① openapi.json 이 분리 전과 같은가 — 주 검증
$PY -c "import json; from app.main import app; \
  print(json.dumps(app.openapi(), ensure_ascii=False, sort_keys=True, indent=1))" \
  > /tmp/openapi_after.json
diff /tmp/openapi_before.json /tmp/openapi_after.json     # 차이 0

# ② undefined name(이동으로 끊긴 참조) 0
$PY -m pyflakes app | grep -i undefined                   # 없음

# ③ import 스모크(순환·import 시점 에러)
$PY -c "from app.main import app; import app.category_store"

# ④ 기존 테스트
$PY -m pytest test -q                                     # 56 passed
```

두 단계 모두 ①~④ 통과. `pyflakes` 의 undefined-name 검사는 "이동했는데 따라오지
않은 참조"를 정확히 잡는다(이번 이동의 주 위험). 단, **실제 OpenSearch·Gemini
호출 경로의 런타임 동작은 로컬에서 확인하지 못했다** — 순수 이동이라 로직은
바뀌지 않았지만, 통합 환경에서 한 번 더 확인하는 것을 권한다.

## 커밋

| 커밋 | 내용 |
| --- | --- |
| `3a003f0` | `_now_iso`/`_error` 중복 제거, `timeutil.py` 신설 |
| `69dc083` | 카테고리 벡터 저장소를 `category_store.py` 로 분리 |
