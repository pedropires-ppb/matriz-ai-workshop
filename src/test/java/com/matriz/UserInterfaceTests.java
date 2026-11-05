package com.matriz;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import com.microsoft.playwright.options.SelectOption;
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
			assertThat(tab.page().getByText("No projects yet.")).isVisible();
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

	@Test
	void signInCreateAProjectSeeItInTheListAndSeeItAgainAfterSigningInAfterAReload() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		inATab((tab) -> {
			tab.open();
			tab.signIn(ana);
			assertThat(tab.page().getByText("No projects yet.")).isVisible();

			tab.button("New project").click();
			tab.page().getByLabel("Name", exact()).fill("  Website Redesign ");
			tab.page().getByLabel("Description (optional)", exact()).fill("A new look");
			Response created = tab.page()
				.waitForResponse((response) -> response.url().endsWith("/api/projects")
						&& "POST".equals(response.request().method()), () -> tab.button("Create project").click());
			assertThat(created.status()).isEqualTo(201);
			assertThat(tab.heading("Website Redesign")).isVisible();
			assertThat(tab.page().getByText("A new look")).isVisible();

			tab.button("Back to My projects").click();
			assertThat(tab.button("Website Redesign")).isVisible();
			assertThat(tab.page().getByText("Active, Project Owner")).isVisible();

			tab.page().reload();
			tab.assertSignInScreen();
			tab.signIn(ana);
			assertThat(tab.button("Website Redesign")).isVisible();
			tab.button("Website Redesign").click();
			assertThat(tab.heading("Website Redesign")).isVisible();
			assertThat(tab.page().getByText("A new look")).isVisible();
		});
		assertThat(this.jdbc.sql("SELECT count(*) FROM membership WHERE person_id = ? AND role = 'PROJECT_OWNER'")
			.param(ana.id())
			.query(Integer.class)
			.single()).isEqualTo(1);
	}

	@Test
	void invalidNamesAndDescriptionsAreExplainedWithoutARequest() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		inATab((tab) -> {
			tab.open();
			tab.signIn(ana);
			tab.button("New project").click();
			int requestsBefore = tab.requests().size();

			tab.button("Create project").click();
			assertThat(tab.page().getByRole(AriaRole.ALERT)).hasText("Name is required.");
			tab.page().getByLabel("Name", exact()).fill("   ");
			tab.button("Create project").click();
			assertThat(tab.page().getByRole(AriaRole.ALERT)).hasText("Name is required.");
			tab.page().getByLabel("Name", exact()).fill("n".repeat(101));
			tab.button("Create project").click();
			assertThat(tab.page().getByRole(AriaRole.ALERT)).hasText("Name must be at most 100 characters.");
			tab.page().getByLabel("Name", exact()).fill("n".repeat(100));
			tab.page().getByLabel("Description (optional)", exact()).fill("d".repeat(1001));
			tab.button("Create project").click();
			assertThat(tab.page().getByRole(AriaRole.ALERT))
				.hasText("Description must be at most 1000 characters.");

			assertThat(tab.requests()).hasSize(requestsBefore);
			tab.page().getByLabel("Description (optional)", exact()).fill("d".repeat(1000));
			tab.button("Create project").click();
			assertThat(tab.heading("n".repeat(100))).isVisible();
		});
	}

	@Test
	void theProjectOwnerRenamesAProjectAndCanClearItsDescription() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		UUID project = addProject("Old name", "Old text", ana, "PROJECT_OWNER");
		inATab((tab) -> {
			tab.open();
			tab.signIn(ana);
			tab.button("Old name").click();

			tab.button("Edit project").click();
			tab.page().getByLabel("Name", exact()).fill("New name");
			tab.page().getByLabel("Description (optional)", exact()).fill("");
			tab.button("Save changes").click();

			assertThat(tab.heading("New name")).isVisible();
			assertThat(tab.page().getByRole(AriaRole.STATUS)).hasText("The project has been saved.");
			assertThat(tab.page().getByText("No description.")).isVisible();
		});
		assertThat(this.jdbc.sql("SELECT name FROM project WHERE project_id = ?")
			.param(project)
			.query(String.class)
			.single()).isEqualTo("New name");
	}

	@Test
	void deletingAsksForConfirmationAndCancellingChangesNothing() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		UUID project = addProject("Doomed", null, ana, "PROJECT_OWNER");
		inATab((tab) -> {
			tab.open();
			tab.signIn(ana);
			tab.button("Doomed").click();

			tab.button("Delete project").click();
			assertThat(tab.page().getByRole(AriaRole.ALERTDIALOG)).containsText(
					"Delete \"Doomed\"? Its tasks, comments and assignments will no longer be visible. "
							+ "This cannot be undone.");
			tab.button("Cancel").click();

			assertThat(tab.page().getByRole(AriaRole.ALERTDIALOG)).hasCount(0);
			assertThat(tab.heading("Doomed")).isVisible();
			assertThat(tab.requests().stream().filter((request) -> request.startsWith("DELETE /") && request.contains("/api/projects"))).isEmpty();
		});
		assertThat(isDeleted(project)).isFalse();
	}

	@Test
	void confirmingTheDeletionRemovesTheProjectFromTheListAndEverywhere() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		UUID project = addProject("Doomed", null, ana, "PROJECT_OWNER");
		addProject("Kept", null, ana, "PROJECT_OWNER");
		inATab((tab) -> {
			tab.open();
			String token = tab.signIn(ana);
			tab.button("Doomed").click();
			tab.button("Delete project").click();

			Response deleted = tab.page()
				.waitForResponse((response) -> "DELETE".equals(response.request().method())
						&& response.url().endsWith("/api/projects/" + project), () -> tab.button("Delete").click());

			assertThat(deleted.status()).isEqualTo(204);
			assertThat(tab.button("Kept")).isVisible();
			assertThat(tab.button("Doomed")).hasCount(0);
			assertThat(apiStatus("GET", "/api/projects/" + project, token)).isEqualTo(404);
		});
		assertThat(isDeleted(project)).isTrue();
	}

	@Test
	void aProjectParticipantSeesTheProjectButCannotEditOrDeleteIt() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		TestAccess.TestPerson rui = TestAccess.addPerson(this.jdbc, "Rui Martins");
		addProject("Shared", "Shared text", ana, "PROJECT_OWNER", rui);
		inATab((tab) -> {
			tab.open();
			tab.signIn(rui);
			tab.button("Shared").click();

			assertThat(tab.heading("Shared")).isVisible();
			assertThat(tab.page().getByText("Project Participant", new Page.GetByTextOptions().setExact(true)))
				.isVisible();
			assertThat(tab.button("Edit project")).hasCount(0);
			assertThat(tab.button("Delete project")).hasCount(0);
		});
	}

	@Test
	void everyMemberSeesTheMembersOfAProjectWithTheOwnerFirst() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		TestAccess.TestPerson rui = TestAccess.addPerson(this.jdbc, "Rui Martins");
		TestAccess.TestPerson eva = TestAccess.addPerson(this.jdbc, "Eva Santos");
		addProject("Shared", null, ana, "PROJECT_OWNER", rui, eva);
		inATab((tab) -> {
			tab.open();
			tab.signIn(rui);
			tab.button("Shared").click();

			assertThat(tab.members()).hasText(new String[] { "Ana Silva · Project Owner",
					"Eva Santos · Project Participant", "Rui Martins · Project Participant" });
			assertThat(tab.page().getByText(rui.loginName())).hasCount(0);
		});
	}

	@Test
	void theListIsPagedAndSearchableByName() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		for (int i = 1; i <= 25; i++) {
			addProject(String.format("Project %02d", i), null, ana, "PROJECT_OWNER");
		}
		addProject("Other", null, ana, "PROJECT_OWNER");
		inATab((tab) -> {
			tab.open();
			tab.signIn(ana);

			assertThat(tab.page().getByText("Page 1 of 2")).isVisible();
			assertThat(tab.button("Previous")).isDisabled();
			assertThat(tab.page().getByRole(AriaRole.LISTITEM)).hasCount(20);
			tab.button("Next").click();
			assertThat(tab.page().getByText("Page 2 of 2")).isVisible();
			assertThat(tab.page().getByRole(AriaRole.LISTITEM)).hasCount(6);
			assertThat(tab.button("Next")).isDisabled();

			tab.page().getByLabel("Search by name", exact()).fill("  PROJECT 2 ");
			tab.button("Search").click();
			assertThat(tab.page().getByText("Page 1 of 1")).hasCount(0);
			assertThat(tab.page().getByRole(AriaRole.LISTITEM)).hasCount(6);
			assertThat(tab.button("Project 25")).isVisible();
			assertThat(tab.button("Project 05")).hasCount(0);

			tab.page().getByLabel("Search by name", exact()).fill("nothing like this");
			tab.button("Search").click();
			assertThat(tab.page().getByText("No project matches your search.")).isVisible();

			tab.button("Clear search").click();
			assertThat(tab.page().getByText("Page 1 of 2")).isVisible();
		});
	}

	@Test
	void aSessionEndedElsewhereSendsThePersonToSignInWhenTheyUseTheProjects() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		addProject("Mine", null, ana, "PROJECT_OWNER");
		inATab((tab) -> {
			tab.open();
			String token = tab.signIn(ana);
			assertThat(tab.button("Mine")).isVisible();
			assertThat(apiStatus("DELETE", SIGN_IN_PATH, token)).isEqualTo(204);

			tab.button("Mine").click();

			assertThat(tab.page().getByRole(AriaRole.STATUS)).hasText("Your session has ended. Please sign in again.");
			tab.assertSignInScreen();
		}, UNAUTHORIZED_RESOURCE);
	}

	@Test
	void theProjectOwnerCreatesChangesFiltersAndDeletesTasksWithoutReloadingThePage() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		UUID project = addProject("Website Redesign", null, ana, "PROJECT_OWNER");
		inATab((tab) -> {
			tab.open();
			String token = tab.signIn(ana);
			tab.button("Website Redesign").click();
			assertThat(tab.heading("Tasks")).isVisible();
			assertThat(tab.page().getByText("This project has no tasks yet.")).isVisible();
			assertThat(tab.taskCount()).hasText("0 tasks");
			tab.page().evaluate("() => { window.sameDocument = true; }");

			tab.button("New task").click();
			tab.page().getByLabel("Title", exact()).fill("  Write the report ");
			tab.page().getByLabel("Description (optional)", exact()).fill("Quarterly numbers");
			tab.page().getByLabel("Priority", exact()).selectOption("HIGH");
			Response created = tab.page()
				.waitForResponse((response) -> response.url().endsWith("/tasks")
						&& "POST".equals(response.request().method()), () -> tab.button("Create task").click());
			assertThat(created.status()).isEqualTo(201);
			assertThat(tab.page().getByText("The task has been created.")).isVisible();
			assertThat(tab.tasks()).hasCount(1);
			assertThat(tab.tasks().first()).containsText("Write the report");
			assertThat(tab.tasks().first()).containsText("To do");
			assertThat(tab.tasks().first()).containsText("High");
			assertThat(tab.taskCount()).hasText("1 task");
			assertThat(tab.page().getByText("Quarterly numbers")).isVisible();

			tab.button("New task").click();
			tab.page().getByLabel("Title", exact()).fill("Review the report");
			tab.button("Create task").click();
			assertThat(tab.tasks()).hasCount(2);
			assertThat(tab.taskCount()).hasText("2 tasks");
			assertThat(tab.tasks().last()).containsText("Medium");

			tab.button("Write the report").click();
			Response stateChanged = tab.page()
				.waitForResponse((response) -> response.url().endsWith("/state")
						&& "PUT".equals(response.request().method()),
						() -> tab.page().getByLabel("State", exact()).selectOption("IN_PROGRESS"));
			assertThat(stateChanged.status()).isEqualTo(200);
			assertThat(tab.tasks().first()).containsText("In progress");
			Response priorityChanged = tab.page()
				.waitForResponse((response) -> "PATCH".equals(response.request().method()),
						() -> tab.page().getByLabel("Priority", exact()).selectOption("LOW"));
			assertThat(priorityChanged.status()).isEqualTo(200);
			assertThat(tab.tasks().first()).containsText("Low");

			tab.filter("Filter by state", "Done").check();
			assertThat(tab.page().getByText("No task matches the filters.")).isVisible();
			assertThat(tab.taskCount()).hasText("0 tasks");
			tab.filter("Filter by state", "Done").uncheck();
			tab.filter("Filter by state", "In progress").check();
			assertThat(tab.tasks()).hasCount(1);
			assertThat(tab.taskCount()).hasText("1 task");
			tab.filter("Filter by state", "To do").check();
			assertThat(tab.tasks()).hasCount(2);
			tab.filter("Filter by priority", "Medium").check();
			assertThat(tab.tasks()).hasCount(1);
			assertThat(tab.tasks().first()).containsText("Review the report");
			tab.button("Clear filters").click();
			assertThat(tab.tasks()).hasCount(2);

			tab.button("Write the report").click();
			tab.button("Delete task").click();
			assertThat(tab.page().getByRole(AriaRole.ALERTDIALOG))
				.containsText("Delete \"Write the report\"? Its assignments will be removed.");
			tab.button("Cancel").click();
			assertThat(tab.page().getByRole(AriaRole.ALERTDIALOG)).hasCount(0);
			assertThat(tab.tasks()).hasCount(2);
			assertThat(tab.requests().stream().filter((request) -> request.startsWith("DELETE ") && request.contains("/tasks/")))
				.isEmpty();

			tab.button("Delete task").click();
			Response deleted = tab.page()
				.waitForResponse((response) -> "DELETE".equals(response.request().method())
						&& response.url().contains("/tasks/"), () -> tab.button("Delete").click());
			assertThat(deleted.status()).isEqualTo(204);
			assertThat(tab.page().getByText("The task has been deleted.")).isVisible();
			assertThat(tab.tasks()).hasCount(1);
			assertThat(tab.taskCount()).hasText("1 task");
			assertThat(tab.button("Write the report")).hasCount(0);
			assertThat(apiStatus("GET", "/api/projects/" + project + "/tasks?state=TO_DO", token)).isEqualTo(200);

			assertThat(tab.page().evaluate("() => window.sameDocument")).isEqualTo(true);
		});
		assertThat(this.jdbc.sql("SELECT count(*) FROM task WHERE project_id = ? AND deleted_at IS NULL")
			.param(project)
			.query(Integer.class)
			.single()).isEqualTo(1);
		assertThat(this.jdbc.sql("SELECT count(*) FROM task WHERE project_id = ? AND deleted_at IS NOT NULL")
			.param(project)
			.query(Integer.class)
			.single()).isEqualTo(1);
	}

	@Test
	void aTaskCanBeEditedAndItsDescriptionCleared() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		UUID project = addProject("Website Redesign", null, ana, "PROJECT_OWNER");
		addTask(project, "Old title", "TO_DO", "MEDIUM");
		this.jdbc.sql("UPDATE task SET description = 'Old text' WHERE project_id = ?").param(project).update();
		inATab((tab) -> {
			tab.open();
			tab.signIn(ana);
			tab.button("Website Redesign").click();
			tab.button("Old title").click();
			assertThat(tab.page().getByText("Old text")).isVisible();

			tab.button("Edit task").click();
			tab.page().getByLabel("Title", exact()).fill("New title");
			tab.page().getByLabel("Description (optional)", exact()).fill("");
			tab.button("Save changes").click();

			assertThat(tab.page().getByText("The task has been saved.")).isVisible();
			assertThat(tab.heading("New title")).isVisible();
			assertThat(tab.page().locator("section[aria-labelledby='task-heading']")).containsText("No description.");
			assertThat(tab.tasks().first()).containsText("New title");
		});
	}

	@Test
	void invalidTitlesAndDescriptionsAreExplainedWithoutARequest() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		UUID project = addProject("Website Redesign", null, ana, "PROJECT_OWNER");
		inATab((tab) -> {
			tab.open();
			tab.signIn(ana);
			tab.button("Website Redesign").click();
			tab.button("New task").click();
			long requests = tab.requests().size();

			tab.button("Create task").click();
			assertThat(tab.page().getByRole(AriaRole.ALERT)).hasText("A title is required.");
			tab.page().getByLabel("Title", exact()).fill("   ");
			tab.button("Create task").click();
			assertThat(tab.page().getByRole(AriaRole.ALERT)).hasText("A title is required.");
			tab.page().getByLabel("Title", exact()).fill("\u00A0\u00A0");
			tab.button("Create task").click();
			assertThat(tab.page().getByRole(AriaRole.ALERT)).hasText("A title is required.");
			tab.page().getByLabel("Title", exact()).fill("a".repeat(151));
			tab.button("Create task").click();
			assertThat(tab.page().getByRole(AriaRole.ALERT)).hasText("The title must have at most 150 characters.");
			tab.page().getByLabel("Title", exact()).fill("a".repeat(150));
			tab.page().getByLabel("Description (optional)", exact()).fill("d".repeat(2001));
			tab.button("Create task").click();
			assertThat(tab.page().getByRole(AriaRole.ALERT))
				.hasText("The description must have at most 2000 characters.");
			assertThat(tab.requests()).hasSize((int) requests);

			tab.page().getByLabel("Description (optional)", exact()).fill("d".repeat(2000));
			tab.button("Create task").click();
			assertThat(tab.tasks()).hasCount(1);
		});
		assertThat(this.jdbc.sql("SELECT count(*) FROM task WHERE project_id = ?")
			.param(project)
			.query(Integer.class)
			.single()).isEqualTo(1);
	}

	@Test
	void aProjectParticipantSeesTheTasksButCannotChangeThem() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		TestAccess.TestPerson rui = TestAccess.addPerson(this.jdbc, "Rui Martins");
		UUID project = addProject("Shared", null, ana, "PROJECT_OWNER", rui);
		addTask(project, "Visible task", "DONE", "HIGH");
		inATab((tab) -> {
			tab.open();
			tab.signIn(rui);
			tab.button("Shared").click();

			assertThat(tab.tasks()).hasCount(1);
			assertThat(tab.page()
				.getByText("Only the Project Owner can create, edit, or delete tasks. The Project Owner or an assignee can change a task’s state."))
				.isVisible();
			assertThat(tab.button("New task")).hasCount(0);
			tab.button("Visible task").click();
			assertThat(tab.page().getByText("Done", new Page.GetByTextOptions().setExact(true)).first()).isVisible();
			assertThat(tab.button("Edit task")).hasCount(0);
			assertThat(tab.button("Delete task")).hasCount(0);
			assertThat(tab.page().getByLabel("State", exact())).hasCount(0);
		});
	}

	@Test
	void theProjectOwnerAssignsSeveralMembersAndRemovesEachOneWithoutReloadingThePage() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		TestAccess.TestPerson rui = TestAccess.addPerson(this.jdbc, "Rui Martins");
		TestAccess.TestPerson eva = TestAccess.addPerson(this.jdbc, "Eva Costa");
		TestAccess.addPerson(this.jdbc, "Daniel Reis");
		UUID project = addProject("Website Redesign", null, ana, "PROJECT_OWNER", rui, eva);
		addTask(project, "Write the report", "TO_DO", "MEDIUM");
		inATab((tab) -> {
			tab.open();
			tab.signIn(ana);
			tab.button("Website Redesign").click();
			tab.button("Write the report").click();
			tab.page().evaluate("() => { window.sameDocument = true; }");
			assertThat(tab.page().getByText("No one is assigned.")).isVisible();
			assertThat(tab.page().getByLabel("Person to assign", exact()).locator("option"))
				.hasText(new String[] { "Choose a member", "Ana Silva", "Eva Costa", "Rui Martins" });
			assertThat(tab.button("Assign")).isDisabled();

			tab.page().getByLabel("Person to assign", exact()).selectOption(new SelectOption().setLabel("Rui Martins"));
			Response assigned = tab.page()
				.waitForResponse((response) -> response.url().endsWith("/assignees")
						&& "POST".equals(response.request().method()), () -> tab.button("Assign").click());
			assertThat(assigned.status()).isEqualTo(200);
			assertThat(tab.page().getByText("The assignment has been saved.")).isVisible();
			tab.page().getByLabel("Person to assign", exact()).selectOption(new SelectOption().setLabel("Eva Costa"));
			tab.button("Assign").click();

			Locator assignees = tab.page()
				.getByRole(AriaRole.LIST, new Page.GetByRoleOptions().setName("Assigned to").setExact(true))
				.getByRole(AriaRole.LISTITEM);
			assertThat(assignees).hasCount(2);
			assertThat(assignees.first()).containsText("Eva Costa");
			assertThat(assignees.last()).containsText("Rui Martins");
			assertThat(tab.page().getByLabel("Person to assign", exact()).locator("option"))
				.hasText(new String[] { "Choose a member", "Ana Silva" });

			Response removed = tab.page()
				.waitForResponse((response) -> response.url().contains("/assignees/")
						&& "DELETE".equals(response.request().method()),
						() -> tab.button("Remove Eva Costa").click());
			assertThat(removed.status()).isEqualTo(200);
			assertThat(tab.page().getByText("The assignment has been removed.")).isVisible();
			assertThat(assignees).hasCount(1);
			assertThat(assignees.first()).containsText("Rui Martins");

			assertThat(tab.page().evaluate("() => window.sameDocument")).isEqualTo(true);
		});
		assertThat(this.jdbc.sql("SELECT count(*) FROM assignment").query(Integer.class).single()).isEqualTo(1);
	}

	@Test
	void aProjectParticipantAssignsThemselvesChangesTheStateAndUnassignsThemselves() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		TestAccess.TestPerson rui = TestAccess.addPerson(this.jdbc, "Rui Martins");
		UUID project = addProject("Shared", null, ana, "PROJECT_OWNER", rui);
		addTask(project, "Visible task", "TO_DO", "HIGH");
		inATab((tab) -> {
			tab.open();
			tab.signIn(rui);
			tab.button("Shared").click();
			tab.button("Visible task").click();
			tab.page().evaluate("() => { window.sameDocument = true; }");
			assertThat(tab.page().getByLabel("State", exact())).hasCount(0);
			assertThat(tab.page().getByLabel("Person to assign", exact())).hasCount(0);

			tab.button("Assign me").click();
			assertThat(tab.page().getByText("The assignment has been saved.")).isVisible();
			assertThat(tab.button("Assign me")).hasCount(0);
			assertThat(tab.button("Unassign me")).isVisible();
			assertThat(tab.page().getByLabel("State", exact())).isEnabled();
			assertThat(tab.page().getByLabel("Priority", exact())).isDisabled();
			tab.page().getByLabel("State", exact()).selectOption("IN_PROGRESS");
			assertThat(tab.tasks().first()).containsText("In progress");

			tab.button("Unassign me").click();
			assertThat(tab.page().getByText("The assignment has been removed.")).isVisible();
			assertThat(tab.button("Assign me")).isVisible();
			assertThat(tab.page().getByLabel("State", exact())).hasCount(0);

			assertThat(tab.page().evaluate("() => window.sameDocument")).isEqualTo(true);
		});
	}

	@Test
	void aCompletedProjectOffersNoAssignmentControls() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		TestAccess.TestPerson rui = TestAccess.addPerson(this.jdbc, "Rui Martins");
		UUID project = addProject("Finished", null, ana, "PROJECT_OWNER", rui);
		addTask(project, "Old task", "DONE", "LOW");
		this.jdbc.sql("UPDATE project SET state = 'COMPLETED' WHERE project_id = ?").param(project).update();
		inATab((tab) -> {
			tab.open();
			tab.signIn(ana);
			tab.button("Finished").click();
			tab.button("Old task").click();

			assertThat(tab.page().getByText("No one is assigned.")).isVisible();
			assertThat(tab.page().getByLabel("Person to assign", exact())).hasCount(0);
			assertThat(tab.button("Assign me")).hasCount(0);
			assertThat(tab.page().getByLabel("State", exact())).hasCount(0);
		});
	}

	@Test
	void aProjectsTasksArePagedAndTheLastPageIsLeftWhenItsOnlyTaskIsDeleted() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		UUID project = addProject("Busy", null, ana, "PROJECT_OWNER");
		for (int i = 1; i <= 21; i++) {
			addTask(project, String.format("Task %02d", i), "TO_DO", "MEDIUM");
		}
		inATab((tab) -> {
			tab.open();
			tab.signIn(ana);
			tab.button("Busy").click();

			assertThat(tab.taskCount()).hasText("21 tasks");
			assertThat(tab.page().getByText("Page 1 of 2")).isVisible();
			assertThat(tab.tasks()).hasCount(20);
			assertThat(tab.button("Previous")).isDisabled();
			tab.button("Next").click();
			assertThat(tab.page().getByText("Page 2 of 2")).isVisible();
			assertThat(tab.tasks()).hasCount(1);
			assertThat(tab.button("Task 21")).isVisible();
			assertThat(tab.button("Next")).isDisabled();

			tab.button("Task 21").click();
			tab.button("Delete task").click();
			tab.button("Delete").click();

			assertThat(tab.taskCount()).hasText("20 tasks");
			assertThat(tab.tasks()).hasCount(20);
			assertThat(tab.page().getByText("Page 1 of 2")).hasCount(0);
			assertThat(tab.button("Previous")).hasCount(0);
		});
	}

	@Test
	void aSessionEndedElsewhereSendsThePersonToSignInWhenTheyUseTheTasks() {
		TestAccess.TestPerson ana = TestAccess.addPerson(this.jdbc, "Ana Silva");
		UUID project = addProject("Mine", null, ana, "PROJECT_OWNER");
		addTask(project, "A task", "TO_DO", "MEDIUM");
		inATab((tab) -> {
			tab.open();
			String token = tab.signIn(ana);
			tab.button("Mine").click();
			assertThat(tab.tasks()).hasCount(1);
			assertThat(apiStatus("DELETE", SIGN_IN_PATH, token)).isEqualTo(204);

			tab.filter("Filter by state", "Done").check();

			assertThat(tab.page().getByRole(AriaRole.STATUS)).hasText("Your session has ended. Please sign in again.");
			tab.assertSignInScreen();
		}, UNAUTHORIZED_RESOURCE);
	}

	private void addTask(UUID project, String title, String state, String priority) {
		this.jdbc.sql("INSERT INTO task (task_id, project_id, title, state, priority) VALUES (?, ?, ?, ?, ?)")
			.params(UUID.randomUUID(), project, title, state, priority)
			.update();
	}

	private static Page.GetByLabelOptions exact() {
		return new Page.GetByLabelOptions().setExact(true);
	}

	private UUID addProject(String name, String description, TestAccess.TestPerson owner, String ownerRole,
			TestAccess.TestPerson... participants) {
		UUID id = UUID.randomUUID();
		this.jdbc.sql("INSERT INTO project (project_id, name, description) VALUES (?, ?, ?)")
			.params(id, name, description)
			.update();
		this.jdbc.sql("INSERT INTO membership (project_id, person_id, role) VALUES (?, ?, ?)")
			.params(id, owner.id(), ownerRole)
			.update();
		for (TestAccess.TestPerson participant : participants) {
			this.jdbc.sql("INSERT INTO membership (project_id, person_id, role) VALUES (?, ?, 'PROJECT_PARTICIPANT')")
				.params(id, participant.id())
				.update();
		}
		return id;
	}

	private boolean isDeleted(UUID project) {
		return this.jdbc.sql("SELECT count(*) FROM project WHERE project_id = ? AND deleted_at IS NOT NULL")
			.param(project)
			.query(Integer.class)
			.single() == 1;
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

		/** The rows of the list of tasks. */
		Locator tasks() {
			return this.page
				.getByRole(AriaRole.LIST, new Page.GetByRoleOptions().setName("Tasks").setExact(true))
				.getByRole(AriaRole.LISTITEM);
		}

		/** The rows of the list of members. */
		Locator members() {
			return this.page
				.getByRole(AriaRole.LIST, new Page.GetByRoleOptions().setName("Members").setExact(true))
				.getByRole(AriaRole.LISTITEM);
		}

		Locator taskCount() {
			return this.page.locator("#task-count");
		}

		/** One checkbox of a group of filters. */
		Locator filter(String group, String label) {
			return this.page
				.getByRole(AriaRole.GROUP, new Page.GetByRoleOptions().setName(group).setExact(true))
				.getByRole(AriaRole.CHECKBOX, new Locator.GetByRoleOptions().setName(label).setExact(true));
		}

	}

}
