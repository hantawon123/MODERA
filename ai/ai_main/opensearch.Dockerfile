# 한글 형태소 분석(nori) 플러그인을 포함한 OpenSearch 이미지.
# 기본 이미지에는 nori 가 없어 커스텀 빌드가 필요하다.
FROM opensearchproject/opensearch:2.17.0

RUN /usr/share/opensearch/bin/opensearch-plugin install --batch analysis-nori
