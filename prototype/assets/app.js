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
  // 切换账本：顶栏名称 + 侧边栏账本高亮 + 面包屑 + localStorage
  window.setBook = function (name) {
    // 1) 顶栏切换器名称
    document.querySelectorAll('.book-switcher .name').forEach(n => n.textContent = name);
    // 2) 账本列表/账本管理页的卡片激活态
    document.querySelectorAll('.book-card').forEach(c => {
      const n = c.querySelector('.book-name');
      c.classList.toggle('active', n && n.textContent.trim() === name);
    });
    // 3) 面包屑中的账本名（保留末尾加粗的当前页）
    document.querySelectorAll('.crumb').forEach(cr => {
      // 找到第一个 <b> 节点之前的文本节点并替换；不动 <b>
      const b = cr.querySelector('b');
      const tail = b ? ' / <b>' + b.textContent + '</b>' : '';
      cr.innerHTML = name + tail;
    });
    // 4) localStorage 持久化
    try { localStorage.setItem('jz_book', name); } catch (_) {}
    // 5) 关闭所有账本相关弹窗
    document.querySelectorAll('.modal-mask.open').forEach(m => m.classList.remove('open'));
    // 6) 反馈
    if (window.toast) window.toast('已切换到「' + name + '」', 'success', 1500);
  };

  // -------- 页面初始化时还原账本（仅顶栏） --------
  try {
    const saved = localStorage.getItem('jz_book');
    if (saved) {
      const applyTop = () => {
        document.querySelectorAll('.book-switcher .name').forEach(n => { n.textContent = saved; });
      };
      if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', applyTop);
      } else {
        applyTop();
      }
    }
  } catch (_) {}

  // -------- Active nav highlight --------
  const path = location.pathname.split('/').pop() || 'index.html';
  document.querySelectorAll('.nav-item').forEach(a => {
    if (a.getAttribute('href') === path) a.classList.add('active');
  });
})();
