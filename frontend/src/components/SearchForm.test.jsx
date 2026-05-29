import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import SearchForm from './SearchForm';

describe('SearchForm Component', () => {
  it('renders_all_input_fields', () => {
    render(<SearchForm onSubmit={vi.fn()} isLoading={false} availableStores={['Aldi', 'Tops']} />);
    expect(screen.getByLabelText(/Zip Code/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Search for a product/i)).toBeInTheDocument();
    expect(screen.getByText(/Debug Mode/i)).toBeInTheDocument();
    expect(screen.getByLabelText('Aldi')).toBeInTheDocument();
    expect(screen.getByLabelText('Tops')).toBeInTheDocument();
  });

  it('when_zip_code_is_empty_disables_submit_or_shows_error', () => {
    const handleSubmit = vi.fn();
    render(<SearchForm onSubmit={handleSubmit} isLoading={false} availableStores={['Aldi']} />);
    
    // Fill only product
    fireEvent.change(screen.getByLabelText(/Search for a product/i), { target: { value: 'milk' } });
    fireEvent.click(screen.getByText(/Search Products/i));
    
    expect(handleSubmit).not.toHaveBeenCalled();
    expect(screen.getByText(/Please enter a valid Zip Code/i)).toBeInTheDocument();
  });

  it('when_search_query_is_empty_disables_submit_or_shows_error', () => {
    const handleSubmit = vi.fn();
    render(<SearchForm onSubmit={handleSubmit} isLoading={false} availableStores={['Aldi']} />);
    
    // Fill only zip
    fireEvent.change(screen.getByLabelText(/Zip Code/i), { target: { value: '10001' } });
    fireEvent.click(screen.getByText(/Search Products/i));
    
    expect(handleSubmit).not.toHaveBeenCalled();
    expect(screen.getByText(/Please enter a search query/i)).toBeInTheDocument();
  });

  it('when_inputs_are_valid_calls_onSubmit_with_correct_payload', () => {
    const handleSubmit = vi.fn();
    render(<SearchForm onSubmit={handleSubmit} isLoading={false} availableStores={['Aldi', 'Tops']} />);
    
    fireEvent.change(screen.getByLabelText(/Zip Code/i), { target: { value: '10001' } });
    fireEvent.change(screen.getByLabelText(/Search for a product/i), { target: { value: 'milk' } });
    
    // Check initial stores state is selected by default
    expect(screen.getByLabelText('Aldi')).toBeChecked();
    
    // Uncheck Tops
    fireEvent.click(screen.getByLabelText('Tops'));
    
    fireEvent.click(screen.getByText(/Search Products/i));
    
    expect(handleSubmit).toHaveBeenCalledWith({
      zipCode: '10001',
      query: 'milk',
      isHeadless: true,
      selectedStores: ['Aldi']
    });
  });

  it('when_headless_mode_is_toggled_updates_state', () => {
    const handleSubmit = vi.fn();
    render(<SearchForm onSubmit={handleSubmit} isLoading={false} availableStores={[]} />);
    
    const debugToggle = screen.getByLabelText(/Debug Mode/i);
    // Initially isHeadless is true, so checkbox is unchecked
    expect(debugToggle).not.toBeChecked();
    
    fireEvent.click(debugToggle);
    expect(debugToggle).toBeChecked();
  });

  it('when_is_loading_prop_is_true_disables_submit_button', () => {
    render(<SearchForm onSubmit={vi.fn()} isLoading={true} availableStores={[]} />);
    
    expect(screen.getByLabelText(/Zip Code/i)).toBeDisabled();
    expect(screen.getByLabelText(/Search for a product/i)).toBeDisabled();
    expect(screen.getByRole('button', { name: /Scraping\.\.\./i })).toBeDisabled();
  });
});
