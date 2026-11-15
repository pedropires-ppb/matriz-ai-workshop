import argparse
import html
import re
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path


GROUPS = (
    ("Frontend unit tests", "frontend"),
    ("Backend unit tests", "backend/unit"),
    ("Backend contract tests", "backend/contract"),
    ("Backend architecture tests", "backend/architecture"),
    ("Backend API and persistence integration tests", "backend/integration"),
    ("Backend browser end-to-end tests", "backend/browser"),
)


def suites(root, group):
    for report in sorted((root / "target/test-results" / group).glob("*.xml")):
        for suite in ET.parse(report).getroot().iter("testsuite"):
            cases = suite.findall("testcase")
            if cases:
                yield suite.get("name", report.stem), cases


def status(case):
    if case.find("failure") is not None or case.find("error") is not None:
        return "FAIL"
    if case.find("skipped") is not None:
        return "SKIP"
    return "PASS"


def escape(value):
    return html.escape(value).replace("|", "&#124;").replace("\n", " ").replace("\r", " ")


def render(root):
    lines = ["# Application test results", ""]
    for title, group in GROUPS:
        lines.extend([f"## {title}", ""])
        results = list(suites(root, group))
        if not results:
            lines.extend(["No report produced (step not reached or results unavailable).", ""])
            continue
        lines.extend([
            "| Suite | Passed | Failed/errors | Skipped | Total |",
            "|---|---:|---:|---:|---:|",
        ])
        total = Counter()
        for name, cases in results:
            counts = Counter(status(case) for case in cases)
            total.update(counts)
            lines.append(
                f"| {escape(name)} | {counts['PASS']} | {counts['FAIL']} | "
                f"{counts['SKIP']} | {len(cases)} |"
            )
        lines.append(
            f"| Total | {total['PASS']} | {total['FAIL']} | {total['SKIP']} | {sum(total.values())} |"
        )
        lines.extend(["", "<details><summary>Test case names and results</summary>", "", "<ul>"])
        for name, cases in results:
            for case in cases:
                lines.append(
                    f"<li>{status(case)} <code>{escape(name)}: "
                    f"{escape(case.get('name', '(unnamed)'))}</code></li>"
                )
        lines.extend(["</ul>", "</details>", ""])
    return "\n".join(lines)


def check_complete(root):
    actual = Counter(
        name for _, group in GROUPS if group.startswith("backend/")
        for name, _ in suites(root, group)
    )
    duplicates = sorted(name for name, count in actual.items() if count > 1)
    if duplicates:
        raise ValueError("Java suites ran in multiple groups: " + ", ".join(duplicates))
    expected = set()
    for source in (root / "src/test/java").rglob("*.java"):
        if source.stem.startswith("Test") or source.stem.endswith(("Test", "Tests", "TestCase")):
            content = source.read_text(encoding="utf-8")
            if not re.search(r"@(?:Test|ParameterizedTest|RepeatedTest|TestFactory|TestTemplate)\b", content):
                continue
            package = re.search(r"^package\s+([\w.]+);", content, re.MULTILINE)
            expected.add(f"{package.group(1)}.{source.stem}" if package else source.stem)
    missing = sorted(expected - actual.keys())
    if missing:
        raise ValueError("Missing Java test suites: " + ", ".join(missing))
    if not list(suites(root, "frontend")):
        raise ValueError("Missing frontend test report")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--require-complete", action="store_true")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    if args.require_complete:
        check_complete(root)
    print(render(root))


if __name__ == "__main__":
    main()
