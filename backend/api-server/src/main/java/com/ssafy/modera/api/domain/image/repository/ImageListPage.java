package com.ssafy.modera.api.domain.image.repository;

import java.util.List;

public record ImageListPage(List<ImageListRow> content, long totalElements) {
}
