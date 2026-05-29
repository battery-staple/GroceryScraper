import React from 'react';
import { Compass, MapPin, Search, Database, AlertTriangle, CheckCircle, XCircle } from 'lucide-react';

export default function ScraperProgress({ progressStates, failures, results }) {
  const allStores = Array.from(
    new Set([
      ...Object.keys(progressStates),
      ...failures.map(f => f.store),
      ...results.map(r => r.store)
    ])
  );

  if (allStores.length === 0) return null;

  return (
    <div className="progress-section" style={{ marginTop: '2rem' }}>
      <h3 style={{ marginBottom: '1rem' }}>Scraping Progress</h3>
      <div className="progress-grid">
        {allStores.map(store => {
          // Determine the visual state of this store
          const failure = failures.find(f => f.store === store);
          const hasResults = results.some(r => r.store === store);
          const state = progressStates[store];
          
          let statusClass = '';
          let icon = null;
          let subtext = '';
          let statusText = 'Pending...';

          if (failure) {
            statusClass = 'status-Failure';
            icon = <XCircle size={20} className="color-crimson" />;
            statusText = 'Failed';
            subtext = failure.reason;
          } else if (hasResults || (state && state.type === 'Success')) {
            statusClass = 'status-Success';
            icon = <CheckCircle size={20} className="color-emerald" />;
            statusText = 'Complete';
            subtext = 'Products found';
          } else if (state) {
            switch (state.type) {
              case 'Navigating':
                icon = <Compass size={20} className="color-indigo icon-spin" />;
                statusText = 'Navigating';
                subtext = state.url;
                break;
              case 'SettingLocation':
                icon = <MapPin size={20} className="color-indigo icon-spin" />;
                statusText = 'Setting Location';
                subtext = `Zip: ${state.zipCode}`;
                break;
              case 'WaitingForResults':
                icon = <Search size={20} className="color-indigo icon-spin" />;
                statusText = 'Searching';
                subtext = state.additionalInfo || 'Waiting for products...';
                break;
              case 'Parsing':
                icon = <Database size={20} className="color-indigo icon-spin" />;
                statusText = 'Parsing';
                subtext = 'Extracting data...';
                break;
              case 'Warning':
                statusClass = 'status-Warning';
                icon = <AlertTriangle size={20} className="color-amber" />;
                statusText = 'Warning';
                subtext = state.message;
                break;
              default:
                icon = <Database size={20} className="color-indigo icon-spin" />;
                statusText = 'Active';
                break;
            }
          }

          return (
            <div key={store} className={`glass-card progress-card ${statusClass}`}>
              <div className="progress-header">
                <span>{store}</span>
                {icon}
              </div>
              <div style={{ fontSize: '0.9rem', fontWeight: 500, marginTop: '0.25rem' }}>
                {statusText}
              </div>
              <div className="progress-subtext" title={subtext}>
                {subtext || '\u00A0'}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
