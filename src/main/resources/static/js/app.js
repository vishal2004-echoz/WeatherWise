const $ = s => document.querySelector(s);

// Sky colours per weather theme: [day, night]
const SKY = {
  clear:  [['#2f7fe0', '#8fcbff'], ['#0a1233', '#2a3a7c']],
  cloudy: [['#5d7188', '#a9b8c8'], ['#161d29', '#364356']],
  fog:    [['#6d7882', '#a3adb5'], ['#1d2329', '#3b444c']],
  rain:   [['#35475c', '#6b8197'], ['#10161f', '#2a3849']],
  snow:   [['#5f7794', '#a9bdd3'], ['#1a2436', '#44587a']],
  storm:  [['#2a2750', '#5a4d88'], ['#0f0d24', '#322a58']]
};
const ICON = {
  clear: ['☀️', '🌙'], cloudy: ['⛅', '☁️'], fog: ['🌫️', '🌫️'],
  rain: ['🌧️', '🌧️'], snow: ['❄️', '❄️'], storm: ['⛈️', '⛈️']
};
const icon = (theme, day) => ICON[theme][day ? 0 : 1];
const dayHour = time => { const h = +time.slice(11, 13); return h >= 6 && h < 18; };
const esc = s => String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

const store = {
  get(k, d) { try { return JSON.parse(localStorage.getItem(k)) ?? d; } catch { return d; } },
  set(k, v) { try { localStorage.setItem(k, JSON.stringify(v)); } catch { /* ignore */ } }
};

const state = { unit: store.get('ww.unit', 'c'), report: null };
let chart = null;

// unit helpers
const t = c => Math.round(state.unit === 'c' ? c : c * 9 / 5 + 32);
const wind = k => state.unit === 'c' ? `${Math.round(k)} km/h` : `${Math.round(k * 0.621371)} mph`;

function setStatus(msg, isError = false) {
  const el = $('#status');
  el.textContent = msg;
  el.className = isError ? 'error' : '';
}

async function load(params) {
  setStatus('Loading weather…');
  $('#app').classList.add('loading');
  try {
    const res = await fetch('/api/weather?' + new URLSearchParams(params));
    const data = await res.json();
    if (!res.ok) throw new Error(data.message || 'Something went wrong. Try again.');
    state.report = data;
    render();
    addRecent(data.city);
    setStatus('');
  } catch (e) {
    setStatus(e.message || 'Could not reach the server.', true);
  } finally {
    $('#app').classList.remove('loading');
  }
}

function render() {
  const r = state.report, c = r.current, today = r.daily[0];
  const [top, bottom] = SKY[c.theme][c.day ? 0 : 1];
  document.documentElement.style.setProperty('--sky-top', top);
  document.documentElement.style.setProperty('--sky-bottom', bottom);
  document.title = `${t(c.temperature)}° ${r.city} · WeatherWise`;

  $('#hero').innerHTML = `
    <h1>${esc(r.city)}</h1>
    <p class="where">${esc(r.country)}</p>
    <div class="now"><span class="icon">${icon(c.theme, c.day)}</span>
      <span class="deg">${t(c.temperature)}<small>°</small></span></div>
    <p class="cond">${esc(c.condition)}, feels like ${t(c.feelsLike)}°</p>
    <p class="time">Local time ${esc(c.time.slice(11, 16))}</p>`;

  const stat = (label, value) => `<div><span>${label}</span><b>${value}</b></div>`;
  $('#stats').innerHTML =
    stat('Humidity', `${c.humidity}%`) +
    stat('Wind', wind(c.windSpeed)) +
    stat('Pressure', `${Math.round(c.pressure)} hPa`) +
    stat('Chance of rain', today.rainChance == null ? '–' : `${today.rainChance}%`) +
    stat('Sunrise', esc(today.sunrise.slice(11, 16))) +
    stat('Sunset', esc(today.sunset.slice(11, 16)));

  $('#strip').innerHTML = r.hourly.map((h, i) => `
    <div><span>${i === 0 ? 'Now' : h.time.slice(11, 16)}</span>
      <i title="${esc(h.condition)}">${icon(h.theme, dayHour(h.time))}</i>
      <b>${t(h.temperature)}°</b></div>`).join('');

  const lo = Math.min(...r.daily.map(d => d.min)), hi = Math.max(...r.daily.map(d => d.max));
  const span = Math.max(hi - lo, 1);
  $('#daily').innerHTML = r.daily.map((d, i) => {
    const name = i === 0 ? 'Today' : new Date(d.date + 'T12:00:00').toLocaleDateString('en', { weekday: 'short' });
    const left = (d.min - lo) / span * 100, width = (d.max - d.min) / span * 100;
    return `<div class="row">
      <span>${name}</span><span>${icon(d.theme, true)}</span>
      <span class="lo">${t(d.min)}°</span>
      <div class="bar"><i style="left:${left}%;width:${Math.max(width, 4)}%"></i></div>
      <span>${t(d.max)}°</span>
      <span class="cond">${esc(d.condition)}${d.rainChance != null ? `, ${d.rainChance}% rain` : ''}</span></div>`;
  }).join('');

  drawChart();
}

function drawChart() {
  if (typeof Chart === 'undefined') return;   // CDN unreachable: skip chart, rest still works
  const hrs = state.report.hourly;
  if (chart) chart.destroy();
  const soft = 'rgba(255,255,255,.75)';
  chart = new Chart($('#chart'), {
    type: 'line',
    data: {
      labels: hrs.map(h => h.time.slice(11, 16)),
      datasets: [{
        data: hrs.map(h => t(h.temperature)),
        borderColor: '#fff', backgroundColor: 'rgba(255,255,255,.15)',
        fill: true, tension: .4, pointRadius: 0, pointHoverRadius: 5, borderWidth: 2
      }]
    },
    options: {
      maintainAspectRatio: false,
      plugins: { legend: { display: false }, tooltip: { callbacks: { label: c => `${c.parsed.y}°` } } },
      scales: {
        x: { ticks: { color: soft, maxTicksLimit: 6 }, grid: { display: false } },
        y: { ticks: { color: soft, callback: v => v + '°' }, grid: { color: 'rgba(255,255,255,.1)' } }
      }
    }
  });
}

// ---- units ----
function syncUnits() {
  document.querySelectorAll('.units button').forEach(b =>
    b.setAttribute('aria-pressed', String(b.dataset.unit === state.unit)));
}
document.querySelectorAll('.units button').forEach(b => b.addEventListener('click', () => {
  state.unit = b.dataset.unit;
  store.set('ww.unit', state.unit);
  syncUnits();
  if (state.report) render();
}));

// ---- recent searches ----
function addRecent(city) {
  const list = [city, ...store.get('ww.recent', []).filter(c => c !== city)].slice(0, 5);
  store.set('ww.recent', list);
  renderRecent();
}
function renderRecent() {
  const list = store.get('ww.recent', []);
  $('#recent').innerHTML = list.map(c => `<button type="button">${esc(c)}</button>`).join('');
  $('#recent').querySelectorAll('button').forEach(b =>
    b.addEventListener('click', () => load({ city: b.textContent })));
}

// ---- search with suggestions ----
const input = $('#q'), box = $('#suggest');
let timer;
const closeSuggest = () => { box.hidden = true; box.innerHTML = ''; };

input.addEventListener('input', () => {
  clearTimeout(timer);
  const q = input.value.trim();
  if (q.length < 2) return closeSuggest();
  timer = setTimeout(async () => {
    try {
      const res = await fetch('/api/places?q=' + encodeURIComponent(q));
      const places = await res.json();
      if (!Array.isArray(places) || !places.length) return closeSuggest();
      box.innerHTML = places.map((p, i) =>
        `<li tabindex="0" data-i="${i}"><span>${esc(p.name)}</span><small>${esc([p.region, p.country].filter(Boolean).join(', '))}</small></li>`).join('');
      box.hidden = false;
      box.querySelectorAll('li').forEach(li => {
        const pick = () => {
          const p = places[li.dataset.i];
          input.value = '';
          closeSuggest();
          load({ lat: p.latitude, lon: p.longitude, name: p.name, country: p.country });
        };
        li.addEventListener('click', pick);
        li.addEventListener('keydown', e => { if (e.key === 'Enter') pick(); });
      });
    } catch { closeSuggest(); }
  }, 250);
});
input.addEventListener('keydown', e => {
  if (e.key === 'Enter' && input.value.trim()) {
    load({ city: input.value.trim() });
    input.value = '';
    closeSuggest();
  }
  if (e.key === 'Escape') closeSuggest();
});
document.addEventListener('click', e => { if (!e.target.closest('.search')) closeSuggest(); });

// ---- start ----
syncUnits();
renderRecent();
load({ city: store.get('ww.recent', [])[0] || 'Visakhapatnam' });
