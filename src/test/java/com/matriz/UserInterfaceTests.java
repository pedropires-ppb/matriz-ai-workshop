package com.matriz;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Clock;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end: a browser uses the User interface served by the running application (ADR-007
 * decisions 2 and 3). It signs in, sees the display name, signs out, and sees what the User
 * interface shows when a session ends (US-013–US-015, EX-014; ADR-009 decision 1). Chromium only
 * until OQ-021 is answered; the build installs it beforehand.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserInterfaceTests extends DatabaseTestSupport {

	private static final String SIGN_IN_PATH = "/api/session";

	/** 2026-11-02T09:00:00Z, a fictitious time at which the page's clock starts. */
	private static final long CLOCK_START = 1_793_610_000_000L;

	/** Real time given to the page to show a change that the test does not expect. */
	private static final double SETTLING_TIME_MILLIS = 500;

	/** What Chromium writes to the console for each answer 401, which the tests expect. */
	private static final String UNAUTHORIZED_RESOURCE = "the server responded with a status of 401";

	@LocalServerPort
	private int port;

	@Autowired
	private JdbcClient jdbc;

	private Playwright playwright;

	private Browser browser;

	@BeforeAll
	void launchTheBrowser() {
		this.playwright = Playwright
			.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
		this.browser = this.playwright.chromium().launch();
	}

	@AfterAll
	void closeTheBrowser() {
		this.browser.close();
		this.playwright.close();
	}

	@Test
	void theApplicationServesTheUserInterfaceShell() {
		inATab((tab) -> {
			tab.open();

			PlaywrightAssertions.assertThat(tab.page()).hasTitle("Matriz");
			assertThat(tab.page().locator("h1")).hasText("Matriz");
		});
	}

	@Test
	void aPersonWhoHasNotSignedInIsSentToSignIn() {
		inATab((tab) -> {
			tab.open();

			tab.assertSignInScreen();
			assertThat(tab.page().getByRole(AriaRole.STATUS)).hasCount(0);
			assertThat(tab.page().getByRole(AriaRole.ALERT)).hasCount(0);
			assertThat(tab.requests()).isEmpty();
		});
	}

	@Test
	void signingInShowsTheDisplayNameAndSigningOutAsksToSignInAgain() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		inATab((tab) -> {
			tab.open();
			String token = tab.signIn(ana);

			assertThat(tab.page().getByText("Signed in as Ana Silva", new Page.GetByTextOptions().setExact(true)))
				.isVisible();
			assertThat(tab.heading("My projects")).isVisible();
			assertThat(tab.page().getByRole(AriaRole.FORM)).hasCount(0);
			assertThat(tab.page().evaluate("() => [localStorage.length, sessionStorage.length, document.cookie]"))
				.as("the token is kept only in memory")
				.isEqualTo(List.of(0, 0, ""));

			Response signOut = tab.page()
				.waitForResponse((response) -> response.url().endsWith(SIGN_IN_PATH)
						&& "DELETE".equals(response.request().method()), () -> tab.button("Sign out").click());
			assertThat(signOut.status()).isEqualTo(204);
			assertThat(tab.page().getByRole(AriaRole.STATUS)).hasText("You have signed out.");
			tab.assertSignInScreen();
			assertThat(apiStatus("GET", "/api/me", token)).as("the ended token is refused").isEqualTo(401);
		});
	}

	@Test
	void reloadingThePageSignsThePersonOutWithoutAMessage() {
		TestAccess.TestPerson rui = TestAccess.addPerson(this.jdbc, "Rui Martins");
		inATab((tab) -> {
			tab.open();
			tab.signIn(rui);
			assertThat(tab.heading("My projects")).isVisible();

			tab.page().reload();

			tab.assertSignInScreen();
			assertThat(tab.page().getByRole(AriaRole.STATUS)).hasCount(0);
		});
	}

	@Test
	void incorrectCredentialsGetOneMessageThatDoesNotSayWhichIsWrong() {
		TestAccess.TestPerson rui = TestAccess.addPerson(this.jdbc, "Rui Martins");
		inATab((tab) -> {
			tab.open();

			tab.submit(rui.loginName(), rui.password() + "-wrong");
			assertThat(tab.page().getByRole(AriaRole.ALERT)).hasText("The login name or password is incorrect.");
			assertThat(tab.page().getByLabel("Password")).isEmpty();

			tab.submit("test-nobody", rui.password());
			assertThat(tab.page().getByRole(AriaRole.ALERT)).hasText("The login name or password is incorrect.");
			tab.assertSignInScreen();
		}, UNAUTHORIZED_RESOURCE);
	}

	@Test
	void anEmptyLoginNameOrPasswordIsRefusedWithoutARequest() {
		inATab((tab) -> {
			tab.open();

			tab.button("Sign in").click();
			assertThat(tab.page().getByRole(AriaRole.ALERT)).hasText("A login name and a password are required.");
			tab.submit("ana", "");
			assertThat(tab.page().getByRole(AriaRole.ALERT)).hasText("A login name and a password are required.");
			tab.submit("", "fictitious-password");
			assertThat(tab.page().getByRole(AriaRole.ALERT)).hasText("A login name and a password are required.");

			assertThat(tab.requests()).isEmpty();
		});
	}

	@Test
	void whenMatrizCannotBeReachedThePersonIsToldSo() {
		TestAccess.TestPerson rui = TestAccess.addPerson(this.jdbc, "Rui Martins");
		inATab((tab) -> {
			tab.open();
			tab.page().route("**" + SIGN_IN_PATH, (route) -> route.abort());

			tab.submit(rui.loginName(), rui.password());

			assertThat(tab.page().getByRole(AriaRole.ALERT))
				.hasText("Matriz could not be reached. Please try again.");
			tab.assertSignInScreen();
		}, "net::ERR_FAILED");
	}

	@Test
	void aSessionEndedElsewhereShowsTheMessageWhenThePageIsVisibleAgain() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		inATab((tab) -> {
			tab.open();
			String token = tab.signIn(ana);
			assertThat(tab.heading("My projects")).isVisible();

			assertThat(apiStatus("DELETE", SIGN_IN_PATH, token)).as("the session ends outside the tab").isEqualTo(204);
			Response check = tab.becomeVisible();

			assertThat(check.status()).isEqualTo(401);
			assertThat(tab.page().getByRole(AriaRole.STATUS))
				.hasText("Your session has ended. Please sign in again.");
			tab.assertSignInScreen();
		}, UNAUTHORIZED_RESOURCE);
	}

	@Test
	void anExpiredSessionShowsTheMessageWithoutWaitingForARequest() {
		TestAccess.TestPerson eva = TestAccess.addPerson(this.jdbc, "Eva Santos");
		inATab((tab) -> {
			Clock clock = tab.page().clock();
			clock.install(new Clock.InstallOptions().setTime(CLOCK_START));
			tab.open();
			// From here on, the page's time moves only when the test moves it.
			clock.pauseAt(CLOCK_START + Duration.ofMinutes(1).toMillis());
			tab.signIn(eva);
			int requestsWhenSignedIn = tab.requests().size();

			clock.runFor(TestAccess.LIFETIME.toMillis() - 1);
			tab.page().waitForTimeout(SETTLING_TIME_MILLIS);
			// Still signed in one millisecond before the expiry.
			assertThat(tab.heading("My projects")).isVisible();
			assertThat(tab.page().getByRole(AriaRole.STATUS)).hasCount(0);

			clock.runFor(1);
			assertThat(tab.page().getByRole(AriaRole.STATUS))
				.hasText("Your session has ended. Please sign in again.");
			tab.assertSignInScreen();
			assertThat(tab.requests()).hasSize(requestsWhenSignedIn);
		});
	}

	@Test
	void twoPeopleEachSeeThemselvesAndOneSigningOutLeavesTheOtherSignedIn() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		TestAccess.TestPerson eva = TestAccess.addPerson(this.jdbc, "Eva Santos");
		inATab((anasTab) -> inATab((evasTab) -> {
			anasTab.open();
			evasTab.open();
			anasTab.signIn(ana);
			evasTab.signIn(eva);
			assertThat(anasTab.page().getByText("Signed in as Ana Silva")).isVisible();
			assertThat(evasTab.page().getByText("Signed in as Eva Santos")).isVisible();
			assertThat(evasTab.page().getByText("Ana Silva")).hasCount(0);

			anasTab.button("Sign out").click();
			assertThat(anasTab.page().getByRole(AriaRole.STATUS)).hasText("You have signed out.");
			Response check = evasTab.becomeVisible();

			assertThat(check.status()).isEqualTo(200);
			assertThat(JsonMapper.shared().readTree(check.text()).path("displayName").stringValue())
				.isEqualTo("Eva Santos");
			assertThat(evasTab.page().getByText("Signed in as Eva Santos")).isVisible();
			assertThat(evasTab.heading("My projects")).isVisible();
		}));
	}

	/** Sends a direct API request, outside the User interface, and returns its status. */
	private int apiStatus(String method, String path, String token) {
		APIRequestContext api = this.playwright.request()
			.newContext(new APIRequest.NewContextOptions().setBaseURL(baseUrl()));
		try {
			return api
				.fetch(path,
						RequestOptions.create()
							.setMethod(method)
							.setHeader(HttpHeaders.AUTHORIZATION, TestAccess.bearer(token)))
				.status();
		}
		finally {
			api.dispose();
		}
	}

	private String baseUrl() {
		return "http://localhost:" + this.port;
	}

	/**
	 * Runs the steps in a new browser context, whose memory no other test shares, and then checks
	 * that the page had no error and logged none but the expected ones.
	 */
	private void inATab(Consumer<Tab> steps, String... expectedConsoleErrors) {
		try (BrowserContext context = this.browser.newContext()) {
			Tab tab = new Tab(context.newPage(), baseUrl());
			steps.accept(tab);
			assertThat(tab.pageErrors()).isEmpty();
			assertThat(tab.consoleErrors())
				.allSatisfy((error) -> assertThat(error).containsAnyOf(expectedConsoleErrors.length > 0
						? expectedConsoleErrors : new String[] { "no console error is expected" }));
		}
	}

	/** A browser tab with the User interface, and what it requested and reported. */
	private record Tab(Page page, String baseUrl, List<String> requests, List<String> pageErrors,
			List<String> consoleErrors) {

		Tab(Page page, String baseUrl) {
			this(page, baseUrl, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
			page.onRequest((request) -> {
				if (request.url().contains("/api/")) {
					this.requests.add(request.method() + " " + request.url());
				}
			});
			page.onPageError(this.pageErrors::add);
			page.onConsoleMessage((message) -> {
				if ("error".equals(message.type())) {
					this.consoleErrors.add(message.text());
				}
			});
		}

		void open() {
			this.page.navigate(this.baseUrl + "/");
		}

		/** Signs in through the form, and returns the access token that the browser received. */
		String signIn(TestAccess.TestPerson person) {
			Response response = this.page.waitForResponse(
					(candidate) -> candidate.url().endsWith(SIGN_IN_PATH)
							&& "POST".equals(candidate.request().method()),
					() -> submit(person.loginName(), person.password()));
			assertThat(response.status()).isEqualTo(200);
			assertThat(heading("My projects")).isVisible();
			return JsonMapper.shared().readTree(response.text()).path("accessToken").stringValue();
		}

		void submit(String loginName, String password) {
			this.page.getByLabel("Login name").fill(loginName);
			this.page.getByLabel("Password").fill(password);
			button("Sign in").click();
		}

		/** Tells the page that it is visible again, and returns the answer to its check of the token. */
		Response becomeVisible() {
			return this.page.waitForResponse((response) -> response.url().endsWith("/api/me"),
					() -> this.page.evaluate("() => document.dispatchEvent(new Event('visibilitychange'))"));
		}

		void assertSignInScreen() {
			assertThat(heading("Sign in")).isVisible();
			assertThat(this.page.getByLabel("Login name")).isVisible();
			assertThat(this.page.getByLabel("Password")).isVisible();
			assertThat(button("Sign in")).isVisible();
			assertThat(heading("My projects")).hasCount(0);
			assertThat(this.page.getByText("Signed in as")).hasCount(0);
			assertThat(button("Sign out")).hasCount(0);
		}

		Locator heading(String name) {
			return this.page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(name).setExact(true));
		}

		Locator button(String name) {
			return this.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
		}

	}

}
