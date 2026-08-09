"""기존 이미지의 documentVector 를 새 조합식(제목+태그+요약)으로 재임베딩한다.

stages.document_vector_text 가 조합식의 단일 출처다 — 여기서 다르게 조합하면
기존 이미지와 새 이미지가 다른 공간에 흩어진다(그게 이 스크립트의 존재 이유다).

Spring pgvector(analysis_result.embedding)는 이 서비스 소유가 아니라 직접
UPDATE 하지 않는다. 입력 JSON 을 읽어 임베딩만 만들고 UPDATE SQL 을 뱉는다 —
적용은 운영자가 psql 로 한다(백업 먼저).

입력:
  --view    user_image_view 덤프. [{image_id, title, summary, tags(jsonb)}]
            (tags 는 [{"name": ...}] 형태 — 콜백 계약과 같다)
  --targets 갱신 대상. [{image_id, result_id}] — 이미지당 최신 result 1행
출력:
  --out     psql 로 흘릴 UPDATE SQL. 대상인데 view 에 없는 이미지(삭제됨)는
            건너뛰고 주석으로 남긴다.

사용례:
  python scripts/backfill_document_vectors.py \
      --view view_data.json --targets targets.json --out backfill.sql
"""

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app import gemini_client          # noqa: E402
from app.stages import document_vector_text  # noqa: E402


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--view", required=True)
    parser.add_argument("--targets", required=True)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()

    view_rows = json.loads(Path(args.view).read_text()) or []
    targets = json.loads(Path(args.targets).read_text()) or []
    # user_image_view 는 (user_id, image_id) 단위라 같은 image_id 가 여러 행일 수
    # 있고, 분석 FAILED 사용자의 행은 제목이 파일명이고 요약·태그가 빈 깡통이다
    # (실측: 84~86). 깡통을 임베딩하면 벡터가 망가지므로 내용이 가장 많은 행을 쓴다.
    def richness(r: dict) -> int:
        return len(r.get("summary") or "") + len(r.get("tags") or [])
    by_image: dict[int, dict] = {}
    for r in view_rows:
        iid = int(r["image_id"])
        if iid not in by_image or richness(r) > richness(by_image[iid]):
            by_image[iid] = r

    texts: list[str] = []
    rows: list[tuple[int, int]] = []   # (image_id, result_id)
    skipped: list[int] = []
    for t in targets:
        image_id, result_id = int(t["image_id"]), int(t["result_id"])
        row = by_image.get(image_id)
        if row is None:
            skipped.append(image_id)   # 삭제된 이미지 — 화면에 안 나오므로 방치
            continue
        tags = [str(x.get("name", "")) for x in (row.get("tags") or [])]
        texts.append(document_vector_text(
            row.get("title") or "", tags, row.get("summary") or ""))
        rows.append((image_id, result_id))

    print(f"대상 {len(targets)}건 중 임베딩 {len(rows)}건, 건너뜀 {len(skipped)}건",
          file=sys.stderr)
    model, vectors = gemini_client.embed(texts, "DOCUMENT")
    assert len(vectors) == len(rows), f"임베딩 {len(vectors)} != 대상 {len(rows)}"

    with open(args.out, "w") as f:
        f.write(f"-- backfill documentVector ({model}, {len(rows)}건)\n")
        if skipped:
            f.write(f"-- 건너뜀(view 에 없음, 삭제 추정): {skipped}\n")
        f.write("BEGIN;\n")
        for (image_id, result_id), vec in zip(rows, vectors):
            literal = "[" + ",".join(f"{v:.8g}" for v in vec) + "]"
            f.write(
                f"UPDATE analysis_result SET embedding = '{literal}'::vector "
                f"WHERE result_id = {result_id} AND image_id = {image_id};\n"
            )
        f.write("COMMIT;\n")
    print(f"{args.out} 생성 완료 — psql 로 적용할 것", file=sys.stderr)


if __name__ == "__main__":
    main()
