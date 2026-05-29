import React, { useState, useEffect, useRef } from 'react';
import SearchForm from './components/SearchForm';
import ScraperProgress from './components/ScraperProgress';
import ProductResults from './components/ProductResults';
import { ShoppingCart } from 'lucide-react';

function App() {
  const [availableStores, setAvailableStores] = useState([]);
  const [isScraping, setIsScraping] = useState(false);
  const [progressStates, setProgressStates] = useState({});
  const [results, setResults] = useState([]);
  const [failures, setFailures] = useState([]);
  const wsRef = useRef(null);

  // Fetch available scrapers on mount
  useEffect(() => {
    fetch('/api/scrapers')
      .then(res => res.json())
      .then(data => {
        if (Array.isArray(data)) {
          setAvailableStores(data);
        }
      })
      .catch(err => console.error('Failed to fetch available scrapers:', err));
  }, []);

  // Cleanup WebSocket on unmount
  useEffect(() => {
    return () => {
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, []);

  const handleSearch = (payload) => {
    // Reset state for new run
    setProgressStates({});
    setResults([]);
    setFailures([]);
    setIsScraping(true);

    // Initialize progress states for selected stores
    const initialStates = {};
    payload.selectedStores.forEach(store => {
      initialStates[store] = { type: 'Pending' };
    });
    setProgressStates(initialStates);

    // Connect WebSocket
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    const wsUrl = `${protocol}//${host}/api/scrape/progress`;
    
    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      // Send the scrape request once connected
      ws.send(JSON.stringify(payload));
    };

    ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        
        switch (message.type) {
          case 'StateUpdate':
            const state = message.state;
            if (state && state.store) {
              setProgressStates(prev => ({
                ...prev,
                [state.store]: state
              }));
            }
            break;
            
          case 'FinalResult':
            const response = message.response;
            setResults(response.results || []);
            setFailures(response.failures || []);
            setIsScraping(false);
            ws.close();
            break;
            
          case 'Error':
            console.error('Server error:', message.message);
            // Treat as a general failure if not attached to a store
            setFailures(prev => [...prev, { store: 'System', reason: message.message }]);
            setIsScraping(false);
            ws.close();
            break;
            
          default:
            console.warn('Unknown message type:', message);
        }
      } catch (err) {
        console.error('Failed to parse WebSocket message:', err);
      }
    };

    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      setFailures(prev => [...prev, { store: 'Network', reason: 'Lost connection to server.' }]);
      setIsScraping(false);
    };

    ws.onclose = () => {
      if (isScraping) {
        // If closed prematurely
        setIsScraping(false);
      }
    };
  };

  return (
    <main>
      <div className="header">
        <h1>
          <ShoppingCart size={36} style={{ display: 'inline-block', verticalAlign: 'middle', marginRight: '0.5rem' }} />
          GroceryScraper
        </h1>
        <p>Find the best deals across your local supermarkets.</p>
      </div>

      <SearchForm 
        onSubmit={handleSearch} 
        isLoading={isScraping} 
        availableStores={availableStores} 
      />

      <ScraperProgress 
        progressStates={progressStates} 
        failures={failures} 
        results={results} 
      />

      <ProductResults 
        results={results} 
        failures={failures} 
      />
    </main>
  );
}

export default App;
