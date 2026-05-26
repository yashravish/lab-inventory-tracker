package com.deltasoft.labinventory.repository;

import com.deltasoft.labinventory.domain.Reagent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReagentRepository extends JpaRepository<Reagent, Long> {

    @Query("""
        select r from Reagent r
        where lower(r.name) like lower(concat('%', :q, '%'))
           or lower(coalesce(r.supplier, '')) like lower(concat('%', :q, '%'))
           or lower(coalesce(r.storageLocation, '')) like lower(concat('%', :q, '%'))
    """)
    Page<Reagent> search(@Param("q") String q, Pageable pageable);

    Optional<Reagent> findFirstByNameIgnoreCase(String name);
}