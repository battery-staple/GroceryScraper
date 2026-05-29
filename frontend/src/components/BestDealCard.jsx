import React from 'react';
import { Award, ExternalLink } from 'lucide-react';

export default function BestDealCard({ products }) {
  if (!products || products.length === 0) return null;

  // Filter out products without a valid price
  const validProducts = products.filter(p => p.priceCents != null);
  if (validProducts.length === 0) return null;

  // Find the cheapest product
  const bestDeal = validProducts.reduce((prev, curr) => 
    prev.priceCents <= curr.priceCents ? prev : curr
  );

  return (
    <div className="glass-card best-deal-card">
      <div style={{ flex: 1 }}>
        <div className="best-deal-badge">
          <Award size={18} />
          Best Overall Deal
        </div>
        <div className="best-deal-details">
          <h3>{bestDeal.productName || 'Unknown Product'}</h3>
          <div className="price">${(bestDeal.priceCents / 100).toFixed(2)}</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginTop: '0.5rem' }}>
            <span className="store-badge">{bestDeal.store}</span>
            <a 
              href={bestDeal.link} 
              target="_blank" 
              rel="noopener noreferrer"
              className="btn-link"
              style={{ padding: 0, background: 'transparent' }}
            >
              View on Store <ExternalLink size={14} />
            </a>
          </div>
        </div>
      </div>
      <div style={{ display: 'none' }}>
        {/* We can add an illustration or icon here for large screens */}
      </div>
    </div>
  );
}
