package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.dto.response.UserDataDeleteResponse;
import com.ssafy.modera.api.domain.user.exception.UserErrorCode;
import com.ssafy.modera.api.domain.user.repository.UserDataResetRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCommandService {

    private final UserDataResetRepository userDataResetRepository;

    @Transactional
    public UserDataDeleteResponse deleteStoredData(Integer userId) {
        if (!userDataResetRepository.existsActiveUser(userId)) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }

        userDataResetRepository.softDeleteAll(userId);
        return new UserDataDeleteResponse();
    }
}
