package com.orbexasystems.legnarapex.repository;

import com.orbexasystems.legnarapex.model.Foto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

public interface FotoRepository extends JpaRepository<Foto, UUID> {
    Optional<Foto> findByCodeAndPhotoDateAndPhotoTime(String code, LocalDate photoDate, LocalTime photoTime);
}
