package org.silkroadpartnership.theway_noti.user.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Role {
    LEADER(             "인도자",    "manage", false),
    INSTRUMENT_MANAGER( "악기 팀장", "manage", false),
    SINGER_MANAGER(     "싱어 팀장", "manage", false),
    MAIN(               "메인",     "inst",  false),
    SECOND(             "세컨",     "inst",  false),
    DRUMS(              "드럼",     "inst",  false),
    BASS(               "베이스",   "inst",  false),
    ELECTRIC_GUITAR(    "일렉",     "inst",  false),
    ACOUSTIC_GUITAR(    "통기타",   "inst",  false),
    SHEET_MUSIC(        "악보",     "inst",  true),

    SINGER_MALE(        "싱어(남)", "sing",   false),
    SINGER_FEMALE(      "싱어(여)", "sing",   false),
    LYRICS(             "자막",     "sing",  true),

    NONE(               "없음",     "none",  false);

    private final String koreanName;
    private final String group;
    private final boolean required;

    public static Role fromKoreanName(String name) {
        for (Role role : Role.values()) {
            if (role.koreanName.equals(name)) {
                return role;
            }
        }
        return NONE;
    }
}