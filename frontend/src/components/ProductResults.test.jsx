import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import ProductResults from './ProductResults';

describe('ProductResults Component', () => {
  it('when_results_are_empty_shows_no_results_message', () => {
    render(<ProductResults results={[]} failures={[]} />);
    // Component returns null if both empty, but we can test when it's rendered empty inside wrapper or just returns null
    // Wait, the component returns null if results.length === 0 && failures.length === 0.
    // Let's pass a dummy failure so it renders the empty state for results.
    render(<ProductResults results={[]} failures={[{store: 'A', reason: 'B'}]} />);
    expect(screen.getByText('No products found.')).toBeInTheDocument();
  });

  it('when_failures_exist_shows_failure_alert', () => {
    render(<ProductResults results={[]} failures={[{ store: 'Aldi', reason: 'Blocked' }]} />);
    expect(screen.getByText(/Some scrapers encountered errors/i)).toBeInTheDocument();
    expect(screen.getByText('Aldi:')).toBeInTheDocument();
    expect(screen.getByText('Blocked', { exact: false })).toBeInTheDocument();
  });

  it('renders_table_with_correct_columns', () => {
    const products = [{ store: 'Aldi', priceCents: 100, productName: 'A', link: '#' }];
    render(<ProductResults results={products} failures={[]} />);
    expect(screen.getByText('Store')).toBeInTheDocument();
    expect(screen.getByText('Product')).toBeInTheDocument();
    expect(screen.getByText(/Price/)).toBeInTheDocument();
    expect(screen.getByText('Link')).toBeInTheDocument();
  });

  it('formats_price_correctly', () => {
    const products = [{ store: 'Aldi', priceCents: 1099, productName: 'A', link: '#' }];
    render(<ProductResults results={products} failures={[]} />);
    expect(screen.getAllByText('$10.99').length).toBeGreaterThan(0);
  });

  it('when_price_is_null_shows_unavailable', () => {
    const products = [{ store: 'Aldi', priceCents: null, productName: 'A', link: '#' }];
    render(<ProductResults results={products} failures={[]} />);
    expect(screen.getAllByText('Unavailable').length).toBeGreaterThan(0);
  });

  it('when_store_filter_is_clicked_hides_unselected_stores', () => {
    const products = [
      { store: 'Aldi', priceCents: 100, productName: 'Aldi A', link: '#' },
      { store: 'Tops', priceCents: 200, productName: 'Tops B', link: '#' }
    ];
    render(<ProductResults results={products} failures={[]} />);
    
    expect(screen.getAllByText('Aldi A').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Tops B').length).toBeGreaterThan(0);
    
    // Click Aldi pill
    const aldiPill = screen.getByRole('button', { name: 'Aldi' });
    fireEvent.click(aldiPill);
    
    expect(screen.getAllByText('Aldi A').length).toBeGreaterThan(0);
    expect(screen.queryByText('Tops B')).not.toBeInTheDocument();
  });

  it('when_price_column_header_is_clicked_sorts_products', () => {
    const products = [
      { store: 'Aldi', priceCents: 500, productName: 'A', link: '#' },
      { store: 'Tops', priceCents: 300, productName: 'B', link: '#' }
    ];
    render(<ProductResults results={products} failures={[]} />);
    
    // By default, sorting is ASC
    const rowsInitial = screen.getAllByRole('row');
    // Header is rowsInitial[0]
    expect(rowsInitial[1]).toHaveTextContent('B'); // 300
    expect(rowsInitial[2]).toHaveTextContent('A'); // 500
    
    // Click to sort DESC
    fireEvent.click(screen.getByText(/Price/));
    const rowsDesc = screen.getAllByRole('row');
    expect(rowsDesc[1]).toHaveTextContent('A'); // 500
    expect(rowsDesc[2]).toHaveTextContent('B'); // 300
  });
});
