package com.matriz;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end: a browser loads the User interface shell from the running application (ADR-007
 * decisions 2 and 3). Chromium only until OQ-021 is answered; the build installs it beforehand.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class UserInterfaceTests extends DatabaseTestSupport {

	@LocalServerPort
	private int port;

	@Test
	void theApplicationServesTheUserInterfaceShell() {
		Playwright.CreateOptions options = new Playwright.CreateOptions()
			.setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1"));
		try (Playwright playwright = Playwright.create(options); Browser browser = playwright.chromium().launch()) {
			Page page = browser.newPage();
			List<String> errors = new ArrayList<>();
			page.onPageError(errors::add);
			page.onConsoleMessage((message) -> {
				if ("error".equals(message.type())) {
					errors.add(message.text());
				}
			});

			page.navigate("http://localhost:" + this.port + "/");

			PlaywrightAssertions.assertThat(page).hasTitle("Matriz");
			PlaywrightAssertions.assertThat(page.locator("h1")).hasText("Matriz");
			assertThat(errors).isEmpty();
		}
	}

}
