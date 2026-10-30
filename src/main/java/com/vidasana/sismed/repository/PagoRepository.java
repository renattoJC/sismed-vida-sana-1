package com.vidasana.sismed.repository;

import com.vidasana.sismed.entity.Pago;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findAllByOrderByFechaPagoDesc();

    List<Pago> findByPacienteIdOrderByFechaPagoDesc(Long pacienteId);

    @Query("select coalesce(sum(p.monto), 0) from Pago p where upper(p.estado) = upper(:estado)")
    BigDecimal sumarPorEstado(@Param("estado") String estado);

    @Query(value = """
            SELECT YEAR(p.fecha_pago) AS anio,
                   MONTH(p.fecha_pago) AS mes,
                   SUM(p.monto) AS total
            FROM pagos p
            WHERE UPPER(p.estado) = UPPER(:estado)
              AND p.fecha_pago >= :desde
              AND p.fecha_pago < :hasta
            GROUP BY YEAR(p.fecha_pago), MONTH(p.fecha_pago)
            ORDER BY anio, mes
            """, nativeQuery = true)
    List<IngresoMensual> resumirIngresosPorMes(
            @Param("estado") String estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}