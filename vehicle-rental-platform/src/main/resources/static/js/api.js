// ---------- API wrapper ----------
const API_BASE = '/api';

function authHeaders() {
  const token = localStorage.getItem('rs_token');
  return token ? { 'Authorization': 'Bearer ' + token } : {};
}

async function api(path, { method = 'GET', body, auth = false } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (auth) Object.assign(headers, authHeaders());

  const res = await fetch(API_BASE + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  let data = null;
  const text = await res.text();
  if (text) {
    try { data = JSON.parse(text); } catch (e) { data = text; }
  }

  if (!res.ok) {
    const message = (data && data.message) ? data.message : `Request failed (${res.status})`;
    throw new Error(message);
  }
  return data;
}

// ---------- Auth state ----------
function getCurrentUser() {
  const raw = localStorage.getItem('rs_user');
  return raw ? JSON.parse(raw) : null;
}

function isLoggedIn() {
  return !!localStorage.getItem('rs_token');
}

function hasRole(role) {
  const user = getCurrentUser();
  return !!user && Array.isArray(user.roles) && user.roles.includes(role);
}

function saveSession(authResponse) {
  localStorage.setItem('rs_token', authResponse.token);
  localStorage.setItem('rs_user', JSON.stringify({
    userId: authResponse.userId,
    fullName: authResponse.fullName,
    email: authResponse.email,
    roles: authResponse.roles,
  }));
}

function logout() {
  localStorage.removeItem('rs_token');
  localStorage.removeItem('rs_user');
  window.location.href = '/index.html';
}

// ---------- Shared nav ----------
function renderNav(activePage) {
  const mount = document.getElementById('nav-mount');
  if (!mount) return;

  const user = getCurrentUser();
  let rightLinks = '';

  if (user) {
    const dashLinks = [];
    if (hasRole('OWNER')) dashLinks.push('<a href="/owner.html">Owner dashboard</a>');
    if (hasRole('RENTER')) dashLinks.push('<a href="/renter.html">My bookings</a>');
    rightLinks = `
      ${dashLinks.join('')}
      <span style="color:var(--concrete-dim); font-size:0.85rem;">${escapeHtml(user.fullName)}</span>
      <a href="#" id="logout-link" class="btn btn-primary pill-btn btn-sm">Log out</a>
    `;
  } else {
    rightLinks = `
      <a href="/login.html">Log in</a>
      <a href="/register.html" class="btn btn-primary pill-btn btn-sm">Sign up</a>
    `;
  }

  mount.innerHTML = `
    <nav class="nav">
      <div class="nav-inner">
        <a href="/index.html" class="brand"><span class="brand-mark"></span>ROADSHARE</a>
        <div class="nav-links">
          <a href="/index.html">Browse vehicles</a>
          ${rightLinks}
        </div>
      </div>
    </nav>
  `;

  const logoutLink = document.getElementById('logout-link');
  if (logoutLink) {
    logoutLink.addEventListener('click', (e) => { e.preventDefault(); logout(); });
  }
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str == null ? '' : String(str);
  return div.innerHTML;
}

function formatMoney(value) {
  const n = Number(value);
  return '\u20B9' + n.toLocaleString('en-IN', { maximumFractionDigits: 0 });
}

function showAlert(mountId, message, type = 'error') {
  const mount = document.getElementById(mountId);
  if (!mount) return;
  mount.innerHTML = `<div class="alert alert-${type === 'error' ? 'error' : 'success'}">${escapeHtml(message)}</div>`;
}

function clearAlert(mountId) {
  const mount = document.getElementById(mountId);
  if (mount) mount.innerHTML = '';
}
