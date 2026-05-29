import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import ScraperProgress from './ScraperProgress';

describe('ScraperProgress Component', () => {
  it('renders_store_name_and_initial_status', () => {
    render(<ScraperProgress progressStates={{ 'Aldi': { type: 'Pending' } }} failures={[]} results={[]} />);
    expect(screen.getByText('Aldi')).toBeInTheDocument();
    expect(screen.getByText('Active')).toBeInTheDocument();
  });

  it('when_state_is_navigating_shows_navigating_icon_and_url', () => {
    render(<ScraperProgress progressStates={{ 'Aldi': { type: 'Navigating', url: 'https://aldi.com' } }} failures={[]} results={[]} />);
    expect(screen.getByText('Navigating')).toBeInTheDocument();
    // Assuming subtext is set via title attribute in our design
    expect(screen.getByTitle('https://aldi.com')).toBeInTheDocument();
  });

  it('when_state_is_parsing_shows_parsing_icon', () => {
    render(<ScraperProgress progressStates={{ 'Aldi': { type: 'Parsing' } }} failures={[]} results={[]} />);
    expect(screen.getByText('Parsing')).toBeInTheDocument();
  });

  it('when_state_is_warning_shows_warning_icon_and_message', () => {
    render(<ScraperProgress progressStates={{ 'Aldi': { type: 'Warning', message: 'Captcha detected' } }} failures={[]} results={[]} />);
    expect(screen.getByText('Warning')).toBeInTheDocument();
    expect(screen.getByTitle('Captcha detected')).toBeInTheDocument();
  });

  it('when_state_is_success_shows_check_icon', () => {
    render(<ScraperProgress progressStates={{}} failures={[]} results={[{ store: 'Aldi', priceCents: 100 }]} />);
    expect(screen.getByText('Complete')).toBeInTheDocument();
  });

  it('when_state_is_failure_shows_error_icon_and_reason', () => {
    render(<ScraperProgress progressStates={{}} failures={[{ store: 'Aldi', reason: 'Timeout' }]} results={[]} />);
    expect(screen.getByText('Failed')).toBeInTheDocument();
    expect(screen.getByTitle('Timeout')).toBeInTheDocument();
  });
});
