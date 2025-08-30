package com.example.barber.automation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
	"whatsapp.webhook-verify-token=test-verify-token",
	"whatsapp.api.token=test-token"
})
class BarberAutomationApplicationTests {

	@Test
	void contextLoads() {
	}

}
