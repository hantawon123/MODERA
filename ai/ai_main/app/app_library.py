"""앱 직결 라이브러리 API — 분석 현황·태그·카테고리·홈·검색.

명세 5 장(5-1 분석 현황, 5-6 작업 목록), 7 장(7-1 태그, 7-2 카테고리, 7-3 홈),
8-1 통합 검색. 이미지 단건을 다루지 않는 화면용 집계·목록이 여기 모인다
(이미지 자체는 app_images.py).

라우트 본문은 main.py 에 있던 것을 그대로 옮겼다 — `@app.` 이 `@router.` 로
바뀐 것뿐이다. 공유 헬퍼는 deps.py 에 있다.
"""

import asyncio
import logging

from fastapi import APIRouter, Depends, Query

from . import responses, search
from .timeutil import now_iso
from .deps import (
    CurrentUserId,
    _resolve_filters,
    _tag_refs,
    _thumbnail_url,
    require_internal_token,
)
from .jobs import job_store
from .schemas import (
    ActiveAnalysis,
    AnalysisJob,
    AnalysisSummary,
    ApiResponse,
    CategoryCard,
    CategoryListData,
    HomeAnalysisStatus,
    HomeCategory,
    HomeRecentImage,
    HomeResponse,
    PageData,
    SearchResultItem,
    TagCount,
    TagItem,
)

logger = logging.getLogger(__name__)

router = APIRouter()


# ── 5-1 분석 현황 요약 ────────────────────────────────────────────────────
@router.get("/api/v1/analysis/summary", dependencies=[Depends(require_internal_token)],
         response_model=ApiResponse[AnalysisSummary])
async def app_analysis_summary(user_id: CurrentUserId):
    jobs = job_store.list_by_user(user_id)

    stage_counts: dict[str, dict[str, int]] = {}
    overall = {"QUEUED": 0, "PROCESSING": 0, "COMPLETED": 0,
               "FAILED": 0, "EMPTY": 0, "CANCELED": 0}
    for job in jobs:
        stage, status = job["stage"], job["status"]
        stage_counts.setdefault(stage, {})
        stage_counts[stage][status] = stage_counts[stage].get(status, 0) + 1
        if status in overall:
            overall[status] += 1

    summary = AnalysisSummary(
        total=len(jobs), stage_counts=stage_counts, overall_counts=overall
    )
    return responses.success(summary.model_dump(by_alias=True))


# ── 5-6 분석 작업 목록 ────────────────────────────────────────────────────
@router.get("/api/v1/analysis/jobs", dependencies=[Depends(require_internal_token)],
         response_model=ApiResponse[PageData[AnalysisJob]])
async def app_analysis_jobs(
    user_id: CurrentUserId,
    status: str | None = Query(None),
    page: int = Query(0),
    size: int = Query(20),
):
    jobs = job_store.list_by_user(user_id)
    if status:
        wanted = {s.strip().upper() for s in status.split(",") if s.strip()}
        jobs = [j for j in jobs if j["status"] in wanted]

    # 최근 갱신 순으로 보여 준다.
    jobs.sort(key=lambda j: j.get("updated_at") or "", reverse=True)
    total = len(jobs)
    window = jobs[max(0, page) * size: max(0, page) * size + size]

    items = [
        AnalysisJob(
            job_id=j["job_id"], image_id=j["image_id"],
            stage=j["stage"], status=j["status"],
            retryable=bool(j.get("error", {}) or {}) and
                      bool((j.get("error") or {}).get("retryable")),
            error_code=(j.get("error") or {}).get("code"),
            updated_at=j.get("updated_at"),
        ).model_dump(by_alias=True)
        for j in window
    ]
    return responses.success(responses.page_data(items, page, size, total))


# ── 7-1 태그 목록 ─────────────────────────────────────────────────────────
@router.get("/api/v1/tags", dependencies=[Depends(require_internal_token)],
         response_model=ApiResponse[PageData[TagItem]])
async def app_tag_list(
    user_id: CurrentUserId,
    q: str | None = Query(None, description="태그명 부분 일치(자동완성용)"),
    page: int = Query(0),
    size: int = Query(20),
    sort: str | None = Query(None, description="usageCount,desc(기본)/name,asc/createdAt,desc"),
):
    # 명세 7-1 정렬. 집계 결과가 이미 메모리에 있어 여기서 정렬한다.
    # createdAt 은 태그에 생성시각이 없어(이름 집계라) usageCount 순으로 대체한다.
    tag_sorts = {"usageCount,desc", "name,asc", "createdAt,desc"}
    sort_value = (sort or "usageCount,desc").strip()
    if sort_value not in tag_sorts:
        raise search.InvalidSortError(f"지원하지 않는 정렬입니다: {sort_value}")

    try:
        import asyncio
        items = await asyncio.to_thread(search.aggregate_tags, user_id, 500)
    except Exception as e:
        logger.exception("태그 조회 실패")
        return responses.failure("INTERNAL_ERROR", "태그를 조회하지 못했습니다.",
                                 str(e)[:300], http_status=500)

    if q:
        needle = q.strip().lower()
        items = [i for i in items if needle in i["name"].lower()]

    if sort_value == "name,asc":
        items.sort(key=lambda i: i["name"])
    else:
        items.sort(key=lambda i: (-i["count"], i["name"]))

    total = len(items)
    window = items[max(0, page) * size: max(0, page) * size + size]
    tags = [
        TagItem(tag_id=search.stable_id(i["name"]), name=i["name"],
                usage_count=i["count"]).model_dump(by_alias=True)
        for i in window
    ]
    return responses.success(responses.page_data(tags, page, size, total))


# ── 7-2 카테고리 목록 ─────────────────────────────────────────────────────
@router.get("/api/v1/categories", dependencies=[Depends(require_internal_token)],
         response_model=ApiResponse[CategoryListData])
async def app_category_list(
    user_id: CurrentUserId,
    sort: str | None = Query(
        None,
        description="name,asc(기본)/updatedAt,desc(최신 업로드순)/imageCount,desc(사진 많은 순)",
    ),
):
    """카테고리 목록(명세 7-2).

    **페이지네이션은 프론트 요청으로 뺐다**(명세와 다른 부분, 팀 합의).
    카테고리는 기본 17종에 AGENT 가 분석 중 새로 만든 것이 더해지는 정도라 수가 적다.
    사람이 직접 만드는 경로는 없다. 그래서 항상 전체를 한 번에 돌려주고,
    `page`·`size` 를 보내도 무시한다.
    """
    # 명세 7-2 정렬. 카테고리 화면의 정렬 드롭다운이 이 세 값을 쓴다.
    category_sorts = {"name,asc", "updatedAt,desc", "imageCount,desc"}
    sort_value = (sort or "name,asc").strip()
    if sort_value not in category_sorts:
        raise search.InvalidSortError(f"지원하지 않는 정렬입니다: {sort_value}")

    try:
        import asyncio
        items = await asyncio.to_thread(search.aggregate_categories, user_id, 500)
    except Exception as e:
        logger.exception("카테고리 조회 실패")
        return responses.failure("INTERNAL_ERROR", "카테고리를 조회하지 못했습니다.",
                                 str(e)[:300], http_status=500)

    if sort_value == "updatedAt,desc":
        items.sort(key=lambda i: i.get("last_updated_at") or "", reverse=True)
    elif sort_value == "imageCount,desc":
        items.sort(key=lambda i: (-i["count"], i["name"]))
    else:
        items.sort(key=lambda i: i["name"])

    cards = [
        CategoryCard(
            category_id=search.stable_id(i["name"]),
            name=i["name"],
            # 카테고리 아이콘. key 가 categoryId 라 주소를 문자열로 조립하면 끝이고
            # (매핑 조회 없음), 없으면 그 주소를 처음 열 때 생성된다(category_icon.py).
            icon_url=f"/api/v1/categories/{search.stable_id(i['name'])}/icon",
            # 이 카테고리에 가장 최근 분류된 사진의 썸네일을 그대로 가리킨다.
            # 카테고리 전용 이미지를 따로 만들지 않는다(사진마다 썸네일 1장).
            thumbnail_url=(_thumbnail_url(i["thumbnail_image_id"],
                                         i.get("thumbnail_s3_key"))
                           if i.get("thumbnail_image_id") else None),
            image_count=i["count"],
            # 이 카테고리 안에서 각 태그가 몇 장에 붙어 있는지까지 함께 내려준다.
            tags=[TagCount(tag_id=search.stable_id(t["name"]),
                           name=t["name"], image_count=t["count"])
                  for t in i.get("tags") or []],
            updated_at=i.get("last_updated_at"),
        ).model_dump(by_alias=True)
        for i in items
    ]
    return responses.success({"list": cards})


# ── 7-3 홈 대시보드 요약 ─────────────────────────────────────────────────
# 진행률은 실제 퍼센트를 잴 수 없어 단계로 환산한다. 단계 수가 정해져 있어
# 사용자에게 보여줄 진행 막대로는 충분하고, 명세의 0~100 정수 계약도 지킨다.
_STAGE_PROGRESS = {"LLM": 25, "IMAGE_ANALYSIS": 50, "AGENT": 75, "INDEXING": 90}


@router.get("/api/v1/home", dependencies=[Depends(require_internal_token)],
         response_model=ApiResponse[HomeResponse])
async def app_home(user_id: CurrentUserId):
    """홈 화면에 필요한 것을 한 번에 모아 준다(명세 7-3).

    모바일 초기 로딩에서 5-1·7-2·6-1 을 각각 부르지 않아도 되게 하는 집계 API 다.
    """
    import asyncio

    jobs = job_store.list_by_user(user_id)
    queued = [j for j in jobs if j["status"] == "QUEUED"]
    processing = [j for j in jobs if j["status"] == "PROCESSING"]
    failed = [j for j in jobs if j["status"] == "FAILED"]

    active: ActiveAnalysis | None = None
    if processing:
        # 명세: "가장 먼저 시작된 진행 중 작업"
        first = min(processing, key=lambda j: j.get("created_at") or "")
        found = await asyncio.to_thread(search.get_image, first["image_id"])
        active = ActiveAnalysis(
            job_id=first["job_id"], image_id=first["image_id"],
            file_name=(found or {}).get("file_name"),
            title=(found or {}).get("title", ""),
            thumbnail_url=_thumbnail_url(first["image_id"],
                                         (found or {}).get("s3_key")),
            stage=first["stage"], status=first["status"],
            progress=_STAGE_PROGRESS.get(first["stage"], 0),
        )

    try:
        categories = await asyncio.to_thread(search.aggregate_categories, user_id, 500)
        # 명세: updatedAt,desc 로 최대 8개
        categories.sort(key=lambda c: c.get("last_updated_at") or "", reverse=True)
        # 명세: 분석 완료 시각 내림차순으로 최대 4개
        images, _ = await asyncio.to_thread(
            search.list_images, user_id, 0, 4, None, None,
            ["COMPLETED", "EMPTY"], None, None, None, "createdAt,desc",
        )
    except Exception as e:
        logger.exception("홈 집계 실패")
        return responses.failure("INTERNAL_ERROR", "홈 정보를 조회하지 못했습니다.",
                                 str(e)[:300], http_status=500)

    home = HomeResponse(
        home_date=now_iso()[:10],
        analysis_status=HomeAnalysisStatus(
            has_active_jobs=bool(queued or processing),
            queued_count=len(queued),
            processing_count=len(processing),
            failed_count=len(failed),
            active_analysis=active,
        ),
        categories=[
            HomeCategory(
                category_id=search.stable_id(c["name"]),
                name=c["name"],
                image_count=c["count"],
                # 명세 7-3: 대표 태그 최대 3개 (7-2 의 4개와 다르다)
                tags=_tag_refs([t["name"] for t in (c.get("tags") or [])[:3]]),
                updated_at=c.get("last_updated_at"),
            )
            for c in categories[:8]
        ],
        recent_images=[
            HomeRecentImage(
                image_id=img["image_id"],
                title=img.get("title", ""),
                thumbnail_url=_thumbnail_url(img["image_id"], img.get("s3_key")),
                tags=_tag_refs(img.get("tags") or []),
                favorite=img.get("favorite", False),
                analyzed_at=img.get("created_at"),
            )
            for img in images
        ],
    )
    return responses.success(home.model_dump(by_alias=True))


# ── 8-1 통합 검색 ─────────────────────────────────────────────────────────
@router.get("/api/v1/search", dependencies=[Depends(require_internal_token)],
         response_model=ApiResponse[PageData[SearchResultItem]])
async def app_search(
    user_id: CurrentUserId,
    q: str = Query(...),
    scope: str = Query("ALL", description="ALL(기본)/OCR/TAG/STRUCTURED"),
    category_id: int | None = Query(None, alias="categoryId"),
    tag_id: int | None = Query(None, alias="tagId"),
    page: int = Query(0),
    size: int = Query(20),
    sort: str | None = Query(
        None,
        description="relevance(기본)/imageId,asc/lastViewedAt,desc/uploadedAt,desc/createdAt,desc",
    ),
):
    if not q.strip():
        return responses.failure("INVALID_PARAMETER", "요청 값이 올바르지 않습니다.",
                                 [{"field": "q", "message": "필수 값입니다."}])
    if scope.upper() not in ("ALL", "OCR", "TAG", "STRUCTURED"):
        return responses.failure("INVALID_PARAMETER", "요청 값이 올바르지 않습니다.",
                                 [{"field": "scope",
                                   "message": "ALL/OCR/TAG/STRUCTURED 중 하나여야 합니다."}])
    try:
        import asyncio
        category, tag = await _resolve_filters(user_id, category_id, tag_id)
        hits, total = await asyncio.to_thread(
            search.keyword_search, user_id, q, category, size, page, tag,
            scope, sort,
        )
    except search.InvalidSortError:
        raise
    except Exception as e:
        logger.exception("검색 실패")
        return responses.failure("INTERNAL_ERROR", "검색에 실패했습니다.",
                                 str(e)[:300], http_status=500)

    items = [
        SearchResultItem(
            image_id=h["image_id"],
            title=h.get("title", ""),
            summary=h.get("summary", ""),
            thumbnail_url=_thumbnail_url(h["image_id"], h.get("s3_key")),
            score=h.get("score", 0.0),
            tags=_tag_refs(h.get("tags") or []),
            last_viewed_at=h.get("last_viewed_at"),
            uploaded_at=h.get("uploaded_at"),
            created_at=h.get("created_at"),
        ).model_dump(by_alias=True)
        for h in hits
    ]
    return responses.success(responses.page_data(items, page, size, total))
