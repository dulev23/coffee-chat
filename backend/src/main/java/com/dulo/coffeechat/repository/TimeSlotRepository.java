package com.dulo.coffeechat.repository;

import com.dulo.coffeechat.model.TimeSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findByBookedFalseAndStartTimeAfterOrderByStartTime(LocalDateTime now);

    List<TimeSlot> findByStartTimeBetween(LocalDateTime from, LocalDateTime to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TimeSlot s where s.id = :id")
    Optional<TimeSlot> findByIdForUpdate(Long id);
}
