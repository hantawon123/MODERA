"""기본 카테고리 아이콘을 미리 만들어 버킷에 심는다(멱등).

    python scripts/seed_category_icons.py                 # 기본 13종
    python scripts/seed_category_icons.py --name 부동산    # 특정 이름만
    python scripts/seed_category_icons.py --dry-run       # 무엇이 빠졌는지만 본다

기본 13종은 고정이고 대부분의 이미지가 여기로 분류된다. 미리 채워 두면
런타임 생성이 영구히 사라지고, "아직 없는 아이콘의 첫 조회가 18.6초 걸린다"는
노출도 대부분 없어진다. 아이콘은 사용자와 무관하게 공유되므로(key 에 user_id 가
없다 — app/category_icon.py) 시스템 전체에 한 번만 심으면 끝이다.

기동 훅이 아니라 스크립트인 이유: 생성이 장당 ~19초라 기동에 붙이면 부팅이
그만큼 매달린다. 이미 있는 아이콘은 건너뛰므로(ensure_icon 이 멱등) 몇 번
돌려도 요금이 중복되지 않는다.

.env 의 값을 그대로 쓴다(OPENAI_API_KEY·S3_* — 환경변수로 덮어쓸 수 있다).
"""

import argparse
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app import category_icon, search, storage  # noqa: E402
from app.config import get_settings  # noqa: E402
from app.stages import DEFAULT_CATEGORIES  # noqa: E402


def _seed(name: str) -> tuple[str, str]:
    """(이름, 결과) — 예외를 삼켜서 한 건 실패가 나머지를 막지 않게 한다."""
    category_id = search.stable_id(name)
    try:
        category_icon.ensure_icon(category_id, name)
        return name, f"ok  {category_id}"
    except Exception as e:
        return name, f"실패 {category_id}: {e}"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--name", action="append",
                        help="심을 카테고리 이름(반복 가능). 없으면 기본 13종")
    parser.add_argument("--workers", type=int, default=4,
                        help="동시 생성 수. 장당 ~19초라 직렬이면 13종에 4분 걸린다")
    parser.add_argument("--dry-run", action="store_true",
                        help="생성하지 않고 빠진 것만 출력한다")
    args = parser.parse_args()

    names = args.name or DEFAULT_CATEGORIES
    settings = get_settings()
    bucket = settings.s3_category_icon_bucket
    print(f"버킷: {bucket} / 모델: {settings.image_model_name} "
          f"({settings.image_gen_size}, quality={settings.image_gen_quality})")

    # 버킷에 먼저 닿아 본다. 못 닿으면 object_exists 가 전부 False 를 돌려주므로
    # (장애와 '객체 없음'을 구분하지 않는다) 13장을 다 생성한 뒤 업로드에서
    # 전부 실패한다 — 요금만 나가고 남는 게 없다.
    try:
        storage._client().head_bucket(Bucket=bucket)
    except Exception as e:
        print(f"버킷에 닿지 못했다: {e.__class__.__name__}: {e}\n"
              f"S3_ENDPOINT·S3_ACCESS_KEY·S3_SECRET_KEY 를 확인하고, 버킷이 없으면 "
              f"scripts/seed_minio.py 를 먼저 돌린다. (생성하지 않았다)")
        return 2

    missing = [
        n for n in names
        if not storage.object_exists(category_icon.icon_key(search.stable_id(n)),
                                     bucket=bucket)
    ]
    print(f"대상 {len(names)}종 중 빠진 것 {len(missing)}종: {missing}")
    if not missing:
        return 0
    if args.dry_run:
        # 단가는 gpt-image-1-mini low 1024x1024 기준 $0.005/장(2026-08 실측 확인).
        # 출력에 em-dash 를 쓰지 않는다 — 윈도우 콘솔(cp949)이 인코딩하지 못한다.
        print(f"--dry-run: 생성하지 않았다. 실행 시 예상 {len(missing)}장 "
              f"(약 ${len(missing) * 0.005:.3f})")
        return 0

    failed = 0
    with ThreadPoolExecutor(max_workers=max(1, args.workers)) as pool:
        for name, result in pool.map(_seed, missing):
            print(f"{name}: {result}")
            failed += result.startswith("실패")
    print(f"\n완료: 성공 {len(missing) - failed} / 실패 {failed}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
