package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class NicknameGenerator {

    // TODO: 서비스 콘셉트에 맞는 실제 형용사/명사 사전으로 교체한다.
    private static final String[] ADJECTIVES = {"행복한", "용감한", "빛나는", "차분한", "즐거운", "공주", "못생긴"};
    private static final String[] NOUNS = {"민국이", "채린이", "승준이", "재훈이", "상현이", "태원이"};
    private static final int MAX_ATTEMPTS = 100;

    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public String generateUnique() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String nickname = ADJECTIVES[random.nextInt(ADJECTIVES.length)]
                    + NOUNS[random.nextInt(NOUNS.length)]
                    + String.format("%04d", random.nextInt(10_000));
            if (!userRepository.existsByNickname(nickname)) {
                return nickname;
            }
        }
        throw new IllegalStateException("고유 닉네임을 생성하지 못했습니다.");
    }
}
