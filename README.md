# GroceryScraper

> **Disclaimer:** 
> This project is a vibecoding experiment. I generally verify that code quality is decent, but I have intentionally not thought very hard about edge cases. Bug reports are appreciated, though!

A Kotlin CLI tool that uses Playwright to concurrently scrape and compare grocery prices across Walmart, Wegmans, Tops, Aldi, Trader Joe's, and Food Bazaar.

## Quick Start

### Requirements
Java 21+

### Run via Gradle (Recommended)
```bash
./gradlew run
```
*(Playwright automatically downloads required browser binaries on the first run.)*

### Build & Run Standalone Executable
```bash
./gradlew shadowJar
./scripts/grocery-scraper
```

## Usage

1. Enter your Zip Code.
2. Enter a product name (e.g., "Organic Milk") to search. 
   - *Tip: Enter `d` instead of a product name to enable non-headless debug mode.*
3. Results are displayed in the terminal and exported to an auto-opened HTML report.
