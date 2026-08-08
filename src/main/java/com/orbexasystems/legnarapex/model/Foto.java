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
@Table(name = "photos", schema = "public")
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
    private String code;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "photo_date", nullable = false)
    private LocalDate photoDate;

    @Column(name = "photo_time", nullable = false)
    private LocalTime photoTime;

    @Column(name = "photo_url", nullable = false)
    private String photoUrl;

    @Column(name = "uploaded_at")
    private OffsetDateTime uploadedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;
}
