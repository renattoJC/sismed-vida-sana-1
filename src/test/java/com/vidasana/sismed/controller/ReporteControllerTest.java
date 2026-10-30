package com.vidasana.sismed.controller;

import com.vidasana.sismed.repository.AtencionRepository;
import com.vidasana.sismed.repository.CitaRepository;
import com.vidasana.sismed.repository.MedicoRepository;
import com.vidasana.sismed.repository.PacienteRepository;
import com.vidasana.sismed.repository.PagoRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReporteControllerTest {

    @Test
    void exportarExcelDevuelveResumenYDoceMesesDeIngresos() throws Exception {
        PacienteRepository pacienteRepository = mock(PacienteRepository.class);
        MedicoRepository medicoRepository = mock(MedicoRepository.class);
        CitaRepository citaRepository = mock(CitaRepository.class);
        AtencionRepository atencionRepository = mock(AtencionRepository.class);
        PagoRepository pagoRepository = mock(PagoRepository.class);

        when(pacienteRepository.count()).thenReturn(3L);
        when(medicoRepository.count()).thenReturn(2L);
        when(citaRepository.count()).thenReturn(5L);
        when(atencionRepository.count()).thenReturn(4L);
        when(pagoRepository.sumarPorEstado("PAGADO")).thenReturn(java.math.BigDecimal.valueOf(250));
        when(pagoRepository.resumirIngresosPorMes(
                anyString(),
                any(LocalDateTime.class),
                any(LocalDateTime.class))).thenReturn(List.of());

        ReporteController controller = new ReporteController(
                pacienteRepository,
                medicoRepository,
                citaRepository,
                atencionRepository,
                pagoRepository);

        var response = controller.exportarExcel();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(
                "attachment; filename=\"reporte-sismed.xlsx\"",
                response.getHeaders().getFirst("Content-Disposition"));

        try (XSSFWorkbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(response.getBody()))) {
            assertEquals(2, workbook.getNumberOfSheets());
            assertEquals("Resumen", workbook.getSheetAt(0).getSheetName());
            assertEquals("Pacientes registrados",
                    workbook.getSheetAt(0).getRow(2).getCell(0).getStringCellValue());
            assertEquals(3, workbook.getSheetAt(0).getRow(2).getCell(1).getNumericCellValue());
            assertEquals("Ingresos mensuales", workbook.getSheetAt(1).getSheetName());
            assertEquals(13, workbook.getSheetAt(1).getPhysicalNumberOfRows());
        }
    }
}
