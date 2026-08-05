package com.ssafy.modera.api.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이미지 리다이렉트 엔드포인트가 인증 없이 열리지 않는지 고정한다.
 *
 * <p>SecurityConfig는 {@code anyRequest().authenticated()}라 새 경로가 자동으로 인증
 * 대상이 된다. 위험은 누군가 PERMIT_ALL_PATHS에 이미지 경로를 추가해 조용히 공개로
 * 바뀌는 경우이므로, 화이트리스트에 들어가지 않았다는 것을 검증한다.
 */
class ImageFileRedirectSecurityTest {

    @Test
    @DisplayName("이미지 파일·썸네일 경로는 인증 예외 목록에 없다(미인증 요청은 401)")
    void imageFileEndpointsRequireAuthentication() {
        String[] permitAll = (String[]) ReflectionTestUtils.getField(
                SecurityConfig.class, "PERMIT_ALL_PATHS");

        List<String> paths = Arrays.asList(permitAll);

        assertThat(paths).noneMatch(path -> path.startsWith("/api/v1/images"));
        // 와일드카드로 이미지까지 열어버리는 항목도 없어야 한다.
        assertThat(paths).doesNotContain("/api/v1/**", "/**");
    }
}
