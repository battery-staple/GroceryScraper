# GroceryScraper Developer Guide

## Project Overview
GroceryScraper is a modular Kotlin application for comparing grocery prices across multiple stores. It uses Playwright for deterministic scraping.

## Project Structure
- `src/main/kotlin/`
    - `App.kt`: Main entry point.
    - `engine/`: Orchestration logic for concurrent scraping.
    - `scrapers/`: Implementation of store-specific logic.
    - `models/`: Domain models and JSON schema definitions.
- `src/test/kotlin/`
    - Unit and integration tests following the guidelines below.

## Code Quality Guidelines

### Modularity
- **High Cohesion**: Keep scraping logic isolated from TUI and orchestration.
- **Loose Coupling**: Use the `Scraper` interface to interact with store providers. The engine should not know about specific store implementations.
- **Single Responsibility**: A class should have one reason to change. If a scraper is handling both network and parsing, split it.

### Design Patterns
- **Provider Pattern**: Scrapers are providers that satisfy a common interface.
- **State Emitting**: Use Kotlin Flow to emit scraping state to the console to track progress of background scraping tasks.

### Error Handling
- **Sealed Results**: Use sealed classes/interfaces for scraping results to force exhaustive handling of success and failure cases.
- **Explicit Failures**: Every failure must have a `reason` string that is both human-readable and suitable for JSON output.
- **No Side Effects**: Scrapers should be pure functions of `ScrapeRequest` -> `ScrapeResult` where possible, with the effect (Playwright) passed in.

### Assumptions and Planning for Change
- **Avoid Hardcoding**: Except for the store-specific scrapers (which inherently rely on site-specific logic), hardcoding should be strictly avoided. Design for extensibility.
- **Assumptions**: Avoid making assumptions whenever possible. If an assumption must be made:
    - It MUST be documented in [ASSUMPTIONS.md](./ASSUMPTIONS.md).
    - It should be backed by assertions in the code to fail fast if the assumption is violated.
- **Forward-looking Design**: Use data classes like `ScrapeRequest` for parameters to allow for future expansion without breaking API contracts.

### Package-Specific Rules
- **Nested AGENTS.md**: Document package-specific invariants in their own directories using `AGENTS.md` files (e.g., `src/main/kotlin/scrapers/AGENTS.md`).
- **Check Nested Directories**: ALWAYS check nested directories for `AGENTS.md` files whenever a directory is relevant to your task.

### Documentation
- **KDoc / Documentation**: Always add KDoc/documentation comments to all classes, interfaces, methods, and properties in the final implementation, except for extremely trivial ones. This rule also strictly applies to test helpers, such as Fake objects or custom utility functions in tests.

---


## Writing tests

### Mocking vs Fakes
- **No Mocking**: Never use mocking libraries (e.g., Mockito, MockK). 
- **Always Prefer Fakes**: Always use **Fake Objects** (stub implementations of interfaces) for testing. This ensures tests are decoupled from implementation details, remain readable, and force you to design better interfaces.

### Organization
When there are several sections of a test within a test class, mark each section using `//region xyz` and `//endregion xyz` comments.
Ensure that the endregion and region tags always match, and include in the `//endregion` tag the name of the region.

### Test Class Names
Unit test classes should be named with the name of the class under test with `Test` appended.

### Method Names
#### Integration tests
Integration test methods should use the following format:
- `fun test[SpecificDescriptionOfCase]`

#### Unit tests
Unit test methods should use the following format:
- `whenCondition_expectedResult`

### Assertions
Always use the most specific assertion possible (i.e. prefer using `assertThat(x).isEmpty()` over `assertTrue { x.isEmpty() }`).
- Avoid incomplete assertions on collections or objects (e.g. asserting only the size and one field of the first element). Instead, prefer complete assertions like `assertThat(myList).containsExactly(...)`.

- Prefer using Google Truth assertions, when possible.
  - Exception: do NOT use `assertThat(...).isTrue()` or `assertThat(...).isFalse()`. Instead prefer `assertTrue { ... }` and `assertFalse { ... }`.
  - Exception: do NOT use `assertThat(...).isInstanceOf(T::class.java)`; prefer Kotlin's `assertIs<T>()`.
- When Truth methods are not available, prefer `kotlin.test` assertions.
- When neither are available, use JUnit6 assertions.

### Style
Above all, ensure that all tests are easy to read and immediately verifiable.
Avoid any complex logic, and avoid duplicating business logic.

Also follow the following guidelines:
- Write tests in the arrange-act-assert style.
    - Separate each of these sections with a blank line, but do not include any comments denoting which section is which (it should be obvious).
- Do include comments for any non-obvious behavior, especially in setup and assertions.
- When two tests are nearly-identical save for the value of a parameter, consider using a JUnit6 `@ParameterizedTest`.
  - When using `@ParameterizedTest`s, use `@ValueSource` for simple values like primitives and `@EnumSource` for enums.
    However, for more complex parameterization, use `@MethodSource`.
    Avoid `@CSVSource`.
    Depending on the complexity, it may be reasonable to use a Kotlin property's auto-generated getter as a `@MethodSource`.
- When a simple piece of setup or assertions is duplicated across tests, extract a method for it.
  When more complex logic is duplicated, prefer extracting it into multiple atomic methods that are each trivially verifiable.
  Never extract a method if it makes a test more difficult to understand or verify.
- Prefer specific assertions over vague ones. For instance, do not just assert that some operation succeeds. Instead, assert that its return value is expected. Similarly, do not just assert that an operation fails; also validate the exception/error message and ensure it is exactly as expected.
- Don't write misleading assertions. For instance, do not use `assertIs<List<XYZ>>(x)`, because that will only validate that `x` is of type `List<*>` due to type erasure. Instead, write multiple assertions, the first of which checks that `x` is of type `List<*>` and the second of which checks its element type.
