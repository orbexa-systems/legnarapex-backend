package com.orbexasystems.legnarapex.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fotos", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Foto {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String codigo;

    @Column(name = "lugar_id", nullable = false)
    private UUID lugarId;

    @Column(name = "fecha_foto", nullable = false)
    private LocalDate fechaFoto;

    @Column(name = "hora_foto", nullable = false)
    private LocalTime horaFoto;

    @Column(name = "url_foto", nullable = false)
    private String urlFoto;

    @Column(name = "fecha_subida")
    private OffsetDateTime fechaSubida;

    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;
}
