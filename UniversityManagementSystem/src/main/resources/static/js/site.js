// Shared by every page: talking to the API, the CSRF token, and the header.
const Site = (() => {
  let csrf = null;
  let currentUser = null;


  // SecurityConfig, AccountUserDetailsService
  // options: method, json (sent as JSON), form (sent as form fields).
  async function request(path, options = {}) {
    const method = options.method || 'GET';
    if (method !== 'GET' && !csrf) {
      csrf = await request('/api/auth/csrf');
    }
    const headers = {};
    let body;
    if (options.json !== undefined) {
      headers['Content-Type'] = 'application/json';
      body = JSON.stringify(options.json);
    } else if (options.form) {
      body = new URLSearchParams(options.form);
    }
    if (method !== 'GET') {
      headers[csrf.headerName] = csrf.token;
    }

    const response = await fetch(path, { method, headers, body });
    if (!response.ok) {
      const reply = await response.json().catch(() => ({}));
      const error = new Error(reply.errors ? reply.errors.join('; ') : reply.message || 'Request failed');
      error.status = response.status;
      throw error;
    }
    return response.status === 204 ? null : response.json();
  }

  // Spring gives a new CSRF token after login and logout, so forget the old one.
  async function login(email, password) {
    await request('/api/auth/login', { method: 'POST', form: { username: email, password } });
    csrf = null;
  }

  async function logout() {
    await request('/api/auth/logout', { method: 'POST' });
    csrf = null;
    location.assign('/');
  }

  // Finds out who is signed in and sets up the header. Resolves to the user, or null.
  async function loadUser() {
    try {
      currentUser = await request('/api/auth/me');
    } catch (error) {
      if (error.status !== 401) throw error;
      currentUser = null;
    }
    showUser();
    return currentUser;
  }

  function showUser() {
    const signedIn = Boolean(currentUser);
    document.getElementById('signin-link').hidden = signedIn;
    document.getElementById('profile-link').hidden = !signedIn;
    document.getElementById('logout-button').hidden = !signedIn;
    const label = document.getElementById('signed-in-as');
    label.hidden = !signedIn;
    label.textContent = signedIn ? `${currentUser.name}, ${roleName(currentUser.role)}` : '';
  }

  function user() {
    return currentUser;
  }

  function roleName(role) {
    return role[0] + role.slice(1).toLowerCase();
  }

  // "Dr. Tahmina Uddin" becomes "TU".
  function initials(name) {
    return name.replace(/^Dr\.\s*/, '').split(/\s+/).slice(0, 2).map(word => word[0]).join('').toUpperCase();
  }

  // Builds an element. Text always goes in as text, so nothing a user typed can run as HTML.
  function el(tag, text, className) {
    const node = document.createElement(tag);
    if (text !== undefined && text !== null) node.textContent = text;
    if (className) node.className = className;
    return node;
  }

  document.getElementById('logout-button').addEventListener('click', () => {
    logout().catch(error => alert(error.message));
  });

  return { request, login, loadUser, user, roleName, initials, el };
})();
