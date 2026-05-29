import React, { useState, useMemo } from 'react';
import { ExternalLink, AlertCircle, ArrowDownAZ, ArrowUpZA } from 'lucide-react';
import BestDealCard from './BestDealCard';

export default function ProductResults({ results, failures }) {
  const [activeFilters, setActiveFilters] = useState([]);
  const [sortOrder, setSortOrder] = useState('asc'); // 'asc' or 'desc'

  // Extract all unique stores from results
  const availableStores = useMemo(() => {
    const stores = new Set(results.map(r => r.store));
    return Array.from(stores);
  }, [results]);

  // Handle toggling store filters
  const toggleFilter = (store) => {
    setActiveFilters(prev => 
      prev.includes(store)
        ? prev.filter(s => s !== store)
        : [...prev, store]
    );
  };

  // Filter and sort the products
  const displayedProducts = useMemo(() => {
    // 1. Filter
    let filtered = results;
    if (activeFilters.length > 0) {
      filtered = results.filter(r => activeFilters.includes(r.store));
    }

    // 2. Sort by price
    return [...filtered].sort((a, b) => {
      // Put null prices at the bottom
      if (a.priceCents == null) return 1;
      if (b.priceCents == null) return -1;
      
      return sortOrder === 'asc' 
        ? a.priceCents - b.priceCents 
        : b.priceCents - a.priceCents;
    });
  }, [results, activeFilters, sortOrder]);

  if (results.length === 0 && failures.length === 0) return null;

  return (
    <div className="results-section">
      {failures.length > 0 && (
        <div className="alert alert-error">
          <h4><AlertCircle size={18} /> Some scrapers encountered errors:</h4>
          <ul>
            {failures.map((f, i) => (
              <li key={i}><strong>{f.store}:</strong> {f.reason}</li>
            ))}
          </ul>
        </div>
      )}

      {results.length > 0 ? (
        <>
          <BestDealCard products={results} />

          <div className="glass-card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
              <h3 style={{ margin: 0 }}>All Results ({displayedProducts.length})</h3>
              
              <div className="filters">
                <span style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', alignSelf: 'center', marginRight: '0.5rem' }}>
                  Filter by Store:
                </span>
                {availableStores.map(store => (
                  <button
                    key={store}
                    className={`filter-pill ${activeFilters.includes(store) ? 'active' : ''}`}
                    onClick={() => toggleFilter(store)}
                  >
                    {store}
                  </button>
                ))}
                {activeFilters.length > 0 && (
                  <button 
                    className="filter-pill" 
                    onClick={() => setActiveFilters([])}
                    style={{ background: 'transparent', border: 'none' }}
                  >
                    Clear
                  </button>
                )}
              </div>
            </div>

            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>Store</th>
                    <th>Product</th>
                    <th 
                      onClick={() => setSortOrder(prev => prev === 'asc' ? 'desc' : 'asc')}
                      style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
                      title="Click to sort"
                    >
                      Price
                      {sortOrder === 'asc' ? <ArrowDownAZ size={16} /> : <ArrowUpZA size={16} />}
                    </th>
                    <th>Link</th>
                  </tr>
                </thead>
                <tbody>
                  {displayedProducts.map((product, idx) => (
                    <tr key={`${product.store}-${idx}`}>
                      <td><span className="store-badge">{product.store}</span></td>
                      <td style={{ maxWidth: '300px' }}>
                        {product.productName || 'Unknown Product'}
                      </td>
                      <td style={{ fontWeight: 600, color: product.priceCents != null ? 'var(--text-primary)' : 'var(--text-secondary)' }}>
                        {product.priceCents != null ? `$${(product.priceCents / 100).toFixed(2)}` : 'Unavailable'}
                      </td>
                      <td>
                        <a 
                          href={product.link} 
                          target="_blank" 
                          rel="noopener noreferrer"
                          className="btn-link"
                        >
                          View <ExternalLink size={14} />
                        </a>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      ) : (
        <div className="glass-card" style={{ textAlign: 'center', padding: '3rem 1rem' }}>
          <h3 style={{ color: 'var(--text-secondary)' }}>No products found.</h3>
        </div>
      )}
    </div>
  );
}
