package com.vidasana.sismed.repository;

import com.vidasana.sismed.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByDni(String dni);

    boolean existsByDni(String dni);

    List<Paciente> findTop5ByOrderByFechaRegistroDesc();
}