# Frontend Guidelines

## Core Principles
- **Simplicity**: Avoid complex state management frameworks like Redux or Zustand unless absolutely necessary. React's built-in `useState`, `useEffect`, and Context API are sufficient for this dashboard.
- **Minimal Dependencies**: Use native browser APIs for WebSockets and HTTP requests (`fetch`). Keep external libraries restricted to React, Vite, testing tools, and lightweight icon packs (e.g., `lucide-react`).

## Styling and Aesthetics
- **No TailwindCSS**: We use pure Vanilla CSS for styling to maximize flexibility and avoid utility-class clutter.
- **Glassmorphism**: Implement a high-fidelity dark mode theme (`#0f172a`). Use `backdrop-filter: blur(16px)` on cards with subtle borders and smooth color transitions.
- **Color Palette**: 
  - Emerald (`#10b981`) for success / cheapest items.
  - Indigo (`#6366f1`) for primary actions.
  - Amber (`#f59e0b`) for loading/warning states.
  - Crimson (`#ef4444`) for errors.
- **Micro-animations**: Use CSS `@keyframes` and transition properties to provide smooth feedback (e.g., pulsing states when scraping is active, glowing borders for the Best Deal card).

## Testing Strategy
- **Framework**: Use **Vitest** combined with **React Testing Library**.
- **Component Tests**: Every component must be rigorously tested in isolation. Test behaviors, not implementation details:
  - Verify all prop variations and conditional rendering states.
  - Verify that user interactions (clicks, typing) trigger the correct callback functions.
  - Use `data-testid` only when semantic queries (like `getByRole` or `getByText`) are not feasible.
- **Mocking**: Mock external dependencies such as the WebSocket connection or the `fetch` API. Provide deterministic JSON responses to simulate various scraping outcomes (success, network error, parsing error).
- **Integration Tests**: Verify that the top-level `App` correctly coordinates state between `SearchForm`, `ScraperProgress`, and `ProductResults`. Validate that real-time progress updates successfully flow down to the UI components.
