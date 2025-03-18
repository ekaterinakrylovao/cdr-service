package com.example.cdrservice;

import com.example.cdrservice.controller.UdrController;
import com.example.cdrservice.service.UdrService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CdrServiceApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private UdrController udrController;

	@Autowired
	private UdrService udrService;

	/**
	 * Проверяет, что контекст Spring загружается без ошибок.
	 */
	@Test
	void contextLoads() {
		assertThat(udrController).isNotNull();
		assertThat(udrService).isNotNull();
	}

	/**
	 * Проверяет работу эндпоинта /udr/{msisdn}.
	 */
	@Test
	void testUdrReportEndpoint() {
		String msisdn = "79991112233";
		String month = "2024-03";

		ResponseEntity<String> response = restTemplate.getForEntity("/udr/" + msisdn + "?month=" + month, String.class);

		// Проверяем, что ответ успешный и содержит ожидаемые данные
		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(response.getBody()).isNotEmpty();
	}

	/**
	 * Проверяет работу эндпоинта /udr/all.
	 */
	@Test
	void testAllUdrReportsEndpoint() {
		String month = "2024-03";

		ResponseEntity<String> response = restTemplate.getForEntity("/udr/all?month=" + month, String.class);

		// Проверяем, что ответ успешный и содержит ожидаемые данные
		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(response.getBody()).isNotEmpty();
	}

	/**
	 * Проверяет работу эндпоинта /udr/cdr-report/{msisdn}.
	 */
	@Test
	void testGenerateCdrReportEndpoint() {
		String msisdn = "79991112233";
		String startDate = "2024-03-01T00:00:00";
		String endDate = "2024-03-31T23:59:59";

		ResponseEntity<String> response = restTemplate.getForEntity(
				"/udr/cdr-report/" + msisdn + "?startDate=" + startDate + "&endDate=" + endDate,
				String.class
		);

		// Проверяем, что ответ успешный и содержит ожидаемые данные
		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(response.getBody()).contains("Report generated with ID:");
	}
}
