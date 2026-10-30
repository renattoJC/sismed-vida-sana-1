package com.vidasana.sismed.controller;

import com.vidasana.sismed.repository.AtencionRepository;
import com.vidasana.sismed.repository.CitaRepository;
import com.vidasana.sismed.repository.MedicoRepository;
import com.vidasana.sismed.repository.PacienteRepository;
import com.vidasana.sismed.repository.PagoRepository;
import com.vidasana.sismed.repository.IngresoMensual;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Controller
public class ReporteController {

    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final CitaRepository citaRepository;
    private final AtencionRepository atencionRepository;
    private final PagoRepository pagoRepository;

    public ReporteController(
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

    @GetMapping("/reportes")
    public String reportes(Model model) {

        long pacientes =
                pacienteRepository.count();

        long medicos =
                medicoRepository.count();

        long citas =
                citaRepository.count();

        long atenciones =
                atencionRepository.count();

        BigDecimal ingresos =
                pagoRepository.findAll()
                        .stream()
                        .filter(pago ->
                                "PAGADO".equalsIgnoreCase(
                                        pago.getEstado()))
                        .map(pago -> pago.getMonto())
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        model.addAttribute(
                "pacientes",
                pacientes
        );

        model.addAttribute(
                "medicos",
                medicos
        );

        model.addAttribute(
                "citas",
                citas
        );

        model.addAttribute(
                "atenciones",
                atenciones
        );

        model.addAttribute(
                "ingresos",
                ingresos
        );

        return "reportes";
    }

    @GetMapping("/reportes/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel() throws IOException {
        LocalDateTime ahora = LocalDateTime.now();
        YearMonth mesActual = YearMonth.from(ahora);
        YearMonth primerMes = mesActual.minusMonths(11);
        LocalDateTime desde = primerMes.atDay(1).atStartOfDay();
        LocalDateTime hasta = mesActual.plusMonths(1).atDay(1).atStartOfDay();

        Map<YearMonth, BigDecimal> ingresosPorMes = new HashMap<>();
        for (IngresoMensual ingreso : pagoRepository.resumirIngresosPorMes(
                "PAGADO", desde, hasta)) {
            ingresosPorMes.put(
                    YearMonth.of(ingreso.getAnio(), ingreso.getMes()),
                    ingreso.getTotal());
        }

        byte[] contenido;
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Sheet resumen = workbook.createSheet("Resumen");
            agregarFila(resumen, 0, "Indicador", "Valor", headerStyle);
            agregarFila(resumen, 1, "Fecha de generación", ahora.toString(), null);
            agregarFila(resumen, 2, "Pacientes registrados", pacienteRepository.count(), null);
            agregarFila(resumen, 3, "Médicos registrados", medicoRepository.count(), null);
            agregarFila(resumen, 4, "Citas registradas", citaRepository.count(), null);
            agregarFila(resumen, 5, "Atenciones realizadas", atencionRepository.count(), null);
            agregarFila(resumen, 6, "Ingresos cobrados (S/)", pagoRepository.sumarPorEstado("PAGADO"), null);
            resumen.setColumnWidth(0, 7500);
            resumen.setColumnWidth(1, 5500);

            Sheet ingresos = workbook.createSheet("Ingresos mensuales");
            agregarFila(ingresos, 0, "Mes", "Ingresos cobrados (S/)", headerStyle);
            for (int i = 0; i < 12; i++) {
                YearMonth mes = primerMes.plusMonths(i);
                String etiqueta = mes.getMonth()
                        .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"))
                        + " " + mes.getYear();
                agregarFila(
                        ingresos,
                        i + 1,
                        etiqueta,
                        ingresosPorMes.getOrDefault(mes, BigDecimal.ZERO),
                        null);
            }
            ingresos.setColumnWidth(0, 6500);
            ingresos.setColumnWidth(1, 7000);

            workbook.write(output);
            contenido = output.toByteArray();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-sismed.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(contenido);
    }

    private void agregarFila(
            Sheet sheet,
            int rowIndex,
            String label,
            Object value,
            CellStyle labelStyle) {
        Row row = sheet.createRow(rowIndex);
        var labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        if (labelStyle != null) {
            labelCell.setCellStyle(labelStyle);
        }

        var valueCell = row.createCell(1);
        if (value instanceof Number number) {
            valueCell.setCellValue(number.doubleValue());
        } else {
            valueCell.setCellValue(String.valueOf(value));
        }
        if (rowIndex == 0 && labelStyle != null) {
            valueCell.setCellStyle(labelStyle);
        }
    }
}