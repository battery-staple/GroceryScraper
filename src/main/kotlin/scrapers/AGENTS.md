# Scrapers Package Guidelines

## Invariants
- **Purity**: Scrapers must ALWAYS be pure functions. They should not have any side effects (such as using `println`). All side effects should be handled by returning a state `Flow` or returning values that the `ScraperEngine` can then process.
- **Single Responsibility**: Scraper implementations should be decoupled into fetching/navigating and parsing logic if they are overly complex.
