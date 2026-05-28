# Implementation Assumptions

This file documents all assumptions made during the development of GroceryScraper.

## Baseline Assumptions
- **Internet Connectivity**: We assume a stable internet connection is available for Playwright to fetch dynamic content.
- **Headless Execution**: We assume browsers can run in headless mode on the host system.
- **Store Availability**: We assume the stores selected (Walmart, Wegmans, etc.) are available in the provided Zip Code, or will gracefully return no results.
- **Currency**: We assume all prices are in USD and will be converted to cents for precision.

## Store-Specific Assumptions
- **Walmart**: Assumes the "Set Location" picker is accessible without a login.
- **Wegmans**: Assumes store selection can be bypassed or automated via a simple zip code search.
- **Tops Markets**: Assumes the site structure remains consistent across different regions.
- **Aldi**: Assumes product prices are displayed on the public search results page.
- **Trader Joe's**: Assumes the "Frequently Asked Questions" or "Products" section provides live pricing (note: TJ's often doesn't show prices online for all items).
