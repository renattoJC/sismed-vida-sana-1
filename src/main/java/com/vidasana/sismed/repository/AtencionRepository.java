package com.vidasana.sismed.repository;

import com.vidasana.sismed.entity.Atencion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AtencionRepository extends JpaRepository<Atencion, Long> {

    Optional<Atencion> findByCitaId(Long citaId);

    boolean existsByCitaId(Long citaId);

    long countByFechaAtencionGreaterThanEqualAndFechaAtencionLessThan(
            LocalDateTime desde,
            LocalDateTime hasta);
}