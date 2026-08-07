package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.dto.response.UserInfoResponse;
import com.ssafy.modera.api.domain.user.exception.UserErrorCode;
import com.ssafy.modera.api.domain.user.repository.UserInfoRow;
import com.ssafy.modera.api.domain.user.repository.UserQueryRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserQueryRepository userQueryRepository;

    public UserInfoResponse getMyInfo(Integer userId) {
        UserInfoRow row = userQueryRepository.findUserInfo(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        return new UserInfoResponse(
                row.userId(),
                row.loginId(),
                row.email(),
                row.notification(),
                row.backgroundAnalysis()
        );
    }
}
