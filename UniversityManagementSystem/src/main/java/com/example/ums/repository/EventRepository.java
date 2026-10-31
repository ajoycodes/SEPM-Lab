package com.example.ums.repository;

import com.example.ums.entity.UniversityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<UniversityEvent, Long> {

    List<UniversityEvent> findAllByOrderByStartsAtAscIdAsc();
}
