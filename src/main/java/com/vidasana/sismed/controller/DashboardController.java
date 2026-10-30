package com.vidasana.sismed.controller;

import com.vidasana.sismed.repository.AtencionRepository;
import com.vidasana.sismed.repository.CitaRepository;
import com.vidasana.sismed.repository.IngresoMensual;
import com.vidasana.sismed.repository.MedicoRepository;
import com.vidasana.sismed.repository.PacienteRepository;
import com.vidasana.sismed.repository.PagoRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Controller
public class DashboardController {

    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final CitaRepository citaRepository;
    private final AtencionRepository atencionRepository;
    private final PagoRepository pagoRepository;

    public DashboardController(
            PacienteRepository pacienteRepository,
            MedicoRepository medicoRepository,
            CitaRepository citaRepository,
            AtencionRepository atencionRepository,
            PagoRepository pagoRepository) {

        this.pacienteRepository = pacienteRepository;
        this.medicoRepository = medicoRepository;
        this.citaRepository = citaRepository;
        this.atencionRepository = atencionRepository;
        this.pagoRepository = pagoRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        LocalDateTime ahora = LocalDateTime.now();
        YearMonth mesActual = YearMonth.from(ahora);
        LocalDateTime inicioMes = mesActual.atDay(1).atStartOfDay();
        LocalDateTime inicioMesSiguiente = mesActual.plusMonths(1).atDay(1).atStartOfDay();

        long totalPacientes =
                pacienteRepository.count();

        long totalMedicos =
                medicoRepository.count();

        long totalCitas =
                citaRepository.count();

        long totalAtenciones =
                atencionRepository.count();

        BigDecimal totalPagos =
                pagoRepository.sumarPorEstado("PAGADO");

        long atencionesDelMes =
                atencionRepository.countByFechaAtencionGreaterThanEqualAndFechaAtencionLessThan(
                        inicioMes,
                        inicioMesSiguiente);

        List<ActividadReciente> actividadReciente = new ArrayList<>();
        pacienteRepository.findTop5ByOrderByFechaRegistroDesc()
                .forEach(paciente -> actividadReciente.add(new ActividadReciente(
                        "Paciente registrado",
                        paciente.getNombres() + " " + paciente.getApellidos(),
                        paciente.getFechaRegistro())));
        citaRepository.findTop5ByOrderByFechaRegistroDesc()
                .forEach(cita -> actividadReciente.add(new ActividadReciente(
                        "Cita creada",
                        cita.getPaciente().getNombres() + " "
                                + cita.getPaciente().getApellidos()
                                + " · cita programada para "
                                + cita.getFechaHora().toString().replace('T', ' '),
                        cita.getFechaRegistro())));
        actividadReciente.sort(Comparator.comparing(
                ActividadReciente::fecha).reversed());

        YearMonth primerMes = mesActual.minusMonths(5);
        Map<YearMonth, BigDecimal> ingresosPorMes = new TreeMap<>();
        for (int i = 0; i < 6; i++) {
            ingresosPorMes.put(primerMes.plusMonths(i), BigDecimal.ZERO);
        }

        List<IngresoMensual> ingresosConsultados =
                pagoRepository.resumirIngresosPorMes(
                        "PAGADO",
                        primerMes.atDay(1).atStartOfDay(),
                        mesActual.plusMonths(1).atDay(1).atStartOfDay());
        for (IngresoMensual ingreso : ingresosConsultados) {
            YearMonth mes = YearMonth.of(ingreso.getAnio(), ingreso.getMes());
            ingresosPorMes.put(mes, ingreso.getTotal());
        }

        BigDecimal ingresoMaximo = ingresosPorMes.values().stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        List<IngresoMensualVista> ingresosMensuales = ingresosPorMes.entrySet()
                .stream()
                .map(entrada -> new IngresoMensualVista(
                        entrada.getKey().getMonth()
                                .getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es"))
                                + " " + entrada.getKey().getYear(),
                        entrada.getValue(),
                        ingresoMaximo.signum() == 0 ? 0
                                : entrada.getValue()
                                        .multiply(BigDecimal.valueOf(100))
                                        .divide(ingresoMaximo, 0, java.math.RoundingMode.HALF_UP)
                                        .intValue()))
                .toList();

        model.addAttribute(
                "totalPacientes",
                totalPacientes
        );

        model.addAttribute(
                "totalMedicos",
                totalMedicos
        );

        model.addAttribute(
                "totalCitas",
                totalCitas
        );

        model.addAttribute(
                "totalAtenciones",
                totalAtenciones
        );

        model.addAttribute(
                "totalPagos",
                totalPagos
        );

        model.addAttribute("atencionesDelMes", atencionesDelMes);
        model.addAttribute("actividadReciente", actividadReciente.stream().limit(10).toList());
        model.addAttribute("ingresosMensuales", ingresosMensuales);

        return "dashboard";
    }

    public record ActividadReciente(String tipo, String descripcion, LocalDateTime fecha) {
    }

    public record IngresoMensualVista(String etiqueta, BigDecimal total, int porcentaje) {
    }
}