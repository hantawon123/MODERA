# OpenSearch + nori 구조 (인프라 참고용)

AI 서비스의 검색은 OpenSearch 가 전담한다. 기본 이미지가 아니라 **커스텀 이미지**를
쓰는데, 그 이유와 건드리면 안 되는 지점을 정리한다.

## 1. 왜 커스텀 이미지인가

`opensearch.Dockerfile` 은 두 줄이다.

```dockerfile
FROM opensearchproject/opensearch:2.17.0
RUN /usr/share/opensearch/bin/opensearch-plugin install --batch analysis-nori
```

**nori 는 한글 형태소 분석기**다. 공식 이미지에는 들어 있지 않아 플러그인으로 설치해야 한다.

없으면 어떻게 되나: OpenSearch 는 한글을 형태소로 못 쪼개고 공백/글자 단위로만 처리한다.
"강남역 맛집" 을 검색해도 "강남역 맛집 추천" 문서가 안 걸리는 식이다. 스크린샷 OCR 텍스트가
거의 다 한글이라 **검색 기능 자체가 무의미해진다.**

## 2. nori 가 코드의 어디에 엮여 있나

`app/search.py` 가 인덱스를 만들 때 `korean` 이라는 analyzer 를 정의하고,
그 tokenizer 로 `nori_tokenizer` 를 지정한다.

```
title, summary, tags, raw_text   ← 이 4개 필드가 korean analyzer 사용
category_name, s3_key            ← keyword (분석 안 함)
```

**중요**: nori 가 없는 OpenSearch 로 바꾸면 인덱스 생성 요청이 실패한다
(`unknown tokenizer [nori_tokenizer]`). 색인·검색이 전부 죽는다.
즉 **OpenSearch 이미지는 자유롭게 교체할 수 없다.** nori 는 선택이 아니라 의존성이다.

## 3. compose 에서 `image:` 와 `build:` 가 둘 다 있는 이유

```yaml
opensearch:
  image: ai-opensearch-nori:latest   # 있으면 이걸 그대로 쓴다
  build:                             # 없으면 여기서 빌드해서 위 태그로 붙인다
    context: .
    dockerfile: opensearch.Dockerfile
```

`docker compose up` 은 해당 태그의 이미지가 로컬에 **있으면 그대로 쓰고, 없으면 빌드**한다.
서버에는 `docker load` 로 올려둔 이미지가 있으니 빌드가 생략되어 기동이 빠르다.

주의할 점 두 가지:

- 이미지가 없는 새 서버에서는 **조용히 빌드가 돌아간다.** 플러그인을 인터넷에서 받으므로
  수 분이 걸리고, 네트워크가 막혀 있으면 실패한다. 새 환경을 만들 땐 이미지를 먼저 올릴 것.
- `docker compose up` 은 이미지를 **갱신하지 않는다.** Dockerfile 이 바뀌어도
  `docker compose build opensearch` 를 명시적으로 하지 않으면 옛 이미지가 계속 쓰인다.

## 4. Jenkins 와의 관계

`infra/Jenkinsfile` 의 AI 배포 단계는 **`ai-service` 컨테이너만** 재생성한다.

```
docker compose up -d --force-recreate ai-service
```

**OpenSearch 는 배포 때 건드리지 않는다. 이건 의도된 설계다.**
AI 코드를 아무리 배포해도 검색 인덱스 데이터가 유지된다.

OpenSearch 이미지나 설정을 바꿔야 할 때만 수동으로 재기동한다.

## 5. ⚠️ 데이터가 날아가는 경우

```yaml
volumes:
  - opensearch-data:/usr/share/opensearch/data
```

이 named volume 이 **분석 결과의 유일한 영속 저장소**다.
AI 서버의 작업 진행률은 메모리에 있고, Spring DB 는 아직 연동 전이라
**여기 말고는 데이터가 남는 곳이 없다.**

절대 하면 안 되는 것:

```bash
docker compose down -v      # ← -v 가 볼륨을 지운다. 분석 결과 전부 소멸
docker volume rm ...        # ← 마찬가지
```

컨테이너만 재기동하려면 `-v` 없이:

```bash
docker compose down && docker compose up -d
docker compose restart opensearch     # 또는 이쪽
```

지워졌을 때 복구 방법은 **이미지를 처음부터 다시 분석하는 것뿐**이다
(Gemini 호출 비용이 다시 든다).

## 6. 자주 걸리는 함정

### `OPENSEARCH_INITIAL_ADMIN_PASSWORD` 는 최초 1회만 적용된다

이름 그대로 **INITIAL** 이다. 볼륨이 이미 초기화된 뒤에 이 값을 바꿔도 반영되지 않고,
`OPENSEARCH_PASSWORD` 만 바꾸면 인증 실패로 AI 서비스가 검색을 못 한다.

- 두 값은 **항상 같아야 한다**
- 비밀번호를 정말 바꾸려면 볼륨을 지우고 처음부터(=데이터 소멸) 하거나
  OpenSearch 의 securityadmin 도구를 써야 한다

### 9200 포트를 외부에 열지 말 것

```yaml
ports:
  - "127.0.0.1:9200:9200"    # ← 127.0.0.1 을 빼면 인터넷에 그대로 노출된다
```

같은 호스트의 FastAPI 만 접근하면 된다. `0.0.0.0` 으로 바꾸거나 보안그룹을 열면
검색 데이터 전체가 외부에서 조회·삭제 가능해진다.

### 버전을 `latest` 로 바꾸지 말 것

`2.17.0` 으로 고정돼 있다. 메이저 업그레이드는 매핑·보안 설정 호환성을 깨뜨릴 수 있고,
nori 플러그인 버전도 OpenSearch 버전과 정확히 맞아야 설치된다.

### 기동에 30~45초 걸린다

그 사이 들어온 요청은 연결 거부된다. `depends_on` 은 "컨테이너 시작"만 기다리고
"준비 완료"는 안 기다린다. 배포 직후 바로 테스트하면 실패할 수 있다.

## 7. 개선 제안 (선택)

지금은 `depends_on: opensearch` 뿐이라 AI 서비스가 OpenSearch 준비 전에 뜬다.
healthcheck 를 붙이면 순서가 보장된다.

```yaml
opensearch:
  healthcheck:
    test: ["CMD-SHELL", "curl -sk -u admin:$${OPENSEARCH_PASSWORD} https://localhost:9200/_cluster/health || exit 1"]
    interval: 10s
    timeout: 5s
    retries: 12
    start_period: 40s

ai-service:
  depends_on:
    opensearch:
      condition: service_healthy
```

필수는 아니다. AI 서비스는 OpenSearch 가 없어도 기동하고, 색인 실패를 best-effort 로
처리해 분석 자체는 계속한다(그 이미지가 검색에서만 빠진다).
다만 배포 직후 몇십 초간 검색이 비어 보이는 혼란은 줄일 수 있다.

## 8. 리소스

```yaml
OPENSEARCH_JAVA_OPTS=-Xms2g -Xmx2g
bootstrap.memory_lock=true
ulimits: memlock -1 / nofile 65536
```

- 힙 2GB 고정. 15GB 인스턴스 기준으로 잡은 값이다
- `memory_lock` 은 힙이 디스크로 스왑되는 것을 막는다. `ulimits.memlock` 이 함께 설정돼야
  하며, 안 되면 부트스트랩 체크에서 기동이 거부될 수 있다
- 같은 인스턴스에 Jenkins·MinIO·Spring 이 함께 올라가면 총 메모리를 한 번 계산해 볼 것

## 9. 상태 확인 명령

```bash
# 플러그인이 실제로 설치돼 있는지 (analysis-nori 가 보여야 한다)
docker compose exec opensearch bin/opensearch-plugin list

# 클러스터 상태 (단일 노드라 green 이 아니라 yellow 여도 정상)
curl -sk -u admin:<비번> https://localhost:9200/_cluster/health

# 인덱스와 문서 수
curl -sk -u admin:<비번> https://localhost:9200/_cat/indices?v

# nori 가 실제로 한글을 쪼개는지 확인
curl -sk -u admin:<비번> -X POST 'https://localhost:9200/screenshot_kb/_analyze' \
  -H 'Content-Type: application/json' \
  -d '{"analyzer":"korean","text":"강남역 맛집 추천"}'
```

마지막 명령의 결과에 `강남`, `역`, `맛집`, `추천` 처럼 형태소가 나뉘어 나오면 정상이다.
`강남역 맛집 추천` 이 통째로 하나의 토큰이면 nori 가 안 먹고 있는 것이다.
