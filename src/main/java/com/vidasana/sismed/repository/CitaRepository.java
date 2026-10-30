package com.vidasana.sismed.repository;

import com.vidasana.sismed.entity.Cita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findAllByOrderByFechaHoraAsc();

    List<Cita> findByEstadoOrderByFechaHoraAsc(String estado);

    List<Cita> findTop5ByOrderByFechaRegistroDesc();
}