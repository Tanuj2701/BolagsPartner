# BolagsPartner Automation (Java)

Maven-based UI test automation for BolagsPartner using **Selenium 4**, **Cucumber** (Gherkin), and **TestNG**. WebDriver binaries are typically resolved via **WebDriverManager**.

---

## Quick status check (dry run)

Use these commands from the project root to validate the build without (or before) a full browser run.


| Goal                        | Command                             | Expected                                    |
| --------------------------- | ----------------------------------- | ------------------------------------------- |
| Compile main code           | `mvn -B compile`                    | `BUILD SUCCESS`                             |
| Compile tests               | `mvn -B test-compile`               | `BUILD SUCCESS`                             |
| Full clean compile          | `mvn -B clean compile test-compile` | `BUILD SUCCESS`                             |
| Run TestNG suite (Surefire) | `mvn -B test`                       | Runs `CucumberTestRunner` from `testng.xml` |


**Dry run result on this workspace (2026-05-29)**

- `mvn clean compile test-compile` completed successfully but reported **nothing to compile** for Java, because `**src/main/java` and `src/test/java` are empty on disk** (sources were removed from the working tree while still listed in git).
- `mvn test` **failed** with: `Cannot find class in classpath: com.qa.bolags.testrunner.CucumberTestRunner`.

**Restore Java sources from git** (if your tree matches this situation):

```bash
git restore src/main/java src/test/java
```

Then re-run `mvn clean test-compile` and `mvn test`.

---

## Prerequisites

- **JDK 8** (project `pom.xml` targets 1.8) or a newer JDK that can compile with `--release` / source 8 as configured.
- **Apache Maven** 3.6+.
- **Google Chrome** (or whichever browser your `BaseTest` / WebDriver setup uses), installed and runnable on the machine that executes tests.
- **Network** access for dependency download and for tests that hit QA/production URLs (as configured in your test data).
- **Tesseract** and related native libs may be required if OCR features (e.g. Tess4j) are used in your restored code paths.

Do **not** commit secrets (passwords, API keys, private keys). Use environment variables or local config excluded from git, and keep `config.xlsx` or similar out of public repos if they contain credentials.

---

## Project layout and flow

```text
BolagsPartner_Automation_Java/
├── pom.xml                          # Maven dependencies & Surefire → TestNG suite
├── README.md                        # This file
├── docs/
│   └── Functional_Specification.md   # Product / flow context (BolagsPartner platform)
├── src/main/java/com/qa/bolags/     # Page objects, base test, utilities, listeners (when restored)
├── src/main/resources/
│   ├── config/
│   │   └── config.properties        # QA HTTP Basic + generic-order super admin (tune per env)
│   ├── qa/config.xlsx               # QA-oriented test data (do not commit secrets)
│   └── prod/config.xlsx             # Prod-oriented test data (use with extreme care)
├── src/test/java/com/qa/bolags/
│   ├── testrunner/
│   │   └── CucumberTestRunner.java  # TestNG entry point; loads Cucumber options & glue
│   └── stepdefs/                    # Cucumber step definitions (glue code)
├── src/test/resources/
│   ├── features/                    # Gherkin feature files
│   │   ├── liquidationflow.feature
│   │   ├── adminflow.feature
│   │   └── shiroResellerUsers.feature   # Generic Order — Shiro & Reseller (super admin)
│   └── testrunner/
│       └── testng.xml               # Surefire runs this suite
├── src/downloadedFiles/             # Runtime downloads (e.g. uploads); review .gitignore
└── ScreenShots/                     # Optional screenshots from runs
```

### Execution flow

1. **Maven** runs the **Surefire** plugin with `src/test/resources/testrunner/testng.xml`.
2. **TestNG** loads `**com.qa.bolags.testrunner.CucumberTestRunner`**.
3. **Cucumber** reads `**features/*.feature`**, matches steps to **glue** classes under `com.qa.bolags.stepdefs`, and drives **Selenium** through **page objects** in `com.qa.bolags.pages` (and shared helpers such as `baseTest`, `utility`).
4. **Reports**: HTML/JSON/JUnit style outputs may be produced under `target/` or `test-output/` depending on listeners and Cucumber plugins configured in the runner (restore Java sources to see exact paths).

---

## How to run tests

### Default suite (all tests configured in the runner)

```bash
mvn clean test
```

### Skip tests (compile only)

```bash
mvn clean install -DskipTests
```

### Run with a specific TestNG XML (if you add alternate suites)

```bash
mvn test -Dsurefire.suiteXmlFiles=src/test/resources/testrunner/testng.xml
```

(Your `pom.xml` already pins the suite file; overriding is optional.)

### Run only Shiro and Reseller (Generic Order) feature

```bash
mvn test -Dcucumber.features=src/test/resources/features/shiroResellerUsers.feature
```

Run one tag (example: Shiro scenario only):

```bash
mvn test -Dcucumber.features=src/test/resources/features/shiroResellerUsers.feature -Dcucumber.filter.tags=@shiroUser
```

HTTP Basic and super-admin application credentials for these flows live in `src/main/resources/config/config.properties` (`qa.server.basicAuth.*`, `qa.app.genericOrder.superAdmin*`).

---

## Cucumber “dry run” (validate steps without executing them)

Cucumber can verify that every step in your feature files has a matching step definition **without** opening a browser or running step bodies. With **Cucumber 7**, the usual approach is a `**cucumber.properties`** file on the test classpath.

1. Create `**src/test/resources/cucumber.properties**` with:
  ```properties
   cucumber.execution.dry-run=true
  ```
2. Run:
  ```bash
   mvn test
  ```
3. Set `**cucumber.execution.dry-run=false**` (or remove the file) for real executions.

**Alternative:** If your restored `CucumberTestRunner` uses `@CucumberOptions`, you can temporarily set `dryRun = true` there (remember to revert before CI or real runs).

---

## Configuration and data


| Resource                                           | Purpose                                                                                |
| -------------------------------------------------- | -------------------------------------------------------------------------------------- |
| `src/main/resources/qa/config.xlsx`                | QA environment URLs, users, or datasets (structure depends on `ExcelUtility` / tests). |
| `src/main/resources/prod/config.xlsx`              | Production-like data; use only when explicitly required and approved.                  |
| Feature files under `src/test/resources/features/` | Human-readable scenarios; keep in sync with step definitions.                          |


---

## Dependencies (high level)

Defined in `pom.xml`, including among others: Selenium Java, Cucumber Java + TestNG, TestNG, WebDriverManager, Apache POI, Rest Assured pieces, ExtentReports, Tess4j, Oracle JDBC, OpenCV / ZXing (image/QR), SLF4J / Logback.

---

## Troubleshooting


| Symptom                                    | Likely cause                              | What to do                                                |
| ------------------------------------------ | ----------------------------------------- | --------------------------------------------------------- |
| `Cannot find class ... CucumberTestRunner` | Missing `src/test/java` or failed compile | `git restore src/test/java` then `mvn clean test-compile` |
| `Nothing to compile` for Java              | No `.java` under `src/*/java`             | Restore sources from git or copy from backup              |
| Browser / driver errors                    | Chrome/driver mismatch or headless flags  | Update Chrome; WebDriverManager usually aligns drivers    |
| Sandbox / CI `mvn` cannot write `~/.m2`    | Restricted environment                    | Run Maven with normal user home and network to Central    |


---

## Related documentation

- `**docs/Functional_Specification.md`** — business context for liquidation and admin flows that these tests automate.

---

## Contributing

- Match existing package names (`com.qa.bolags.*`) and patterns in page objects and step defs.
- Add or update step definitions whenever you add new Gherkin steps.
- Run a Cucumber dry run after feature edits to catch missing steps early.

