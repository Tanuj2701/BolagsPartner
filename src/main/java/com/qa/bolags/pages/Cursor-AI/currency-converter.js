class CurrencyConverter {
    /**
     * Creates a CurrencyConverter.
     * @param {Object} rates - The rates object, e.g. { USD: 1, EUR: 0.92, JPY: 150.35 }
     * All rates should be relative to a "base" currency (e.g., USD).
     */
    constructor(rates = {}) {
        this.rates = { ...rates };
    }

    /**
     * Update conversion rates.
     * @param {Object} rates - Rates object to merge/update.
     */
    setRates(rates) {
        this.rates = { ...this.rates, ...rates };
    }

    /**
     * Convert a single amount or an array of amounts between currencies.
     * @param {number|number[]} amount - The amount(s) to convert.
     * @param {string} from - Source currency, e.g. 'USD'
     * @param {string} to - Target currency, e.g. 'EUR'
     * @returns {number|number[]} - Converted amount(s).
     */
    convert(amount, from, to) {
        if (!this.rates[from] || !this.rates[to]) {
            throw new Error(`Unsupported currency: from='${from}' or to='${to}'`);
        }
        // Convert a single number
        const convertOne = amt => {
            // Convert to base, then to target
            return (amt / this.rates[from]) * this.rates[to];
        };

        if (Array.isArray(amount)) {
            return amount.map(convertOne);
        }
        return convertOne(amount);
    }

    /**
     * Bulk convert amounts with different source/targets.
     * @param {Array<{amount:number, from:string, to:string}>} conversions
     * @returns {number[]} - Array of results.
     */
    bulkConvert(conversions) {
        return conversions.map(({ amount, from, to }) =>
            this.convert(amount, from, to)
        );
    }