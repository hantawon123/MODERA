package com.ssafy.modera.global.domain;

import org.springframework.http.HttpStatus;

/**
 * 모든 에러 코드 enum 이 구현하는 계약.
 * {@code BusinessException}, {@code AuthorizationException}, {@code CommonResponse} 는
 * 이 타입만 알고 있으므로 도메인은 자기 enum 을 자유롭게 추가할 수 있다.
 *
 * <h2>코드 prefix 레지스트리 (필수 준수)</h2>
 * enum 이 분리되어 있어 <b>컴파일러가 코드 번호의 유일성을 보장하지 못한다.</b>
 * 새 도메인을 추가할 때는 아래 표에 prefix 를 먼저 등록하고, 그 prefix 안에서만 번호를 발급한다.
 *
 * <pre>
 * prefix | enum                              | 위치                      | 범위
 * -------|-----------------------------------|---------------------------|--------------------------------
 *   G    | GlobalErrorCode                   | global/domain             | HTTP·서버 공통 오류
 *   A    | GlobalErrorCode                   | global/domain             | 인증/인가 '인프라' (JWT 파싱, 필터, 핸들러)
 *   U    | UserErrorCode                     | domain/user/exception     | 회원·인증 '비즈니스' (가입/로그인/토큰/검증)
 *   I    | (예정) ImageErrorCode             | domain/image/exception    | 이미지 등록·업로드·조회
 *   T    | (예정) TagErrorCode               | domain/tag/exception      | 태그
 *   C    | (예정) CategoryErrorCode          | domain/category/exception | 카테고리
 *   S    | (예정) SearchErrorCode            | domain/search/exception   | 검색
 *   J    | (예정) AnalysisJobErrorCode       | domain/analysis/exception | 분석 파이프라인·콜백
 * </pre>
 *
 * <h2>규칙</h2>
 * <ol>
 *   <li>하나의 enum 은 하나의 prefix 만 사용한다. 다른 prefix 를 섞지 않는다.</li>
 *   <li>번호는 해당 enum 안에서 1부터 순차 발급하며, <b>이미 나간 번호는 재사용하지 않는다.</b>
 *       상수를 지우거나 옮겨도 번호는 비워 둔다 (클라이언트가 옛 코드를 매핑하고 있을 수 있다).</li>
 *   <li>A007·A008·A010~A023 은 U 로 이관되어 <b>영구 결번</b>이다. 재사용 금지.</li>
 *   <li>도메인이 던지는 에러는 도메인 enum 에 둔다. global 에는 어느 도메인에도 속하지 않는 것만 남긴다.</li>
 * </ol>
 */
public interface ErrorCode {

    HttpStatus getStatus();

    String getCode();

    String getMessage();
}
