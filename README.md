# GroceryScraper

> **Disclaimer:** 
> This project is a vibecoding experiment. I generally verify that code quality is decent, but I have intentionally not thought very hard about edge cases. Bug reports are appreciated, though!

A Kotlin CLI tool that uses Playwright to concurrently scrape and compare grocery prices across Walmart, Wegmans, Tops, Aldi, Trader Joe's, and Food Bazaar.

## Quick Start

### Requirements
Java 21+

### Run via Gradle (Local Development)
```bash
./gradlew run
```
*(Playwright automatically downloads required browser binaries on the first run. The frontend is built and bundled automatically via Gradle tasks.)*

### Build & Run Standalone Executable
```bash
./gradlew executable
```
This generates self-contained executables for both Unix (macOS/Linux) and Windows.
You can then run the tool directly:
**macOS/Linux:**
```bash
./build/executable/grocery-scraper
```
**Windows:**
```cmd
.\build\executable\grocery-scraper.bat
```

## Usage

You can run the application in two different modes: **Terminal UI (TUI)** and **Web Interface**.

### Web Interface (New)
To launch the interactive Web Dashboard instead of the CLI, run the application with the `--web` (or `-w`) flag. 
- Run via Gradle: `./gradlew run --args="--web"`
- Run via standalone executable: `./build/executable/grocery-scraper --web` (or `.bat` on Windows)

### Terminal UI (CLI)

1. Enter your Zip Code.
2. Enter a product name (e.g., "Organic Milk") to search. 
3. Results are displayed in the terminal and exported to an auto-opened HTML report.

### Command-Line Arguments
- `--web` or `-w`: Launches the Ktor Web server on `localhost:8080` instead of the TUI.
- `--debug` or `-d`: Launches Playwright in non-headless mode, making the browser visible so you can watch the scrapers run (useful for debugging captchas).
