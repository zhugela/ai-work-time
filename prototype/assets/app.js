/* ===========================================================
 * 个人记账原型 · 通用交互（路由/弹窗/校验/Toast）
 * =========================================================== */
(function () {
  // -------- Theme --------
  function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('jz_theme', theme);
  }
  window.toggleTheme = function () {
    const cur = document.documentElement.getAttribute('data-theme') || 'light';
    applyTheme(cur === 'light' ? 'dark' : 'light');
  };
  const saved = localStorage.getItem('jz_theme');
  if (saved) applyTheme(saved);
  else if (window.matchMedia('(prefers-color-scheme: dark)').matches) applyTheme('dark');

  // -------- Toast --------
  let wrap;
  function ensureWrap() {
    if (!wrap) {
      wrap = document.createElement('div');
      wrap.className = 'toast-wrap';
      document.body.appendChild(wrap);
    }
    return wrap;
  }
  window.toast = function (msg, type = 'info', timeout = 2500) {
    const el = document.createElement('div');
    el.className = 'toast ' + type;
    el.textContent = msg;
    ensureWrap().appendChild(el);
    setTimeout(() => { el.style.opacity = '0'; el.style.transition = 'opacity .2s'; }, timeout - 200);
    setTimeout(() => el.remove(), timeout);
  };

  // -------- Modal --------
  window.openModal = function (id) {
    const m = document.getElementById(id);
    if (m) m.classList.add('open');
  };
  window.closeModal = function (id) {
    const m = document.getElementById(id);
    if (m) m.classList.remove('open');
  };
  document.addEventListener('click', (e) => {
    if (e.target.classList && e.target.classList.contains('modal-mask')) {
      e.target.classList.remove('open');
    }
    if (e.target.dataset && e.target.dataset.close) {
      closeModal(e.target.dataset.close);
    }
  });
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      document.querySelectorAll('.modal-mask.open').forEach(m => m.classList.remove('open'));
    }
  });

  // -------- Form validation helper --------
  window.validateForm = function (formId, rules) {
    const form = document.getElementById(formId);
    if (!form) return true;
    let ok = true;
    rules.forEach(r => {
      const el = form.querySelector('[name="' + r.name + '"]');
      if (!el) return;
      const val = (el.value || '').trim();
      const errEl = form.querySelector('.err-msg[data-for="' + r.name + '"]');
      let msg = '';
      if (r.required && !val) msg = r.label + '不能为空';
      else if (r.minLen && val.length < r.minLen) msg = r.label + '至少 ' + r.minLen + ' 位';
      else if (r.maxLen && val.length > r.maxLen) msg = r.label + '不超过 ' + r.maxLen + ' 位';
      else if (r.pattern && !r.pattern.test(val)) msg = r.label + '格式不正确';
      else if (r.equal && val !== form.querySelector('[name="' + r.equal + '"]').value) msg = r.label + '两次输入不一致';
      if (msg) {
        el.classList.add('err');
        if (errEl) errEl.textContent = msg;
        ok = false;
      } else {
        el.classList.remove('err');
        if (errEl) errEl.textContent = '';
      }
    });
    return ok;
  };

  // -------- Book switcher --------
  window.setBook = function (name) {
    document.querySelectorAll('.book-switcher .name').forEach(n => n.textContent = name);
    localStorage.setItem('jz_book', name);
  };

  // -------- Active nav highlight --------
  const path = location.pathname.split('/').pop() || 'index.html';
  document.querySelectorAll('.nav-item').forEach(a => {
    if (a.getAttribute('href') === path) a.classList.add('active');
  });
})();
