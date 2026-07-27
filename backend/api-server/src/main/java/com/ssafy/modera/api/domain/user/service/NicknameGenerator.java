package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class NicknameGenerator {

    // TODO: 서비스 콘셉트에 맞는 실제 형용사/명사 사전으로 교체한다.
    private static final String[] ADJECTIVES = { "폭신한",
    	    "노래하는",
    	    "느긋한",
    	    "호기심많은",
    	    "용감한",
    	    "수줍은",
    	    "명랑한",
    	    "엉뚱한",
    	    "졸린",
    	    "신나는",
    	    "행복한",
    	    "다정한",
    	    "상냥한",
    	    "씩씩한",
    	    "발랄한",
    	    "차분한",
    	    "든든한",
    	    "영리한",
    	    "재빠른",
    	    "부지런한",
    	    "장난꾸러기",
    	    "사랑스러운",
    	    "배고픈",
    	    "포근한",
    	    "말랑한",
    	    "통통한",
    	    "새침한",
    	    "순수한",
    	    "낭만적인",
    	    "긍정적인",
    	    "꿈꾸는",
    	    "춤추는",
    	    "달리는",
    	    "산책하는",
    	    "모험하는",
    	    "여행하는",
    	    "공부하는",
    	    "책읽는",
    	    "그림그리는",
    	    "요리하는",
    	    "간식먹는",
    	    "해바라기먹는",
    	    "낮잠자는",
    	    "별을보는",
    	    "달을보는",
    	    "구름을따라가는",
    	    "햇살을좋아하는",
    	    "비를기다리는",
    	    "바람을타는",
    	    "꽃을좋아하는",
    	    "숲을걷는",
    	    "바다를보는",
    	    "눈을기다리는",
    	    "노을을보는",
    	    "보물을찾는",
    	    "길을찾는",
    	    "친구를기다리는",
    	    "웃고있는",
    	    "손흔드는",
    	    "기지개켜는"};
    private static final String[] NOUNS = {  "밤톨햄스터",
    	    "쿠키햄스터",
    	    "해바라기햄스터",
    	    "도토리햄스터",
    	    "치즈햄스터",
    	    "모찌햄스터",
    	    "푸딩햄스터",
    	    "젤리햄스터",
    	    "땅콩햄스터",
    	    "호두햄스터",
    	    "아몬드햄스터",
    	    "보리햄스터",
    	    "콩알햄스터",
    	    "완두콩햄스터",
    	    "감자햄스터",
    	    "고구마햄스터",
    	    "옥수수햄스터",
    	    "당근햄스터",
    	    "밤고구마햄스터",
    	    "찹쌀햄스터",
    	    "인절미햄스터",
    	    "만두햄스터",
    	    "호빵햄스터",
    	    "식빵햄스터",
    	    "베이글햄스터",
    	    "와플햄스터",
    	    "마카롱햄스터",
    	    "머핀햄스터",
    	    "도넛햄스터",
    	    "초코햄스터",
    	    "바닐라햄스터",
    	    "카라멜햄스터",
    	    "버터햄스터",
    	    "연유햄스터",
    	    "딸기햄스터",
    	    "복숭아햄스터",
    	    "사과햄스터",
    	    "포도햄스터",
    	    "체리햄스터",
    	    "레몬햄스터",
    	    "귤햄스터",
    	    "자두햄스터",
    	    "망고햄스터",
    	    "메론햄스터",
    	    "블루베리햄스터",
    	    "솜사탕햄스터",
    	    "구름햄스터",
    	    "별빛햄스터",
    	    "달빛햄스터",
    	    "햇살햄스터",
    	    "노을햄스터",
    	    "새벽햄스터",
    	    "봄날햄스터",
    	    "여름햄스터",
    	    "가을햄스터",
    	    "겨울햄스터",
    	    "단풍햄스터",
    	    "눈꽃햄스터",
    	    "민들레햄스터",
    	    "클로버햄스터"};
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
