package com.dulo.coffeechat.repository;

import com.dulo.coffeechat.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            select b from Booking b join fetch b.timeSlot s
            where s.startTime > :now
            order by s.startTime
            """)
    List<Booking> findUpcoming(LocalDateTime now);
}
