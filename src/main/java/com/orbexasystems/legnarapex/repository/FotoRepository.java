package com.orbexasystems.legnarapex.repository;

import com.orbexasystems.legnarapex.model.Foto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FotoRepository extends JpaRepository<Foto, UUID> {
    List<Foto> findByExpiresAtBefore(OffsetDateTime now);
    Optional<Foto> findByCode(String code);
}
