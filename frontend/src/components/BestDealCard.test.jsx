import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import BestDealCard from './BestDealCard';

describe('BestDealCard Component', () => {
  it('when_no_products_exist_does_not_render', () => {
    const { container } = render(<BestDealCard products={[]} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('when_multiple_products_exist_identifies_cheapest_item', () => {
    const products = [
      { store: 'Aldi', priceCents: 500, productName: 'Eggs', link: 'a' },
      { store: 'Tops', priceCents: 300, productName: 'Cheap Eggs', link: 'b' },
      { store: 'Wegmans', priceCents: 400, productName: 'Good Eggs', link: 'c' }
    ];
    render(<BestDealCard products={products} />);
    
    expect(screen.getByText('Cheap Eggs')).toBeInTheDocument();
    expect(screen.getByText('$3.00')).toBeInTheDocument();
    expect(screen.getByText('Tops')).toBeInTheDocument();
  });

  it('when_multiple_products_have_same_lowest_price_identifies_first', () => {
    const products = [
      { store: 'Tops', priceCents: 300, productName: 'Tops Eggs', link: 'a' },
      { store: 'Aldi', priceCents: 300, productName: 'Aldi Eggs', link: 'b' }
    ];
    render(<BestDealCard products={products} />);
    
    expect(screen.getByText('Tops Eggs')).toBeInTheDocument();
  });

  it('when_all_products_have_null_price_does_not_render', () => {
    const products = [
      { store: 'Tops', priceCents: null, productName: 'Tops Eggs', link: 'a' }
    ];
    const { container } = render(<BestDealCard products={products} />);
    expect(container).toBeEmptyDOMElement();
  });
});
