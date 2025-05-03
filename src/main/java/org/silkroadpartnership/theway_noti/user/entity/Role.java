package org.silkroadpartnership.theway_noti.user.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Role {
    LEADER("인도자",false,true),
    MAIN("메인",false,true),
    SECOND("세컨",false,true),
    DRUMS("드럼",false,true),
    BASS("베이스",false,true),
    ELECTRIC_GUITAR("일렉",false,true),
    ACOUSTIC_GUITAR("통기타",false,false),
    INSTRUMENT_MANAGER("악기 팀장",false,false),
    SINGER_MALE("싱어(남)",true,true),
    SINGER_FEMALE("싱어(여)",true,true),
    SINGER_MANAGER("싱어 팀장",false,false),
    SHEET_MUSIC("악보",false,true),
    LYRICS("자막",false,true),
    NONE("없음",false,false);

    private final String koreanName;
    private final boolean needParse;
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