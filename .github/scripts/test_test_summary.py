import importlib.util
import tempfile
import unittest
from pathlib import Path


SPEC = importlib.util.spec_from_file_location(
    "test_summary", Path(__file__).with_name("test_summary.py")
)
SUMMARY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SUMMARY)


class TestSummaryTests(unittest.TestCase):
    def test_lists_real_cases_and_counts_without_copying_failure_messages(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            reports = root / "target/test-results/frontend"
            reports.mkdir(parents=True)
            (reports / "junit.xml").write_text(
                '<testsuites><testsuite name="validation.test.ts">'
                '<testcase name="accepts &lt;valid&gt; text"/>'
                '<testcase name="rejects input"><failure>private diagnostic</failure></testcase>'
                '<testcase name="unsupported"><skipped/></testcase>'
                '</testsuite></testsuites>',
                encoding="utf-8",
            )
            text = SUMMARY.render(root)
            self.assertIn("Frontend unit tests", text)
            self.assertIn("| validation.test.ts | 1 | 1 | 1 | 3 |", text)
            self.assertIn("accepts &lt;valid&gt; text", text)
            self.assertIn("FAIL", text)
            self.assertIn("SKIP", text)
            self.assertNotIn("private diagnostic", text)

    def test_missing_reports_are_not_presented_as_passing_tests(self):
        with tempfile.TemporaryDirectory() as directory:
            text = SUMMARY.render(Path(directory))
            self.assertIn("No report produced", text)
            self.assertNotIn("| Total | 0 | 0 | 0 | 0 |", text)

    def test_coverage_rejects_an_omitted_java_test_and_accepts_all_groups(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            sources = root / "src/test/java/com/matriz"
            sources.mkdir(parents=True)
            (sources / "ExampleTests.java").write_text(
                "package com.matriz;\nclass ExampleTests { @Test void works() {} }", encoding="utf-8"
            )
            with self.assertRaisesRegex(ValueError, "com.matriz.ExampleTests"):
                SUMMARY.check_complete(root)
            for group, name in (
                ("frontend", "validation.test.ts"),
                ("backend/unit", "com.matriz.ExampleTests"),
            ):
                reports = root / "target/test-results" / group
                reports.mkdir(parents=True)
                (reports / "junit.xml").write_text(
                    f'<testsuite name="{name}"><testcase name="works"/></testsuite>',
                    encoding="utf-8",
                )
            SUMMARY.check_complete(root)

    def test_coverage_rejects_a_java_suite_run_in_multiple_groups(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for group in ("unit", "integration"):
                reports = root / "target/test-results/backend" / group
                reports.mkdir(parents=True)
                (reports / "junit.xml").write_text(
                    '<testsuite name="com.matriz.ExampleTests">'
                    '<testcase name="works"/></testsuite>',
                    encoding="utf-8",
                )
            with self.assertRaisesRegex(ValueError, "multiple groups"):
                SUMMARY.check_complete(root)

    def test_coverage_ignores_test_helpers_without_junit_test_methods(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            sources = root / "src/test/java/com/matriz"
            sources.mkdir(parents=True)
            (sources / "TestAccess.java").write_text(
                "package com.matriz;\nclass TestAccess {}", encoding="utf-8"
            )
            reports = root / "target/test-results/frontend"
            reports.mkdir(parents=True)
            (reports / "junit.xml").write_text(
                '<testsuite name="validation.test.ts"><testcase name="works"/></testsuite>',
                encoding="utf-8",
            )
            SUMMARY.check_complete(root)


if __name__ == "__main__":
    unittest.main()
