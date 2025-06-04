package org.silkroadpartnership.theway_noti.shedule.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.silkroadpartnership.theway_noti.shedule.entity.DayType;
import org.silkroadpartnership.theway_noti.shedule.entity.NotiSchedule;
import org.silkroadpartnership.theway_noti.shedule.entity.ScheduleDto;
import org.silkroadpartnership.theway_noti.shedule.entity.TargetType;
import org.silkroadpartnership.theway_noti.shedule.entity.Week;
import org.silkroadpartnership.theway_noti.shedule.repository.NotiScheduleRepository;
import org.silkroadpartnership.theway_noti.user.entity.Role;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotiScheduleService {
  private final NotiScheduleRepository notiScheduleRepository;

  public List<ScheduleDto> findSchedules() {
    List<NotiSchedule> schedules = notiScheduleRepository.findAll();
    return schedules.stream().map(schdl -> {
      return ScheduleDto.builder()
          .scheduleId(Long.toString(schdl.getId()))
          .targetType(schdl.getTargetType().getKoreanName())
          .role(schdl.getRole().getKoreanName())
          .dayType(schdl.getDayType().getKoreanName())
          .targetDay(
              schdl.getDayType() == DayType.WEEK
                  ? Week.valueOf(schdl.getTargetDay()).getKoreanName()
                  : schdl.getTargetDay() + "일")
          .targetHour(schdl.getTargetHour() + "시")
          .content(schdl.getContent())
          .build();
    }).collect(Collectors.toList());
  }

  public void saveSchedule(ScheduleDto dto) {
    NotiSchedule schedule = NotiSchedule.builder()
        .targetType(TargetType.valueOf(dto.getTargetType()))
        .role(Role.valueOf(dto.getRole()))
        .dayType(DayType.valueOf(dto.getDayType()))
        .targetDay(dto.getTargetDay())
        .targetHour(dto.getTargetHour())
        .content(dto.getContent())
        .build();

    notiScheduleRepository.save(schedule);
  }

  public void deleteSchedule(String scheduleId){
    notiScheduleRepository.deleteById(Long.parseLong(scheduleId));
  }

  public List<NotiSchedule> findByNow(){
    ZoneId seoulZone = ZoneId.of("Asia/Seoul");
    ZonedDateTime nowInSeoul = ZonedDateTime.now(seoulZone);
    
    String thisHour = String.valueOf(nowInSeoul.getHour()); 
    String thisWeekDay = nowInSeoul.getDayOfWeek().name(); 
    String thisDay = String.valueOf(nowInSeoul.getDayOfMonth()); 

    return notiScheduleRepository.findByHourAndDay(thisHour, thisWeekDay, thisDay);
  }
}
