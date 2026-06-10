// Global State
let currencies = {};
let currentBase = 'USD';

// DOM Elements
const amountInput = document.getElementById('amount');
const sourceSymbol = document.getElementById('source-symbol');
const sourceSelect = document.getElementById('source-currency');
const targetSelect = document.getElementById('target-currency');
const swapBtn = document.getElementById('swap-btn');
const convertBtn = document.getElementById('convert-btn');

// Result Panel DOM
const rateFormula = document.getElementById('rate-formula');
const sourceDisplay = document.getElementById('source-display');
const targetDisplay = document.getElementById('target-display');

// Info Panel DOM
const infoSourceCode = document.getElementById('info-source-code');
const infoSourceSymbol = document.getElementById('info-source-symbol');
const infoSourceName = document.getElementById('info-source-name');
const infoTargetCode = document.getElementById('info-target-code');
const infoTargetSymbol = document.getElementById('info-target-symbol');
const infoTargetName = document.getElementById('info-target-name');

// Popular Rates & Update Time DOM
const lastUpdatedTime = document.getElementById('last-updated-time');
const popularRatesList = document.getElementById('popular-rates-list');
const popularBaseBadge = document.getElementById('popular-base-badge');

// Theme Switcher Logic
function setTheme(theme) {
    // Remove all theme classes
    document.body.classList.remove('theme-dark', 'theme-light', 'theme-blue');
    // Add selected theme class
    document.body.classList.add(`theme-${theme}`);
    
    // Toggle active class in UI buttons
    document.querySelectorAll('.theme-btn').forEach(btn => {
        if (btn.getAttribute('data-theme') === theme) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });
    
    // Persist choice
    localStorage.setItem('theme', theme);
}

// Initialize App
async function init() {
    // Apply saved theme or default to dark
    const savedTheme = localStorage.getItem('theme') || 'dark';
    setTheme(savedTheme);

    try {
        await fetchCurrencies();
        setupDropdowns();
        updateInfoPanel();
        await Promise.all([
            runConversion(),
            refreshPopularRates()
        ]);
        setupEventListeners();
    } catch (error) {
        console.error('Initialization failed:', error);
        showError('Please make sure the Java server is running on port 8080.');
    }
}

// Fetch supported currencies list
async function fetchCurrencies() {
    const response = await fetch('/api/currencies');
    if (!response.ok) throw new Error('Failed to load currency list');
    currencies = await response.json();
}

// Populate dropdown selectors
function setupDropdowns() {
    sourceSelect.innerHTML = '';
    targetSelect.innerHTML = '';

    // Sort codes alphabetically
    const codes = Object.keys(currencies).sort();

    codes.forEach(code => {
        const name = currencies[code].name;
        
        const optSource = document.createElement('option');
        optSource.value = code;
        optSource.textContent = `${code} - ${name}`;
        if (code === 'USD') optSource.selected = true;
        sourceSelect.appendChild(optSource);

        const optTarget = document.createElement('option');
        optTarget.value = code;
        optTarget.textContent = `${code} - ${name}`;
        if (code === 'INR') optTarget.selected = true;
        targetSelect.appendChild(optTarget);
    });
}

// Update currency detail panel and amount input symbol prefix
function updateInfoPanel() {
    const srcCode = sourceSelect.value;
    const tgtCode = targetSelect.value;

    const srcInfo = currencies[srcCode] || { name: 'Unknown', symbol: srcCode };
    const tgtInfo = currencies[tgtCode] || { name: 'Unknown', symbol: tgtCode };

    // Update Input Symbol Prefix
    sourceSymbol.textContent = srcInfo.symbol;

    // Update Detail Panel
    infoSourceCode.textContent = srcCode;
    infoSourceSymbol.textContent = srcInfo.symbol;
    infoSourceName.textContent = srcInfo.name;

    infoTargetCode.textContent = tgtCode;
    infoTargetSymbol.textContent = tgtInfo.symbol;
    infoTargetName.textContent = tgtInfo.name;
}

// Execute currency conversion
async function runConversion() {
    const from = sourceSelect.value;
    const to = targetSelect.value;
    const amount = amountInput.value || 1;

    try {
        convertBtn.classList.add('loading');
        const response = await fetch(`/api/convert?from=${from}&to=${to}&amount=${amount}`);
        
        if (!response.ok) {
            const err = await response.json();
            throw new Error(err.error || 'Conversion error');
        }

        const data = await response.json();
        
        // Update Results
        const srcSymbol = currencies[from]?.symbol || '';
        const tgtSymbol = currencies[to]?.symbol || '';
        
        rateFormula.textContent = `1 ${from} = ${data.rate.toFixed(4)} ${to}`;
        sourceDisplay.textContent = `${Number(data.amount).toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})} ${from} =`;
        targetDisplay.textContent = `${tgtSymbol} ${Number(data.result).toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})} ${to}`;
    } catch (error) {
        console.error('Conversion failed:', error);
        rateFormula.textContent = 'Error';
        sourceDisplay.textContent = 'Conversion failed';
        targetDisplay.textContent = error.message;
    } finally {
        convertBtn.classList.remove('loading');
    }
}

// Refresh popular rates section based on selected source currency
async function refreshPopularRates() {
    const base = sourceSelect.value;
    popularBaseBadge.textContent = `Base: ${base}`;
    
    // Popular list requested: USD, INR, EUR, GBP, JPY, AUD, CAD
    const popularCodes = ['USD', 'INR', 'EUR', 'GBP', 'JPY', 'AUD', 'CAD'];

    try {
        const response = await fetch(`/api/rates?base=${base}`);
        if (!response.ok) throw new Error('Failed to fetch rates');
        
        const data = await response.json();
        lastUpdatedTime.textContent = data.date;

        popularRatesList.innerHTML = '';
        
        popularCodes.forEach(code => {
            // Skip showing conversion to itself to save space/redundancy
            if (code === base) return;

            const rate = data.rates[code];
            if (rate === undefined) return;

            const name = currencies[code]?.name || '';
            const symbol = currencies[code]?.symbol || '';
            const baseSymbol = currencies[base]?.symbol || '';
            const equiv = (1 / rate).toFixed(4);

            const row = document.createElement('div');
            row.className = 'rate-row';
            row.innerHTML = `
                <div class="currency-cell">
                    <span class="code">${code}</span>
                    <span class="name">${name}</span>
                </div>
                <div class="rate-cell">${rate.toFixed(4)}</div>
                <div class="equiv-cell">1 ${code} = ${equiv} ${base}</div>
            `;

            // Make clicking a row set it as the target currency!
            row.addEventListener('click', () => {
                targetSelect.value = code;
                updateInfoPanel();
                runConversion();
            });

            popularRatesList.appendChild(row);
        });
    } catch (error) {
        console.error('Failed to load popular rates:', error);
        popularRatesList.innerHTML = '<div style="padding: 12px; color: var(--text-secondary);">Unable to fetch popular rates.</div>';
    }
}

// Show global error card
function showError(message) {
    rateFormula.textContent = 'Connection Error';
    sourceDisplay.textContent = 'Server Offline';
    targetDisplay.textContent = message;
    lastUpdatedTime.textContent = 'Offline';
}

// Setup Event Listeners
function setupEventListeners() {
    // Convert button click
    convertBtn.addEventListener('click', runConversion);

    // Swap currencies
    swapBtn.addEventListener('click', () => {
        const temp = sourceSelect.value;
        sourceSelect.value = targetSelect.value;
        targetSelect.value = temp;

        updateInfoPanel();
        runConversion();
        refreshPopularRates();
    });

    // Dropdown change events
    sourceSelect.addEventListener('change', () => {
        updateInfoPanel();
        runConversion();
        refreshPopularRates();
    });

    targetSelect.addEventListener('change', () => {
        updateInfoPanel();
        runConversion();
    });

    // Auto-convert on amount input (debounced slightly to prevent excessive requests)
    let timeout;
    amountInput.addEventListener('input', () => {
        clearTimeout(timeout);
        timeout = setTimeout(runConversion, 300);
    });

    // Quick conversion card triggers
    document.querySelectorAll('.quick-card').forEach(card => {
        card.addEventListener('click', () => {
            const from = card.getAttribute('data-from');
            const to = card.getAttribute('data-to');

            sourceSelect.value = from;
            targetSelect.value = to;

            updateInfoPanel();
            runConversion();
            refreshPopularRates();
        });
    });

    // Theme Switcher click events
    document.querySelectorAll('.theme-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const theme = btn.getAttribute('data-theme');
            setTheme(theme);
        });
    });
}

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', init);
