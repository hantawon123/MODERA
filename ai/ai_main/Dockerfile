FROM python:3.12-slim

# 파이썬 런타임 설정: 로그 즉시 출력, .pyc 미생성
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

# 의존성 먼저 설치해 레이어 캐시를 활용한다.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 앱 코드
COPY app ./app

# 비루트 사용자로 실행 (보안)
RUN useradd --create-home --uid 1000 appuser
USER appuser

EXPOSE 8000

# /health 로 컨테이너 상태 점검 (slim 이미지에 curl 이 없어 파이썬으로 확인)
HEALTHCHECK --interval=60s --timeout=5s --start-period=10s --retries=3 \
    CMD python -c "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:8000/health').status==200 else 1)"

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
