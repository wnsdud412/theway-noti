package org.silkroadpartnership.theway_noti.user.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Role {
    LEADER("인도자",false,true,null),
    INSTRUMENT_MANAGER("악기 팀장",false,false,null),
    SINGER_MANAGER("싱어 팀장",false,false,null),
    
    MAIN("메인",false,true,Role.INSTRUMENT_MANAGER),
    SECOND("세컨",false,true,Role.INSTRUMENT_MANAGER),
    DRUMS("드럼",false,true,Role.INSTRUMENT_MANAGER),
    BASS("베이스",false,true,Role.INSTRUMENT_MANAGER),
    ELECTRIC_GUITAR("일렉",false,true,Role.INSTRUMENT_MANAGER),
    ACOUSTIC_GUITAR("통기타",false,false,Role.INSTRUMENT_MANAGER),
    SHEET_MUSIC("악보",false,true,Role.INSTRUMENT_MANAGER),

    SINGER_MALE("싱어(남)",true,true,Role.SINGER_MANAGER),
    SINGER_FEMALE("싱어(여)",true,true,Role.SINGER_MANAGER),
    LYRICS("자막",false,true,Role.SINGER_MANAGER),

    NONE("없음",false,false,null);

    private final String koreanName;
    private final boolean needParse;
    private final boolean required;
    private final Role managerRole;

    public static Role fromKoreanName(String name) {
        for (Role role : Role.values()) {
            if (role.koreanName.equals(name)) {
                return role;
            }
        }
        return NONE;
    }
}