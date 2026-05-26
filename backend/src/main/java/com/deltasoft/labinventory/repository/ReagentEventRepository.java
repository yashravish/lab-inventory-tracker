package com.deltasoft.labinventory.repository;

import com.deltasoft.labinventory.domain.ReagentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReagentEventRepository extends JpaRepository<ReagentEvent, Long> {

    List<ReagentEvent> findByReagentIdOrderByCreatedAtDesc(Long reagentId);

    List<ReagentEvent> findTop100ByOrderByCreatedAtDesc();
}
