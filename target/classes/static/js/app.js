/* ════════════════════════════════════════════════════════════
   WATTS AHEAD — Frontend App
   ════════════════════════════════════════════════════════════ */

const API = '/api';
const READING_FORM_STATE_KEY = 'wattsAheadReadingFormState';
const DEFAULT_CHARGES = {
    generationChargePerKwh: 7.6398,
    distributionChargePerKwh: 1.2908,
    transmissionChargePerKwh: 1.2343,
    systemLossChargePerKwh: 0.6987,
    taxesAndUniversalChargeRatePercent: 12.00,
};

let kwhChartInst = null;
let billChartInst = null;
let projChartInst = null;

/* ─── UTILS ─────────────────────────────────────────────── */
const $ = id => document.getElementById(id);
const fmt = n => Number(n).toLocaleString('en-PH', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

function readSavedReadingForms() {
    try {
        return JSON.parse(localStorage.getItem(READING_FORM_STATE_KEY)) || {};
    } catch (_) {
        return {};
    }
}

function getSavedReadingForm(id) {
    return readSavedReadingForms()[String(id)] || null;
}

function saveReadingForm(id, formState) {
    if (!id) return;

    const forms = readSavedReadingForms();
    forms[String(id)] = formState;
    localStorage.setItem(READING_FORM_STATE_KEY, JSON.stringify(forms));
}

function removeSavedReadingForm(id) {
    const forms = readSavedReadingForms();
    delete forms[String(id)];
    localStorage.setItem(READING_FORM_STATE_KEY, JSON.stringify(forms));
}

function getChargeBreakdown(reading = {}) {
    const hasComponentCharges = [
        reading.generationChargePerKwh,
        reading.distributionChargePerKwh,
        reading.transmissionChargePerKwh,
        reading.systemLossChargePerKwh,
    ].some(value => value !== null && value !== undefined);

    if (hasComponentCharges) {
        return {
            generationChargePerKwh: Number(reading.generationChargePerKwh ?? DEFAULT_CHARGES.generationChargePerKwh),
            distributionChargePerKwh: Number(reading.distributionChargePerKwh ?? DEFAULT_CHARGES.distributionChargePerKwh),
            transmissionChargePerKwh: Number(reading.transmissionChargePerKwh ?? DEFAULT_CHARGES.transmissionChargePerKwh),
            systemLossChargePerKwh: Number(reading.systemLossChargePerKwh ?? DEFAULT_CHARGES.systemLossChargePerKwh),
        };
    }

    const defaultTotal =
        DEFAULT_CHARGES.generationChargePerKwh +
        DEFAULT_CHARGES.distributionChargePerKwh +
        DEFAULT_CHARGES.transmissionChargePerKwh +
        DEFAULT_CHARGES.systemLossChargePerKwh;
    const totalRate = Number(reading.ratePerKwh) || defaultTotal;
    const scale = totalRate / defaultTotal;

    return {
        generationChargePerKwh: DEFAULT_CHARGES.generationChargePerKwh * scale,
        distributionChargePerKwh: DEFAULT_CHARGES.distributionChargePerKwh * scale,
        transmissionChargePerKwh: DEFAULT_CHARGES.transmissionChargePerKwh * scale,
        systemLossChargePerKwh: DEFAULT_CHARGES.systemLossChargePerKwh * scale,
    };
}

function showToast(msg, type = 'success') {
    const t = $('toast');
    t.textContent = msg;
    t.className = `toast ${type}`;
    setTimeout(() => t.className = 'toast hidden', 3500);
}

async function apiFetch(path, opts = {}) {
    try {
        const res = await fetch(API + path, {
            headers: { 'Content-Type': 'application/json' },
            ...opts,
        });
        if (!res.ok) {
            const err = await res.text();
            let message = err || `HTTP ${res.status}`;

            try {
                const parsed = JSON.parse(err);
                message = parsed.message || parsed.error || message;
                if (parsed.path) message = `${message}: ${parsed.path}`;
            } catch (_) {}

            throw new Error(message);
        }
        return res.status === 204 ? null : res.json();
    } catch (e) {
        showToast(e.message, 'error');
        throw e;
    }
}

/* ─── TAB NAVIGATION ────────────────────────────────────── */
document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.tab-panel').forEach(p => p.classList.add('hidden'));
        btn.classList.add('active');
        $(`tab-${btn.dataset.tab}`).classList.remove('hidden');

        if (btn.dataset.tab === 'dashboard') loadDashboard();
        if (btn.dataset.tab === 'readings') loadReadings();
    });
});

/* ─── DASHBOARD ─────────────────────────────────────────── */
async function loadDashboard() {
    const stats = await apiFetch('/stats').catch(() => null);
    if (!stats) return;

    $('statTotalReadings').textContent = stats.totalReadings;
    $('statAvgBillVal').textContent = stats.totalReadings ? `₱${fmt(stats.avgBill)}` : '—';
    $('statHighBill').textContent = stats.totalReadings ? `₱${fmt(stats.highestBill)}` : '—';
    $('statLowBill').textContent = stats.totalReadings ? `₱${fmt(stats.lowestBill)}` : '—';

    const trendEl = $('statTrendVal');
    const trendMap = { increasing: '↑ Increasing', decreasing: '↓ Decreasing', stable: '→ Stable', 'no data': 'No data' };
    trendEl.textContent = trendMap[stats.trend] || stats.trend;
    trendEl.dataset.trend = stats.trend;

    drawKwhChart(stats.chartLabels || [], stats.chartKwh || []);
    drawBillChart(stats.chartLabels || [], stats.chartBill || []);
}

function chartDefaults() {
    return {
        responsive: true,
        maintainAspectRatio: true,
        plugins: { legend: { display: false }, tooltip: { bodyFont: { family: 'Space Mono' } } },
        scales: {
            x: { ticks: { color: '#555f70', font: { family: 'Space Mono', size: 11 } }, grid: { color: '#1a1e24' } },
            y: { ticks: { color: '#555f70', font: { family: 'Space Mono', size: 11 } }, grid: { color: '#1a1e24' } },
        },
    };
}

function drawKwhChart(labels, data) {
    if (kwhChartInst) kwhChartInst.destroy();
    kwhChartInst = new Chart($('kwhChart'), {
        type: 'line',
        data: {
            labels,
            datasets: [{
                data,
                borderColor: '#4f9cf9',
                backgroundColor: 'rgba(79,156,249,0.08)',
                borderWidth: 2,
                pointBackgroundColor: '#4f9cf9',
                pointRadius: 4,
                tension: 0.3,
                fill: true,
            }],
        },
        options: { ...chartDefaults() },
    });
}

function drawBillChart(labels, data) {
    if (billChartInst) billChartInst.destroy();
    billChartInst = new Chart($('billChart'), {
        type: 'bar',
        data: {
            labels,
            datasets: [{
                data,
                backgroundColor: 'rgba(245,200,66,0.25)',
                borderColor: '#f5c842',
                borderWidth: 1.5,
                borderRadius: 4,
            }],
        },
        options: { ...chartDefaults() },
    });
}

/* ─── READINGS ──────────────────────────────────────────── */
async function loadReadings() {
    const readings = await apiFetch('/readings').catch(() => []);
    const tbody = $('readingsTbody');
    tbody.innerHTML = '';

    if (!readings.length) {
        tbody.innerHTML = '<tr><td colspan="10" class="empty-row">No readings yet. Add your first one!</td></tr>';
        return;
    }

    readings.forEach(r => {
        const previous = Number(r.previousMeterReading) || 0;
        const current = Number(r.currentMeterReading);
        const consumption = Number(r.kwhConsumption ?? r.kwhUsage ?? Math.max(0, (Number.isFinite(current) ? current : 0) - previous));
        const charges = getChargeBreakdown(r);
        const generationCharge = charges.generationChargePerKwh;
        const distributionCharge = charges.distributionChargePerKwh;
        const transmissionCharge = charges.transmissionChargePerKwh;
        const systemLossCharge = charges.systemLossChargePerKwh;
        const generationTotal = consumption * generationCharge;
        const distributionTotal = consumption * distributionCharge;
        const transmissionTotal = consumption * transmissionCharge;
        const systemLossTotal = consumption * systemLossCharge;
        const baseCost = generationTotal + distributionTotal + transmissionTotal + systemLossTotal;
        const taxRate = Number(r.taxesAndUniversalChargeRate ?? ((r.taxesPercent || 0) / 100));
        const taxes = Number(r.taxesAndUniversalCharges ?? r.taxesTotal ?? (baseCost * taxRate));
        const total = Number(r.totalBill ?? (baseCost + taxes));
        
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${r.readingDate}</td>
            <td>${fmt(consumption)} kWh</td>
            <td>₱${fmt(generationTotal)}</td>
            <td>₱${fmt(distributionTotal)}</td>
            <td>₱${fmt(transmissionTotal)}</td>
            <td>₱${fmt(systemLossTotal)}</td>
            <td>₱${fmt(taxes)}</td>
            <td class="bill-amount">₱${fmt(total)}</td>
            <td>${r.notes || '<span style="color:var(--text3)">—</span>'}</td>
            <td>
                <button class="btn btn-edit" onclick="editReading(${r.id})">Edit</button>
                <button class="btn btn-danger" onclick="deleteReading(${r.id})">Delete</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

/* ─── MODAL ─────────────────────────────────────────────── */
function openModal(title = 'Add Reading') {
    $('modalTitle').textContent = title;
    $('readingModal').classList.remove('hidden');
    $('modalBackdrop').classList.remove('hidden');
}

function closeModal() {
    $('readingModal').classList.add('hidden');
    $('modalBackdrop').classList.add('hidden');
    $('readingForm').reset();
    $('readingId').value = '';
    $('generationChargePerKwh').value = DEFAULT_CHARGES.generationChargePerKwh.toFixed(4);
    $('distributionChargePerKwh').value = DEFAULT_CHARGES.distributionChargePerKwh.toFixed(4);
    $('transmissionChargePerKwh').value = DEFAULT_CHARGES.transmissionChargePerKwh.toFixed(4);
    $('systemLossChargePerKwh').value = DEFAULT_CHARGES.systemLossChargePerKwh.toFixed(4);
    $('taxesAndUniversalChargeRate').value = DEFAULT_CHARGES.taxesAndUniversalChargeRatePercent.toFixed(2);
    updateConsumptionPreview();
}

$('openAddModal').addEventListener('click', () => openModal('Add Reading'));
$('closeModal').addEventListener('click', closeModal);
$('cancelModal').addEventListener('click', closeModal);
$('modalBackdrop').addEventListener('click', closeModal);

let deleteConfirmResolve = null;

function askDeleteConfirmation() {
    $('deleteConfirmModal').classList.remove('hidden');
    $('confirmDeleteBtn').focus();

    return new Promise(resolve => {
        deleteConfirmResolve = resolve;
    });
}

function closeDeleteConfirmation(confirmed = false) {
    $('deleteConfirmModal').classList.add('hidden');

    if (deleteConfirmResolve) {
        deleteConfirmResolve(confirmed);
        deleteConfirmResolve = null;
    }
}

$('confirmDeleteBtn').addEventListener('click', () => closeDeleteConfirmation(true));
$('cancelDeleteBtn').addEventListener('click', () => closeDeleteConfirmation(false));
$('deleteConfirmModal').addEventListener('click', e => {
    if (e.target === $('deleteConfirmModal')) closeDeleteConfirmation(false);
});

document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && !$('deleteConfirmModal').classList.contains('hidden')) {
        closeDeleteConfirmation(false);
    }
});

function updateConsumptionPreview() {
    const current = parseFloat($('currentMeterReading').value) || 0;
    const previous = parseFloat($('previousMeterReading').value) || 0;
    const consumption = Math.max(0, current - previous);

    if ($('calcConsumption')) {
        $('calcConsumption').textContent = consumption.toFixed(2);
    }
}

// Update consumption display as user types
['currentMeterReading', 'previousMeterReading'].forEach(id => {
    $(id) && $(id).addEventListener('input', updateConsumptionPreview);
});

$('readingForm').addEventListener('submit', async e => {
    e.preventDefault();
    const id = $('readingId').value;
    const current = parseFloat($('currentMeterReading').value);
    const previous = parseFloat($('previousMeterReading').value) || 0;
    
    if (current < previous) {
        showToast('Current reading must be >= previous reading', 'error');
        return;
    }

    const generationCharge = parseFloat($('generationChargePerKwh').value) || 0;
    const distributionCharge = parseFloat($('distributionChargePerKwh').value) || 0;
    const transmissionCharge = parseFloat($('transmissionChargePerKwh').value) || 0;
    const systemLossCharge = parseFloat($('systemLossChargePerKwh').value) || 0;
    const taxRatePercent = parseFloat($('taxesAndUniversalChargeRate').value) || 0;
    const consumption = Math.max(0, current - previous);
    const totalRatePerKwh = generationCharge + distributionCharge + transmissionCharge + systemLossCharge;
    
    const body = {
        readingDate: $('readingDate').value,
        currentMeterReading: current,
        previousMeterReading: previous,
        generationChargePerKwh: generationCharge,
        distributionChargePerKwh: distributionCharge,
        transmissionChargePerKwh: transmissionCharge,
        systemLossChargePerKwh: systemLossCharge,
        taxesAndUniversalChargeRate: taxRatePercent / 100,
        kwhUsage: consumption,
        ratePerKwh: totalRatePerKwh,
        fixedCharges: 0,
        taxesPercent: taxRatePercent,
        notes: $('notes').value,
    };

    try {
        let saved;

        if (id) {
            saved = await apiFetch(`/readings/${id}`, { method: 'PUT', body: JSON.stringify(body) });
            showToast('Reading updated!');
        } else {
            saved = await apiFetch('/readings', { method: 'POST', body: JSON.stringify(body) });
            showToast('Reading saved!');
        }

        saveReadingForm(saved?.id || id, {
            currentMeterReading: current,
            previousMeterReading: previous,
            generationChargePerKwh: generationCharge,
            distributionChargePerKwh: distributionCharge,
            transmissionChargePerKwh: transmissionCharge,
            systemLossChargePerKwh: systemLossCharge,
            taxesAndUniversalChargeRatePercent: taxRatePercent,
        });

        closeModal();
        loadReadings();
    } catch (_) {}
});

async function editReading(id) {
    const r = await apiFetch(`/readings/${id}`).catch(() => null);
    if (!r) return;
    const savedForm = getSavedReadingForm(id);
    const hasCurrentReading = r.currentMeterReading !== null && r.currentMeterReading !== undefined;
    const hasPreviousReading = r.previousMeterReading !== null && r.previousMeterReading !== undefined;
    const charges = getChargeBreakdown(r);

    $('readingId').value = r.id;
    $('readingDate').value = r.readingDate;
    $('currentMeterReading').value = savedForm?.currentMeterReading ?? (hasCurrentReading ? r.currentMeterReading : '');
    $('previousMeterReading').value = savedForm?.previousMeterReading ?? (hasPreviousReading ? r.previousMeterReading : '');
    $('generationChargePerKwh').value = savedForm?.generationChargePerKwh ?? charges.generationChargePerKwh.toFixed(4);
    $('distributionChargePerKwh').value = savedForm?.distributionChargePerKwh ?? charges.distributionChargePerKwh.toFixed(4);
    $('transmissionChargePerKwh').value = savedForm?.transmissionChargePerKwh ?? charges.transmissionChargePerKwh.toFixed(4);
    $('systemLossChargePerKwh').value = savedForm?.systemLossChargePerKwh ?? charges.systemLossChargePerKwh.toFixed(4);
    $('taxesAndUniversalChargeRate').value = (savedForm?.taxesAndUniversalChargeRatePercent ?? ((r.taxesAndUniversalChargeRate ?? DEFAULT_CHARGES.taxesAndUniversalChargeRatePercent / 100) * 100)).toFixed(2);
    $('notes').value = r.notes || '';
    updateConsumptionPreview();
    openModal('Edit Reading');
}

async function deleteReading(id) {
    const confirmed = await askDeleteConfirmation();
    if (!confirmed) return;

    await apiFetch(`/readings/${id}`, { method: 'DELETE' }).catch(() => null);
    removeSavedReadingForm(id);
    showToast('Reading deleted.', 'error');
    loadReadings();
}

/* ─── PROJECTION ────────────────────────────────────────── */
$('runProjection').addEventListener('click', runProjection);

async function runProjection() {
    const months = $('projMonths').value;
    const projections = await apiFetch(`/project?months=${months}`).catch(() => []);

    const grid = $('projectionResults');
    const chartCard = $('projChartCard');
    const empty = $('projEmpty');

    if (!projections.length) {
        grid.classList.add('hidden');
        chartCard.style.display = 'none';
        empty.style.display = 'block';
        return;
    }

    empty.style.display = 'none';
    grid.classList.remove('hidden');
    chartCard.style.display = 'block';

    grid.innerHTML = projections.map((p, i) => {
        const changeClass = p.percentChange > 0 ? 'up' : p.percentChange < 0 ? 'down' : 'flat';
        const changeSign = p.percentChange > 0 ? '+' : '';
        return `
        <div class="proj-card" data-trend="${p.trend}" style="animation-delay:${i * 0.05}s">
            <div class="proj-month">${p.month}</div>
            <div class="proj-bill">${fmt(p.projectedBill)}</div>
            <div class="proj-meta">
                ~${fmt(p.projectedKwh)} kWh<br>
                ₱${fmt(p.totalRatePerKwh ?? p.ratePerKwh ?? 0)}/kWh<br>
                ₱${fmt(p.taxesAndOtherCharges ?? 0)} taxes & charges
            </div>
            <span class="proj-change ${changeClass}">${changeSign}${p.percentChange}% vs prev</span>
        </div>`;
    }).join('');

    drawProjChart(projections.map(p => p.month), projections.map(p => p.projectedBill));
}

function drawProjChart(labels, data) {
    if (projChartInst) projChartInst.destroy();
    projChartInst = new Chart($('projChart'), {
        type: 'line',
        data: {
            labels,
            datasets: [{
                data,
                borderColor: '#f5c842',
                backgroundColor: 'rgba(245,200,66,0.08)',
                borderWidth: 2,
                pointBackgroundColor: '#f5c842',
                pointRadius: 5,
                tension: 0.3,
                fill: true,
            }],
        },
        options: {
            ...chartDefaults(),
            scales: {
                ...chartDefaults().scales,
                y: {
                    ...chartDefaults().scales.y,
                    ticks: {
                        ...chartDefaults().scales.y.ticks,
                        callback: v => `₱${v.toLocaleString()}`,
                    },
                },
            },
        },
    });
}

/* ─── INIT ──────────────────────────────────────────────── */
loadDashboard();
