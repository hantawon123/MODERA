# 비용 경로 재검토 + OCR 중복 조사 (2026-08-05)

조사만 했고 **코드 변경 없음**. 다른 장비에서 이어받기 위한 인계 문서다.
선행 문서: `MODEL_COST_OPTIMIZATION_2026-08-03.md`(1차 최적화).

세 갈래다 — (A) 문서 생성 호출 비용, (B) 인프라 경로(GMS→Vertex), (C) OCR 중복.
C가 가장 값이 크고, A는 C를 고치면 상당 부분 같이 해결된다.

---

## A. 문서 생성 호출이 비쌌던 이유

### 계기
문서 생성 1회(이미지 11장 묶음)에 300크레딧. 이미지 분석 장당 67크레딧
(`config.py:153` 실측)과 자릿수가 다르다.

### 진단
| 요인 | 판정 |
|---|---|
| `DOCUMENT_MODEL_NAME=gemini-3.5-flash` ($1.50/$9.00) | **주범.** 단가 그대로다 |
| thinking 토큰 누수 | 아님 — `GEMINI_THINKING_LEVEL=minimal` 적용 확인 |
| 프롬프트 중복(OCR 이중 수록) | 2차 요인 → C 항목 |
| `maxOutputTokens` 미설정 | 미확인 리스크. 출력 $9/M 모델에 상한 없음 |

`thoughts` 값은 usage 로그(`gemini_client.py:133`)로 판정한다. 0/None이면 순수 단가.

### 모델 비교 (출력 지배 워크로드, 호출당 추정 — 입력 ~8k / 출력 ~2.5k 토큰)
| 모델 | 입출력 $/1M | 결제 경로 | 추정 |
|---|---|---|---|
| gemini-3.5-flash (당시) | 1.50 / 9.00 | GMS 크레딧 | ~$0.035 |
| gpt-5-mini | 0.25 / 2.00 | 자체 OpenAI 키 | ~$0.007 |
| gemini-2.5-flash | 0.30 / 2.50 | GMS 크레딧 | ~$0.009 |
| gemini-2.5-flash-lite | 0.10 / 0.40 | GMS 크레딧 | ~$0.002 |
| claude-haiku-4-5 | 1.00 / 5.00 | 신규 Anthropic 계정 | ~$0.021 |
| claude-sonnet-5 | 3.00 / 15.00 | 신규 Anthropic 계정 | ~$0.062 |

Claude 단가는 Anthropic 공식값. **Claude 탈락** — Haiku가 Claude 최저가인데
gpt-5-mini 대비 입력 4배·출력 2.5배. 세 번째 프로바이더 + 신규 결제 계정을
붙이면서 더 비싸게 내는 선택이라 볼 이유가 없다.

**gemini-2.5-flash-lite는 종이 위 최강이지만 이 작업엔 부적합.** 11소재 통합 +
규칙 8개 + 스키마 준수가 lite 최약점이고, `config.py:154-158`에 lite 누수
실측 기록이 있다. 시스템에서 가장 어려운 프롬프트를 최약 모델에 거는 도박.

### 검토했으나 적용 불가
- **프롬프트 캐싱**: 고정 접두사가 규칙 블록뿐(~700토큰 추정). 최소 캐시 길이
  미달(OpenAI 1024 / Gemini 1024+ / Haiku 4096). 마커 달아도 조용히 안 걸린다.
- **Batch API 50% 할인**: 완료까지 최대 24시간. 문서 생성은 사용자 대기 동기 경로.

### 별건 — 로컬 `.env`가 1차 최적화 이전 값
`.env:88` `LLM_MODEL_NAME=gemini-3.5-flash`
`.env.example:111` `LLM_MODEL_NAME=gemini-2.5-flash`

AGENT는 **장당** 호출되는 대량 주력이라 총액이 문서 경로보다 크다. 한 줄 수정이
문서 모델 교체보다 회수가 크다. **미적용 상태.**

---

## B. 인프라 경로 — GMS → Vertex AI (결정됨, 미구현)

### 결정
GCP 무료 체험 $300으로 Vertex AI를 쓴다. 단가 논쟁은 이걸로 종료.

### 확인해야 하는 전제 (미확인)
**"Vertex 신청 → 그 크레딧을 Google AI Studio에서 쓴다"는 성립하지 않는다.**
두 서비스는 엔드포인트·인증이 다르다.

| | Google AI Studio (Gemini API) | Vertex AI |
|---|---|---|
| 호스트 | `generativelanguage.googleapis.com` | `aiplatform.googleapis.com` |
| 인증 | API 키 (`x-goog-api-key`) | ADC / OAuth2 서비스 계정 |
| 경로 | `/v1beta/models/{m}:generateContent` | `/v1/projects/{p}/locations/{l}/publishers/google/models/{m}:generateContent` |
| SDK | `genai.Client(api_key=...)` | `genai.Client(vertexai=True, project=, location=)` |

현재 코드는 AI Studio 모양이다(`.env:10` GMS = `generativelanguage.googleapis.com` 프록시).

크레딧 적용 범위는 계정 상태와 Google 정책에 달렸다. **가입 전 확인 필요:**
1. GCP Console → 결제 → 체험 결제 계정 → **크레딧** 탭에서 적용 대상 서비스 확인
2. AI Studio에 그 결제 계정 연결 시도 (무료 체험 계정은 유료 티어 업그레이드가
   막히는 경우 있음)
3. 결정적 테스트: 1회 호출 후 결제 보고서에서 크레딧 상계 vs 카드 청구 확인

크레딧이 AI Studio에 안 붙으면 → Vertex로 코드 이전(아래) 또는 AI Studio 유료
티어를 카드로 결제(코드 무변경, 크레딧 못 씀).

### 구현 범위 (미착수)
`google-genai`가 이미 Vertex를 지원한다 — SDK 교체 없음. 2026-08-03 이전이
여기서 값을 낸다(`gemini_client.py:3-8`).

**클라이언트 싱글턴 한 곳** — `gemini_client.py:85-100`:
```python
_client_instance = genai.Client(
    vertexai=True,
    project=settings.gcp_project,
    location=settings.gcp_location,   # "global" 권장
    http_options=types.HttpOptions(timeout=int(settings.gemini_timeout * 1000)),
)
```
`api_key`·`base_url` 둘 다 빠진다. 인증은 `GOOGLE_APPLICATION_CREDENTIALS`
(서비스 계정 JSON 경로).

**임베딩은 GMS 유지 (결정)** — 768차원 pgvector 계약 리스크 회피. 따라서
**클라이언트 두 개**가 필요하다(인증 방식이 달라 하나로 못 묶는다):
- `_client()` → Vertex: `gemini_client.py:228` generate_content
- `_embed_client()` → GMS(api_key + base_url): `gemini_client.py:311` embed_content

15줄 안쪽. `GEMINI_API_KEY`·`GEMINI_BASE_URL`은 **지우지 말 것** — 임베딩
전용으로 살아남는다. `GCP_PROJECT`·`GCP_LOCATION`·`GOOGLE_APPLICATION_CREDENTIALS`가 추가된다.

### 얻는 것
- **GMS 화이트리스트 소멸.** `config.py:146-148`이 기록한 `gemini-3.5-flash-lite`
  400 "Model is not available" 벽이 사라진다. `MODEL_COST_OPTIMIZATION_2026-08-03.md`
  남은 일 #3("GMS에 3.x 저가 라인 추가 요청 필요")이 요청 없이 해결된다.
- 2.5 세대 은퇴(2026-10-16) 대비도 같이 종료.

### 지뢰
1. **임베딩 모델 ID.** GMS 유지하면 무해. 나중에 Vertex로 옮길 땐
   `gemini-embedding-2`가 Vertex에서 같은 이름인지 확인 필수 — 차원/벡터 공간이
   바뀌면 적재된 벡터와 섞여 검색이 조용히 망가진다(`config.py:172-176`).
2. **thinking 제어는 그대로 유효.** 크레딧이 넉넉해도 `GEMINI_THINKING_LEVEL`을
   빼면 3.5-flash가 medium으로 태운다. `_thinking_config`의
   `startswith("gemini-3")` 분기는 Vertex가 모델명을 그대로 쓰므로 유지된다.
3. **무료 체험 초기 쿼터.** Vertex 신규 프로젝트 QPM/TPM이 낮게 시작한다. 111장
   대량 분석이면 429. `_is_rate_limit`이 `google.genai.errors.ClientError.code`를
   보므로 Vertex 429도 잡히고 죽지는 않지만 느려진다 — Console에서 상향 선행.
4. **$300 만료.** GCP 무료 체험 90일. 2026-08-05 기준 대략 11월 초.
5. **GMS 배치 임베딩 한계 유지.** `gemini_client.py:329-331`에 기록된 대로 GMS는
   `batchEmbedContents`를 무시하고 첫 항목 1개만 반환한다. 단건 폴백이 정상
   동작 경로로 남는다(느리지만 맞다). 임베딩까지 Vertex로 옮기면 배치가 살아나고
   그 폴백은 죽은 코드가 된다.

---

## C. OCR 중복 — 가장 값이 큰 발견

### 증상
`rawText`(→ 프롬프트의 `OCR 원문`)에 **같은 한글이 뭉개진 형태 + 정상 형태로
두 번** 들어 있다. 11장 전부 동일 구조 — 뭉개진 짧은 블록이 앞, 정상 한글이 뒤.

이미지 #35 예:
| 뭉개진 쪽 | 정상 쪽 |
|---|---|
| `4 (Domain)` | `정보 영역(Domain )` |
| `A5 (ncome)` | `소득정보 (ncome)` |
| `x aeaKCB) Bx` | `부산광역시 코리아크레딧뷰로 (KCB) 공동 발제` |
| `o\|E\| 4}o\|E (KCB N )` | `제공 데이터 속성 가이드 (KCB 신용 정보)` |

### 원인 판정 — 상류다 (확실)
`ocr_text` 생성 경로 전부 확인. **이어붙이는 코드가 서비스에 없다.**

| 위치 | 동작 |
|---|---|
| `document.py:65` | `refined_text or raw_text` — 택일 |
| `stages.py:576` | 동일 택일 |
| `stages.py:729` | 동일 택일 |
| `_merge_refined` (`stages.py:560-570`) | 줄 단위 치환. 줄 수 보존, append 없음 |
| `stages.py:983` | 비전 OCR 폴백 — `ocr_text`가 **빈** 경우만 대체. 합치지 않음 |

그리고 뭉개진 텍스트(`x aeaKCB) Bx`)는 교정 LLM이 만들 수 없는 형태다 → OCR
엔진 출력이며 모든 LLM 단계보다 상류.

**계약 위반이기도 하다.** `schemas.py:65` 예시는 단일 패스 평문
(`"rawText": "오후 4:20 85% 교보문고 C++ 프로그래밍 입문 32,000원"`).
상류 팀에 올릴 때 "성능 개선"이 아니라 **"rawText 계약 위반"**으로 프레이밍할 것.

### 메커니즘 (유력, 미증명)
**같은 OCR 결과를 두 방식으로 직렬화**한 것으로 보인다. 인식기 두 개보다 흔하다.

#35 순회 순서:
- 뭉개진 블록: 표를 **열 단위**로 내려간다 (`4 (Domain)` → `(Demographics)` → `A5 (ncome)` → …)
- 정상 블록: 같은 표를 **행/영역 단위**로 읽는다

ML Kit `Text`는 `text`(전체) → `textBlocks` → `lines` 계층이다. 전체 문자열과
블록 순회 결과를 **둘 다** 이어붙이면 이 모양이 나온다.

### 왜 교정 단계가 못 고치는가 (구조적)
`stages.py:379` 프롬프트가 명시적으로 금지한다:
> 줄을 지우거나 요약하지 마라 — 번호 줄의 내용을 통째로 바꾸는 교정만 허용된다.

중복 제거는 줄 **삭제**가 필요한데 그게 금지된 유일한 연산이다. 근거 필드
무결성(rawText와 줄 대응)을 지키려는 설계이고 그 자체는 타당하다. 결과적으로
중복이 파이프라인 전체를 통과한다.

### 영향 범위 — 문서 경로보다 넓다
1. **융합 분석 프롬프트 (제일 큼).** `stages.py:408-409`가 OCR에 줄번호를 붙여
   넣는다. **장당** 호출이다. 문서 생성은 희소한데 이건 모든 이미지가 지나간다.
2. **3000자 절단이 품질 문제로 번진다.** `stages.py:341-346`. 뭉개진 블록이
   **앞에** 있어 예산을 먼저 먹는다. 텍스트 밀집 스크린샷이면 정상 한글이
   절단선 밖으로 밀려나 — 모델이 읽을 수 있는 유일한 정상 텍스트가 사라진다.
3. **문서 경로 1500자 절단은 더 빡빡.** `document.py:78` `OCR_CHARS = 1500`.
   #35 OCR이 대략 1400자로 경계선. 기존 `ponytail:` 주석
   (`document.py:37-38`)이 표시한 "뒷부분 날아감"과 겹치면 **날아가는 게 정상
   한글 쪽**이다.
4. **BM25 인덱스 오염 — 단서.** `search.py:93`이 `raw_text`를 nori로 색인한다.
   뭉개진 토큰(`aeaKCB`, `o|E|`, `7|E}AS`)이 그대로 들어간다. `config.py:138-140`
   실측("'시험 신청한 내역'에 BM25 9건 전부 오탐")의 기여 요인일 수 있다.
   **미증명** — 색인 문서 하나 열어 확인하면 판정된다.

### 오탐 정정 — 필터 설계에 중요
**#26/#27의 영문은 낭비가 아니다.** `Problem Definition / 문제정의`,
`Creativity / 창의성 및 차별성` — 심사기준표 슬라이드가 실제로 이중언어다.
화면에 둘 다 있으니 지우면 정보 손실.

구분 기준: **같은 한글이 뭉개진 형태 + 정상 형태로 두 번** 나오면 중복.
영문+한글 병기는 원본. 단순히 "비ASCII 비율 낮은 줄 버리기"로 필터를 만들면
이중언어 슬라이드의 영문 컬럼이 같이 죽는다.

### 중복과 별개로 남는 프롬프트 낭비 (문서 경로)
- **워터마크·푸터 11장 전부 중복** — `부산광역시_박미애`, `04/11`, 공동발제 문구
- **title/summary가 OCR 재진술** — `document.py:96-97`이 넣고
  `document.py:117-119`가 "그대로 쓰지 마라"고 금지한다. 같은 사실 3중 전달 +
  금지 규칙 추가로 양쪽 손해
- **`responseSchema` 없이 `responseMimeType`만** — 스키마를 박으면 프롬프트의
  JSON 예시 줄 + 규칙 2줄이 삭제되고 `parse_json_response`
  (`gemini_client.py:145`) 복구 경로도 문서 경로에선 무용
- **`maxOutputTokens` 미설정**

---

## 남은 일 (우선순위)

1. **[C] 상류/내부 확정 — 임시 로그 1줄.** 요청 진입점에서 `raw_text`와
   `refined_text`의 길이 + 앞 200자를 찍는다. `raw_text`에 이미 중복이면 상류
   확정, 안드로이드에 올릴 근거가 된다. 대안: OpenSearch에서
   `GET /{index}/_doc/{image_id}?_source=raw_text` — 빠르지만 `refined_text or
   raw_text`라 원본 구분이 안 된다.
2. **[C] 안드로이드/Spring 어느 쪽인지.** Spring이 `rawText`를 가공 없이
   전달하는지 확인해야 갈린다. 이 repo에서는 안 보인다.
   ⚠️ **안드로이드 이슈는 직접 수정하지 않는다** — 별도 보고 경로.
3. **[A] `.env` 한 줄** — `LLM_MODEL_NAME=gemini-2.5-flash`. 회수 제일 큼.
4. **[B] Vertex 크레딧 적용 범위 확인** (B 항목 3단계).
5. **[B] Vertex 전환** — 클라이언트 2개 분리 + config. 4번 통과 후.
6. **[C] AI 측 완화** (상류가 안 되면). 중복 제거를 **파이프라인 입구**에서.
   `document.py`가 아니다 — 분석·검색·문서가 전부 혜택을 본다. 휴리스틱이니
   `ponytail:` 주석으로 천장·상향 경로 명시.
7. **[A] 문서 경로 국소 수정** — title/summary 제거, 워터마크 필터,
   `maxOutputTokens`, `responseSchema`.

### 측정 원칙
1번 로그로 상류를 확정한 뒤, 중복 제거 전/후로 usage 로그
(`gemini_client.py:133`)의 `prompt=` 값을 비교한다. 추정치 대신 숫자로.
이 문서의 호출당 추정은 전부 추정이다.

---

## 다른 장비에서 이어받을 때

- **`.env`는 git 추적 안 됨.** `GEMINI_API_KEY`·`OPENAI_API_KEY` 등이 이 파일에만
  있다. 안전한 경로로 별도 전달하고 **커밋하지 말 것**. `.env.example`이 필요한
  키 목록이다.
- 코드 변경 없음 — 이 문서가 전부다.
- 선행 문서 `MODEL_COST_OPTIMIZATION_2026-08-03.md`를 같이 읽을 것. 단가표,
  thinking 제어 설계, 1차 모델 배치가 거기 있다.
