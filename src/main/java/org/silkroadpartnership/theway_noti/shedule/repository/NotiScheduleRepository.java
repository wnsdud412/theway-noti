package org.silkroadpartnership.theway_noti.shedule.repository;

import java.util.List;

import org.silkroadpartnership.theway_noti.shedule.entity.NotiSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotiScheduleRepository extends JpaRepository<NotiSchedule, Long> {

  @Query(value = "SELECT * FROM noti_schedule ns " +
      "WHERE ns.target_hour = :thisHour " +
      "AND (ns.target_day = :thisWeekday " +
      "OR ns.target_day = :thisDay)", nativeQuery = true)
  List<NotiSchedule> findByHourAndDay(@Param("thisHour") String thisHour,
      @Param("thisWeekday") String thisWeekday,
      @Param("thisDay") String thisDay);
}
