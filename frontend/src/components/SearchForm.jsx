import React, { useState, useEffect } from 'react';
import { Search } from 'lucide-react';

export default function SearchForm({ onSubmit, isLoading, availableStores }) {
  const [zipCode, setZipCode] = useState('');
  const [query, setQuery] = useState('');
  const [isHeadless, setIsHeadless] = useState(true);
  const [selectedStores, setSelectedStores] = useState([]);
  const [error, setError] = useState(null);

  // Initialize all stores to be selected when availableStores loads
  useEffect(() => {
    if (availableStores && availableStores.length > 0 && selectedStores.length === 0) {
      setSelectedStores(availableStores);
    }
  }, [availableStores]);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!zipCode.trim()) {
      setError('Please enter a valid Zip Code.');
      return;
    }
    if (!query.trim()) {
      setError('Please enter a search query.');
      return;
    }
    setError(null);
    onSubmit({
      query: query.trim(),
      zipCode: zipCode.trim(),
      isHeadless,
      selectedStores
    });
  };

  const handleStoreToggle = (store) => {
    setSelectedStores(prev => 
      prev.includes(store) 
        ? prev.filter(s => s !== store)
        : [...prev, store]
    );
  };

  return (
    <div className="glass-card form-card">
      <form onSubmit={handleSubmit}>
        <div className="grid-2">
          <div className="form-group">
            <label htmlFor="zipCode">Zip Code</label>
            <input
              id="zipCode"
              type="text"
              placeholder="e.g. 10001"
              value={zipCode}
              onChange={(e) => setZipCode(e.target.value)}
              disabled={isLoading}
            />
          </div>
          <div className="form-group">
            <label htmlFor="query">Search for a product</label>
            <input
              id="query"
              type="text"
              placeholder="e.g. milk, eggs, bread"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              disabled={isLoading}
            />
          </div>
        </div>

        {error && <div className="alert alert-error" style={{ marginBottom: '1rem', padding: '0.5rem' }}>{error}</div>}

        <div className="form-group" style={{ marginBottom: '1.5rem' }}>
          <label>Select Stores</label>
          <div className="checkbox-grid">
            {availableStores.map(store => (
              <label key={store} className="checkbox-label">
                <input
                  type="checkbox"
                  checked={selectedStores.includes(store)}
                  onChange={() => handleStoreToggle(store)}
                  disabled={isLoading}
                />
                {store}
              </label>
            ))}
          </div>
        </div>

        <div className="form-group" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <label className="toggle-switch">
            <div className={`toggle-switch-track ${!isHeadless ? 'active' : ''}`}>
              <div className="toggle-switch-thumb"></div>
            </div>
            <span>Debug Mode (Visible Browser)</span>
            <input
              type="checkbox"
              checked={!isHeadless}
              onChange={(e) => setIsHeadless(!e.target.checked)}
              disabled={isLoading}
              style={{ display: 'none' }}
            />
          </label>
          
          <button type="submit" className="btn-primary" disabled={isLoading} style={{ width: 'auto', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            {isLoading ? (
              <>
                <Search size={18} className="icon-spin" />
                Scraping...
              </>
            ) : (
              <>
                <Search size={18} />
                Search Products
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
