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
*(Playwright automatically downloads required browser binaries on the first run. The frontend is not built automatically through this task; use Docker for a complete build.)*

### Build & Run Standalone Executable
```bash
./gradlew shadowJar
./scripts/grocery-scraper
```

### Build & Run via Docker (Recommended for Web UI)
This project uses a multi-stage Docker build to compile the React frontend, package the Kotlin backend, and run them together in a Playwright-ready environment.

```bash
docker build -t grocery-scraper .
docker run -p 8080:8080 -it grocery-scraper
```

## Usage

You can run the application in two different modes: **Terminal UI (TUI)** and **Web Interface**.

### Web Interface (New)
To launch the interactive Web Dashboard instead of the CLI, run the application with the `--web` (or `-w`) flag. 
- If running via Docker, the web interface is started automatically by default on `http://localhost:8080`.
- If running via Gradle: `./gradlew run --args="--web"`

### Terminal UI (CLI)

1. Enter your Zip Code.
2. Enter a product name (e.g., "Organic Milk") to search. 
3. Results are displayed in the terminal and exported to an auto-opened HTML report.

### Command-Line Arguments
- `--web` or `-w`: Launches the Ktor Web server on `localhost:8080` instead of the TUI.
- `--debug` or `-d`: Launches Playwright in non-headless mode, making the browser visible so you can watch the scrapers run (useful for debugging captchas).
