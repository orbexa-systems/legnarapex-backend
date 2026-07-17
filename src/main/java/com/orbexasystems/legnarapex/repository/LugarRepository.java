package com.orbexasystems.legnarapex.repository;

import com.orbexasystems.legnarapex.model.Lugar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LugarRepository extends JpaRepository<Lugar, UUID> {
    List<Lugar> findByActivoTrue();
}
