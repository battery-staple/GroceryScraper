# GroceryScraper

A modular grocery price comparison tool with a premium Terminal User Interface (TUI) built using Kotlin, Mosaic, and Playwright.

## Setup

### Prerequisites
- Java 23+ (earlier versions may work if `jvmToolchain` is adjusted)
- Gradle 9.3+

### Installation
1. Clone the repository.
2. Initialize Playwright browsers:
   ```bash
   mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"
   ```
   *Note: Or run the application once; Playwright often handles installation on first run if configured, but manual installation is safer.*

### Option 3: Single Executable (Nicer)
You can build a single "fat" JAR that contains all dependencies:
```bash
./gradlew shadowJar
./scripts/grocery-scraper
```
*Note: This JAR includes Playwright and all required libraries, making it easy to distribute.*

## Usage
1. Enter your Zip Code when prompted.
2. Search for a product (e.g., "Organic Milk").
3. View comparison results across Walmart, Wegmans, Tops, Aldi, and Trader Joe's.
