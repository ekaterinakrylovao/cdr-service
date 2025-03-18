package com.example.cdrservice.controller;

import com.example.cdrservice.service.UdrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UdrControllerTest {

    @Mock
    private UdrService udrService;

    @InjectMocks
    private UdrController udrController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(udrController).build();
    }

    // Тесты для /udr/{msisdn}
    // Успешное получение отчёта
    @Test
    void testGetUdrReport_Success() throws Exception {
        String msisdn = "79991112233";
        String month = "2024-03";
        String expectedResponse = "{\"msisdn\": \"79991112233\", \"incomingCall\": " +
                "{\"totalTime\": \"00:10:00\"}, \"outcomingCall\": {\"totalTime\": \"00:05:00\"}}";

        when(udrService.generateUdrReport(msisdn, month)).thenReturn(expectedResponse);

        mockMvc.perform(get("/udr/{msisdn}", msisdn).param("month", month))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResponse));

        verify(udrService, times(1)).generateUdrReport(msisdn, month);
    }

    //Отсутствие записей
    @Test
    void testGetUdrReport_NoRecords() throws Exception {
        String msisdn = "79991112233";
        String month = "2024-03";
        String expectedResponse = "No records found for the specified MSISDN.";

        when(udrService.generateUdrReport(msisdn, month)).thenReturn(expectedResponse);

        mockMvc.perform(get("/udr/{msisdn}", msisdn).param("month", month))
                .andExpect(status().isNotFound())
                .andExpect(content().string(expectedResponse));

        verify(udrService, times(1)).generateUdrReport(msisdn, month);
    }

    @Test
    void testGetUdrReport_InvalidMonthFormat() throws Exception {
        String msisdn = "79991112233";
        String invalidMonth = "2024-13"; // Некорректный месяц

        mockMvc.perform(get("/udr/{msisdn}", msisdn).param("month", invalidMonth))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid month format. Expected format: YYYY-MM"));

        verify(udrService, never()).generateUdrReport(any(), any());
    }

    // Тесты для /udr/all
    // Успешное получение всех отчётов
    @Test
    void testGetAllUdrReports_Success() throws Exception {
        String month = "2024-03";
        String expectedResponse = "{\"msisdn\": \"79991112233\", \"incomingCall\": " +
                "{\"totalTime\": \"00:10:00\"}, \"outcomingCall\": {\"totalTime\": \"00:05:00\"}}\n" +
                "{\"msisdn\": \"79992221122\", \"incomingCall\": {\"totalTime\": \"00:05:00\"}, " +
                "\"outcomingCall\": {\"totalTime\": \"00:10:00\"}}";

        when(udrService.generateAllUdrReports(month)).thenReturn(expectedResponse);

        mockMvc.perform(get("/udr/all").param("month", month))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedResponse));

        verify(udrService, times(1)).generateAllUdrReports(month);
    }

    //Отсутствие записей
    @Test
    void testGetAllUdrReports_NoRecords() throws Exception {
        String month = "2024-03";
        String expectedResponse = "No records found for the specified period.";

        when(udrService.generateAllUdrReports(month)).thenReturn(expectedResponse);

        mockMvc.perform(get("/udr/all").param("month", month))
                .andExpect(status().isNotFound())
                .andExpect(content().string(expectedResponse));

        verify(udrService, times(1)).generateAllUdrReports(month);
    }

    @Test
    void testGetAllUdrReports_InvalidMonthFormat() throws Exception {
        String invalidMonth = "2024-13"; // Некорректный месяц

        mockMvc.perform(get("/udr/all").param("month", invalidMonth))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid month format. Expected format: YYYY-MM"));

        verify(udrService, never()).generateAllUdrReports(any());
    }

    // Тесты для /udr/cdr-report/{msisdn}
    // Успешная генерация отчёта
    @Test
    void testGenerateCdrReport_Success() throws Exception {
        String msisdn = "79991112233";
        String startDate = "2024-03-01T00:00:00";
        String endDate = "2024-03-31T23:59:59";
        String reportId = "123e4567-e89b-12d3-a456-426614174000";

        when(udrService.generateCdrReport(msisdn, startDate, endDate)).thenReturn(reportId);

        mockMvc.perform(get("/udr/cdr-report/{msisdn}", msisdn)
                        .param("startDate", startDate)
                        .param("endDate", endDate))
                .andExpect(status().isOk())
                .andExpect(content().string("Report generated with ID: " + reportId));

        verify(udrService, times(1)).generateCdrReport(msisdn, startDate, endDate);
    }

    // Ошибка при генерации отчёта
    @Test
    void testGenerateCdrReport_Error() throws Exception {
        String msisdn = "79991112233";
        String startDate = "2024-03-01T00:00:00";
        String endDate = "2024-03-31T23:59:59";
        String errorMessage = "No records found for the specified period.";

        when(udrService.generateCdrReport(msisdn, startDate, endDate)).thenThrow(new RuntimeException(errorMessage));

        mockMvc.perform(get("/udr/cdr-report/{msisdn}", msisdn)
                        .param("startDate", startDate)
                        .param("endDate", endDate))
                .andExpect(status().isNotFound())
                .andExpect(content().string(errorMessage));

        verify(udrService, times(1)).generateCdrReport(msisdn, startDate, endDate);
    }

    @Test
    void testGenerateCdrReport_InvalidStartDateFormat() throws Exception {
        String msisdn = "79991112233";
        String invalidStartDate = "2024-03-01 00:00:00"; // Некорректный формат
        String endDate = "2024-03-31T23:59:59";

        mockMvc.perform(get("/udr/cdr-report/{msisdn}", msisdn)
                        .param("startDate", invalidStartDate)
                        .param("endDate", endDate))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid startDate format. Expected format: YYYY-MM-DDTHH:mm:ss"));

        verify(udrService, never()).generateCdrReport(any(), any(), any());
    }

    @Test
    void testGenerateCdrReport_InvalidEndDateFormat() throws Exception {
        String msisdn = "79991112233";
        String startDate = "2024-03-01T00:00:00";
        String invalidEndDate = "2024-03-31 23:59:59"; // Некорректный формат

        mockMvc.perform(get("/udr/cdr-report/{msisdn}", msisdn)
                        .param("startDate", startDate)
                        .param("endDate", invalidEndDate))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid endDate format. Expected format: YYYY-MM-DDTHH:mm:ss"));

        verify(udrService, never()).generateCdrReport(any(), any(), any());
    }

    @Test
    void testGenerateCdrReport_StartDateAfterEndDate() throws Exception {
        String msisdn = "79991112233";
        String startDate = "2024-03-31T23:59:59";
        String endDate = "2024-03-01T00:00:00"; // startDate позже endDate

        mockMvc.perform(get("/udr/cdr-report/{msisdn}", msisdn)
                        .param("startDate", startDate)
                        .param("endDate", endDate))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("startDate must be before endDate"));

        verify(udrService, never()).generateCdrReport(any(), any(), any());
    }
}
