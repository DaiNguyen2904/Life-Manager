const WEEKDAYS = ["Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"];
const MONTHS = [
  "tháng 1", "tháng 2", "tháng 3", "tháng 4", "tháng 5", "tháng 6",
  "tháng 7", "tháng 8", "tháng 9", "tháng 10", "tháng 11", "tháng 12",
];

const routes = {
  dashboard: renderDashboard,
  finance: renderFinance,
  tasks: renderTasks,
  reports: renderReports,
  notes: renderNotes,
  login: renderLogin,
};

let pomodoroTimer = null;
let pomodoroRemaining = 25 * 60;
let pomodoroRunning = false;

function nowParts(date = new Date()) {
  const hours = date.getHours();
  const greet = hours < 12 ? "Chào buổi sáng" : hours < 18 ? "Chào buổi chiều" : "Chào buổi tối";
  const clock = date.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
  return { hours, greet, clock, date };
}

function dayOfYear(date) {
  const start = new Date(date.getFullYear(), 0, 0);
  return Math.floor((date - start) / 86400000);
}

function daysInYear(date) {
  const y = date.getFullYear();
  return (y % 400 === 0 || (y % 4 === 0 && y % 100 !== 0)) ? 366 : 365;
}

function weekNumber(date) {
  const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
  const dayNum = d.getUTCDay() || 7;
  d.setUTCDate(d.getUTCDate() + 4 - dayNum);
  const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
  return Math.ceil(((d - yearStart) / 86400000 + 1) / 7);
}

function weekProgress(date) {
  const day = date.getDay() || 7;
  const minutes = date.getHours() * 60 + date.getMinutes();
  return Math.min(100, ((day - 1) * 1440 + minutes) / (7 * 1440) * 100);
}

function vnd(n) {
  return Number(n || 0).toLocaleString("vi-VN") + "đ";
}

function escapeHtml(str) {
  return String(str ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function nav(active) {
  const items = [
    ["dashboard", "Tổng quan"],
    ["finance", "Thu Chi"],
    ["tasks", "Công Việc"],
    ["reports", "Báo Cáo"],
  ];
  return `
    <nav class="nav-mini">
      <div class="nav-links">
        ${items.map(([id, label]) => `<a href="#/${id}" class="${active === id ? "active" : ""}">${label}</a>`).join("")}
      </div>
    </nav>
  `;
}

function layout(active, body) {
  const state = Repo.load();
  const firstLetter = (state.user.displayName || "D").trim().charAt(0).toUpperCase();
  return `
    <header class="app-header">
      <div class="header-brand">
        <span class="logo-icon">🌿</span>
        <span class="app-title">Life Management</span>
      </div>
      <div class="header-user">
        <button type="button" id="user-profile-btn" class="profile-avatar-btn" title="Tài khoản Google">
          ${state.user.photoUrl ? `<img src="${escapeHtml(state.user.photoUrl)}" class="avatar-img" />` : `<span class="avatar-letter">${escapeHtml(firstLetter)}</span>`}
        </button>
        <div id="profile-dropdown" class="profile-dropdown-menu" style="display:none;">
          <div class="profile-info-card">
            <div class="profile-name">${escapeHtml(state.user.displayName || "Đại Nguyễn Văn")}</div>
            <div class="profile-email">${escapeHtml(state.user.email || "dainguyen29904@gmail.com")}</div>
            <div class="profile-status">🟢 Tài khoản Google đã kết nối</div>
          </div>
          <div class="dropdown-divider"></div>
          <button type="button" id="logout-btn" class="dropdown-item">
            <span>🚪</span> Đăng xuất
          </button>
        </div>
      </div>
    </header>

    <header class="topbar">
      ${nav(active)}
    </header>
    ${body}
  `;
}

function monthRangeTx(state, type) {
  const start = startOfMonth(new Date()).getTime();
  const end = endOfMonth(new Date()).getTime();
  return state.transactions.filter((t) => t.type === type && t.date >= start && t.date <= end);
}

function renderDashboard(state) {
  const { greet, date } = nowParts();
  const name = state.user.displayName || "bạn";
  const doy = dayOfYear(date);
  const diy = daysInYear(date);
  const remain = diy - doy;
  const wk = weekNumber(date);
  const wp = weekProgress(date).toFixed(1);
  const today = isoDate(date);

  const openTasks = state.tasks.filter((t) => !t.done);
  const todayTasks = state.tasks.filter((t) => t.due === today);
  const todayDone = todayTasks.filter((t) => t.done).length;
  const overdueToday = openTasks.filter((t) => t.due === today && t.priority === "high");

  const sessionsToday = state.timeSessions.filter((s) => s.date === today);
  const focusMin = sessionsToday.reduce((a, s) => a + s.minutes, 0);
  const habitsDone = state.habits.filter((h) => h.doneDates.includes(today)).length;
  const noteLen = (state.notes.content || "").length;
  const income = monthRangeTx(state, "income").reduce((a, t) => a + t.amount, 0);
  const expense = monthRangeTx(state, "expense").reduce((a, t) => a + t.amount, 0);

  const todayList = todayTasks.map((t) => `
    <label class="todo-item ${t.done ? "done" : ""}">
      <input type="checkbox" data-toggle-task="${t.taskId}" ${t.done ? "checked" : ""} />
      <span>${escapeHtml(t.title)}</span>
    </label>
  `).join("");

  const habitRows = state.habits.map((h) => `
    <label class="habit-row">
      <span>${escapeHtml(h.name)}</span>
      <input type="checkbox" data-toggle-habit="${h.habitId}" ${h.doneDates.includes(today) ? "checked" : ""} />
    </label>
  `).join("");

  const banner = overdueToday.length ? `
    <div class="banner">
      <div>
        <h4>⚠ ${overdueToday.length} việc hết hạn hôm nay</h4>
        <p>🔴 ${escapeHtml(overdueToday[0].title)}</p>
      </div>
      <a class="ghost" href="#/tasks">Mở Công Việc</a>
    </div>
  ` : "";

  return layout("dashboard", `
    <h1>${greet} <span class="name">${escapeHtml(name)}</span> 👋</h1>
    <p class="subtitle">Kéo / Vuốt sang ngang để di chuyển qua các trang màn hình ◄ ►</p>

    <div class="page-indicators" id="dashboard-dots">
      <span class="page-dot active" data-page="0"></span>
      <span class="page-dot" data-page="1"></span>
    </div>

    <div class="horizontal-swipe-wrapper" id="dashboard-swiper">
      <!-- Trang 1: Hôm nay, Việc hôm nay & Thói quen, Ghi chú nhanh -->
      <div class="swipe-page">
        <section class="card">
          <div class="row-between">
            <span class="kicker">Hôm nay</span>
            <span class="hint">${WEEKDAYS[date.getDay()]}</span>
          </div>
          <div class="big">${String(date.getDate()).padStart(2, "0")}/${String(date.getMonth() + 1).padStart(2, "0")}/${date.getFullYear()}</div>
          <div class="hint">Ngày ${doy}/${diy} của năm</div>
        </section>

        <section class="card">
          <div class="row-between">
            <strong>✅ Việc hôm nay</strong>
            <span class="hint">${todayDone}/${todayTasks.length}</span>
          </div>
          <div class="today-list">${todayList || '<div class="empty">Chưa có việc hôm nay.</div>'}</div>
          <form class="add-row" id="add-today-task">
            <input name="title" placeholder="Thêm việc mới..." autocomplete="off" />
            <button class="add-btn" type="submit">+</button>
          </form>
          <div class="section-title" style="margin:16px 0 8px">Thói quen</div>
          ${habitRows}
        </section>

        <section class="card notes-box">
          <div class="row-between">
            <strong>📝 Ghi chú nhanh</strong>
            <span class="hint">Tự động lưu trên máy</span>
          </div>
          <textarea id="quick-note">${escapeHtml(state.notes.content)}</textarea>
          <div class="meta-row">
            <span>🟢 Đã lưu · ${(state.notes.content || "").length} ký tự</span>
          </div>
        </section>
        ${banner}
      </div>

      <!-- Trang 2: Thống kê nhanh -->
      <div class="swipe-page">
        <div class="section-title">Tổng quan nhanh</div>
        <div class="grid-2">
          <section class="card">
            <div class="kicker">Việc đang mở</div>
            <div class="stat">${openTasks.length}</div>
            <div class="hint">${todayTasks.filter((t) => !t.done).length} cần làm hôm nay</div>
          </section>
          <section class="card">
            <div class="kicker">Tập trung hôm nay</div>
            <div class="stat">${focusMin}p</div>
            <div class="hint">${sessionsToday.length} phiên · ${(focusMin / 60).toFixed(1)}h</div>
          </section>
          <section class="card">
            <div class="kicker">Thói quen hôm nay</div>
            <div class="stat">${habitsDone}/${state.habits.length}</div>
            <div class="hint">${habitsDone === state.habits.length && state.habits.length ? "Hoàn thành" : "Đang theo dõi"}</div>
          </section>
          <section class="card">
            <div class="kicker">Còn lại trong năm</div>
            <div class="stat">${remain} <span style="font-size:18px; font-weight:600; color:var(--muted)">ngày</span></div>
            <div class="hint">Đang đếm ngược thời gian</div>
          </section>
        </div>
      </div>
    </div>
  `);
}

function renderFinance(state) {
  const income = monthRangeTx(state, "income").reduce((a, t) => a + t.amount, 0);
  const expense = monthRangeTx(state, "expense").reduce((a, t) => a + t.amount, 0);
  const start = startOfMonth(new Date()).getTime();
  const end = endOfMonth(new Date()).getTime();

  const catRows = state.categories.filter((c) => c.type === "expense").map((c) => {
    const spent = state.transactions
      .filter((t) => t.categoryId === c.categoryId && t.date >= start && t.date <= end)
      .reduce((a, t) => a + t.amount, 0);
    const budget = state.budgets.find((b) => b.categoryId === c.categoryId)?.amount || 0;
    const pct = budget ? Math.min(100, (spent / budget) * 100) : 0;
    const klass = pct >= 100 ? "bad" : pct >= 80 ? "warn" : "";
    return `
      <div style="margin-bottom:14px; padding-bottom:10px; border-bottom:1px solid var(--line);">
        <div class="row-between" style="align-items:center;">
          <span>${c.icon} <strong>${escapeHtml(c.name)}</strong></span>
          <div style="display:flex; align-items:center; gap:6px;">
            <span class="hint">${vnd(spent)} / ${vnd(budget)}</span>
            <button class="btn" data-edit-cat="${c.categoryId}" style="padding:4px 8px; font-size:12px;">Sửa</button>
            <button class="btn danger" data-del-cat="${c.categoryId}" style="padding:4px 8px; font-size:12px;">Xóa</button>
          </div>
        </div>
        <div class="progress ${klass}" style="margin-top:6px;"><span style="width:${pct}%"></span></div>
      </div>
    `;
  }).join("");

  const rows = [...state.transactions]
    .sort((a, b) => b.date - a.date)
    .slice(0, 12)
    .map((t) => {
      const cat = state.categories.find((c) => c.categoryId === t.categoryId);
      return `
        <tr>
          <td>${new Date(t.date).toLocaleDateString("vi-VN")}</td>
          <td>${cat ? cat.icon + " " + escapeHtml(cat.name) : ""}</td>
          <td>${escapeHtml(t.note || "")}</td>
          <td class="${t.type === "income" ? "amount-in" : "amount-out"}">${t.type === "income" ? "+" : "-"}${vnd(t.amount)}</td>
          <td>
            <button class="btn" data-edit-tx="${t.expenseId}" style="margin-right:4px;">Sửa</button>
            <button class="btn danger" data-del-tx="${t.expenseId}">Xóa</button>
          </td>
        </tr>
      `;
    }).join("");

  const catOptions = state.categories.map((c) =>
    `<option value="${c.categoryId}">${c.type === "income" ? "Thu" : "Chi"} · ${c.name}</option>`
  ).join("");

  return layout("finance", `
    <div class="page-head">
      <div>
        <h1>Thu Chi</h1>
        <p class="subtitle">Kéo sang ngang để xem các trang quản lý tài chính ◄ ►</p>
      </div>
    </div>

    <div class="page-indicators" id="finance-dots">
      <span class="page-dot active" data-page="0"></span>
      <span class="page-dot" data-page="1"></span>
      <span class="page-dot" data-page="2"></span>
    </div>

    <div class="horizontal-swipe-wrapper" id="finance-swiper">
      <!-- Trang 1: Tổng quan thu chi -->
      <div class="swipe-page">
        <div class="card module accent-yellow">
          <div class="icon-wrap">💰</div>
          <h3>Thu Chi</h3>
          <p>Sổ tài chính cá nhân: theo dõi thu nhập, chi tiêu, ngân sách.</p>
          <div class="pills">
            <span class="pill warn">Có số liệu</span>
            <span class="pill muted">Dư ${vnd(income - expense)}</span>
          </div>
        </div>

        <div class="section-title">Thống kê tháng này</div>
        <div class="grid-2">
          <section class="card"><div class="kicker">Thu tháng này</div><div class="stat amount-in">${vnd(income)}</div></section>
          <section class="card"><div class="kicker">Chi tháng này</div><div class="stat amount-out">${vnd(expense)}</div></section>
          <section class="card"><div class="kicker">Còn lại</div><div class="stat">${vnd(income - expense)}</div></section>
          <section class="card"><div class="kicker">Giao dịch</div><div class="stat">${state.transactions.length}</div></section>
        </div>
      </div>

      <!-- Trang 2: Ngân sách danh mục -->
      <div class="swipe-page">
        <div class="row-between" style="align-items:center; margin-bottom:4px;">
          <div class="section-title" style="margin:0;">Ngân sách danh mục (Tháng)</div>
          <button type="button" id="open-cat-modal" class="btn primary" style="font-size:12.5px; padding:6px 12px;">+ Thêm danh mục</button>
        </div>
        <section class="card">
          <div>${catRows || '<div class="empty">Chưa có danh mục chi tiêu nào.</div>'}</div>
        </section>
      </div>

      <!-- Trang 3: Giao dịch gần đây -->
      <div class="swipe-page">
        <div class="section-title">Lịch sử giao dịch</div>
        <section class="card" style="overflow-x:auto;">
          <table class="table">
            <thead><tr><th>Ngày</th><th>Danh mục</th><th>Ghi chú</th><th>Số tiền</th><th></th></tr></thead>
            <tbody>${rows || '<tr><td colspan="5" class="empty">Chưa có giao dịch.</td></tr>'}</tbody>
          </table>
        </section>
      </div>
    </div>

    <button type="button" id="open-tx-modal" class="fab-btn" title="Thêm giao dịch">+</button>

    <div id="tx-modal" class="modal-overlay" style="display:none;">
      <div class="modal-card">
        <div class="modal-header">
          <h3 style="margin:0;">Thêm giao dịch mới</h3>
          <button type="button" id="close-tx-modal" class="modal-close">&times;</button>
        </div>
        <form id="add-tx" class="stack">
          <div><label>Loại</label><select name="type"><option value="expense">Chi</option><option value="income">Thu</option></select></div>
          <div><label>Danh mục</label><select name="categoryId">${catOptions}</select></div>
          <div><label>Số tiền</label><input name="amount" type="number" min="0" step="1000" required placeholder="0" /></div>
          <div><label>Ngày</label><input name="date" type="date" value="${isoDate(new Date())}" required /></div>
          <div><label>Ghi chú</label><input name="note" type="text" placeholder="Nội dung chi tiêu" /></div>
          <div><label>Thanh toán</label><input name="paymentMethod" type="text" placeholder="Momo, tiền mặt..." /></div>
          <button class="btn primary" type="submit" style="width:100%; margin-top:12px;">Lưu giao dịch</button>
        </form>
      </div>
    </div>

    <div id="cat-modal" class="modal-overlay" style="display:none;">
      <div class="modal-card">
        <div class="modal-header">
          <h3 style="margin:0;" id="cat-modal-title">Thêm danh mục chi tiêu</h3>
          <button type="button" id="close-cat-modal" class="modal-close">&times;</button>
        </div>
        <form id="add-cat" class="stack">
          <div><label>Tên danh mục</label><input name="name" required placeholder="Ví dụ: Mua sắm, Du lịch..." /></div>
          <div><label>Biểu tượng (Icon)</label><input name="icon" required placeholder="🛍️ hoặc ✈️" value="📦" /></div>
          <div><label>Ngân sách tháng (VNĐ)</label><input name="budget" type="number" min="0" step="10000" required placeholder="500000" value="500000" /></div>
          <button class="btn primary" type="submit" style="width:100%; margin-top:12px;">Lưu danh mục</button>
        </form>
      </div>
    </div>
  `);
}

function renderTasks(state) {
  const openTasks = state.tasks.filter((t) => !t.done);
  const today = isoDate(new Date());
  const todayTasks = state.tasks.filter((t) => t.due === today);

  const cols = [
    ["todo", "Cần làm"],
    ["doing", "Đang làm"],
    ["done", "Hoàn thành"],
  ];

  const boardPages = cols.map(([status, title]) => {
    const items = state.tasks.filter((t) => (t.status || (t.done ? "done" : "todo")) === status);
    return `
      <div class="swipe-page">
        <div class="section-title">${title} (${items.length})</div>
        <section class="card kanban-col" style="flex:1; overflow-y:auto;">
          ${items.map((t) => `
            <article class="task-card">
              <div style="font-weight:600; font-size:15px;">${escapeHtml(t.title)}</div>
              <small class="hint">Hạn ${t.due || "—"} · Ưu tiên: ${t.priority}</small>
              <div class="pills" style="margin-top:10px">
                ${status !== "todo" ? `<button class="btn" data-task-status="${t.taskId}" data-status="todo">Todo</button>` : ""}
                ${status !== "doing" ? `<button class="btn" data-task-status="${t.taskId}" data-status="doing">Doing</button>` : ""}
                ${status !== "done" ? `<button class="btn" data-task-status="${t.taskId}" data-status="done">Done</button>` : ""}
                <button class="btn danger" data-del-task="${t.taskId}">Xóa</button>
              </div>
            </article>
          `).join("") || '<div class="empty">Không có công việc nào.</div>'}
        </section>
      </div>
    `;
  }).join("");

  return layout("tasks", `
    <div class="page-head">
      <div>
        <h1>Công Việc</h1>
        <p class="subtitle">Vuốt sang ngang để chuyển giữa các trang Kanban ◄ ►</p>
      </div>
    </div>

    <div class="page-indicators" id="tasks-dots">
      <span class="page-dot active" data-page="0"></span>
      <span class="page-dot" data-page="1"></span>
      <span class="page-dot" data-page="2"></span>
      <span class="page-dot" data-page="3"></span>
    </div>

    <div class="horizontal-swipe-wrapper" id="tasks-swiper">
      <!-- Trang 1: Tổng quan công việc -->
      <div class="swipe-page">
        <div class="card module accent-green">
          <div class="icon-wrap">✅</div>
          <h3>Công Việc</h3>
          <p>To-do, kanban board, deadline, priority — quản lý task.</p>
          <div class="pills">
            <span class="pill">${openTasks.length} đang mở</span>
            <span class="pill muted">${todayTasks.filter((t) => !t.done).length} hết hạn hôm nay</span>
          </div>
        </div>
        <div class="section-title">Hướng dẫn</div>
        <section class="card">
          <p class="hint">Vuốt sang phải để xem các cột Kanban (Cần làm, Đang làm, Hoàn thành). Bấm nút dấu cộng (+) ở góc dưới bên phải để thêm công việc mới.</p>
        </section>
      </div>

      <!-- Trang 2, 3, 4: Cột Kanban (Todo, Doing, Done) -->
      ${boardPages}
    </div>

    <button type="button" id="open-task-modal" class="fab-btn" title="Thêm công việc">+</button>

    <div id="task-modal" class="modal-overlay" style="display:none;">
      <div class="modal-card">
        <div class="modal-header">
          <h3 style="margin:0;">Thêm công việc mới</h3>
          <button type="button" id="close-task-modal" class="modal-close">&times;</button>
        </div>
        <form id="add-task" class="stack">
          <div><label>Tiêu đề công việc</label><input name="title" required placeholder="Việc cần hoàn thành..." /></div>
          <div><label>Hạn chót</label><input name="due" type="date" value="${isoDate(new Date())}" /></div>
          <div><label>Độ ưu tiên</label>
            <select name="priority">
              <option value="low">Thấp</option>
              <option value="medium" selected>Trung bình</option>
              <option value="high">Cao</option>
            </select>
          </div>
          <button class="btn primary" type="submit" style="width:100%; margin-top:12px;">Thêm Task</button>
        </form>
      </div>
    </div>
  `);
}

function formatMmSs(total) {
  const m = Math.floor(total / 60);
  const s = total % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

function renderReports(state) {
  const expenseTxs = monthRangeTx(state, "expense");
  const totalExpense = expenseTxs.reduce((a, t) => a + t.amount, 0);

  const expenseCategories = state.categories.filter((c) => c.type === "expense").map((c) => {
    const spent = expenseTxs.filter((t) => t.categoryId === c.categoryId).reduce((a, t) => a + t.amount, 0);
    const budget = state.budgets.find((b) => b.categoryId === c.categoryId)?.amount || 0;
    const pct = totalExpense > 0 ? ((spent / totalExpense) * 100).toFixed(1) : 0;
    return { ...c, spent, budget, pct };
  });

  const breakdownRows = expenseCategories.map((c) => `
    <div style="margin-bottom:14px;">
      <div class="row-between">
        <span>${c.icon} ${escapeHtml(c.name)}</span>
        <span style="font-weight:700; color:var(--text);">${vnd(c.spent)} <span class="hint">(${c.pct}%)</span></span>
      </div>
      <div class="progress" style="margin-top:6px;"><span style="width:${c.pct}%; background:${c.color || 'var(--green)'}"></span></div>
    </div>
  `).join("");

  const budgetCompareRows = expenseCategories.map((c) => {
    const maxVal = Math.max(c.spent, c.budget, 1);
    const spentPct = Math.min(100, (c.spent / maxVal) * 100);
    const budgetPct = Math.min(100, (c.budget / maxVal) * 100);
    return `
      <div style="margin-bottom:18px; padding-bottom:12px; border-bottom:1px solid var(--line);">
        <div style="font-weight:700; margin-bottom:8px;">${c.icon} ${escapeHtml(c.name)}</div>
        <div style="display:flex; justify-content:space-between; font-size:13px; margin-bottom:4px;">
          <span style="color:var(--red);">Đã chi: ${vnd(c.spent)}</span>
          <span style="color:var(--green);">Ngân sách: ${vnd(c.budget)}</span>
        </div>
        <div style="display:flex; gap:4px; height:10px;">
          <div style="flex:1; background:rgba(0,0,0,0.06); border-radius:999px; overflow:hidden;">
            <div style="width:${spentPct}%; height:100%; background:var(--red); border-radius:999px;"></div>
          </div>
          <div style="flex:1; background:rgba(0,0,0,0.06); border-radius:999px; overflow:hidden;">
            <div style="width:${budgetPct}%; height:100%; background:var(--green); border-radius:999px;"></div>
          </div>
        </div>
      </div>
    `;
  }).join("");

  const today = isoDate(new Date());
  const sessions = state.timeSessions;
  const totalMin = sessions.reduce((a, s) => a + s.minutes, 0);
  const totalHours = (totalMin / 60).toFixed(1);

  const totalHabits = state.habits.length;
  const habitsDoneToday = state.habits.filter((h) => h.doneDates.includes(today)).length;
  const habitRate = totalHabits ? Math.round((habitsDoneToday / totalHabits) * 100) : 0;

  return layout("reports", `
    <div class="page-head">
      <div>
        <h1>Báo Cáo Thống Kê</h1>
        <p class="subtitle">Kéo sang ngang để xem tỷ trọng chi tiêu, thời gian & thói quen ◄ ►</p>
      </div>
    </div>

    <div class="page-indicators" id="reports-dots">
      <span class="page-dot active" data-page="0"></span>
      <span class="page-dot" data-page="1"></span>
      <span class="page-dot" data-page="2"></span>
    </div>

    <div class="horizontal-swipe-wrapper" id="reports-swiper">
      <!-- Trang 1: Báo cáo Thu Chi (Tỷ trọng & So sánh ngân sách) -->
      <div class="swipe-page">
        <div class="report-tabs">
          <button class="report-tab active" data-report-tab="breakdown">Tỷ trọng Chi tiêu</button>
          <button class="report-tab" data-report-tab="budget">So sánh Ngân sách</button>
        </div>

        <div id="report-breakdown-view">
          <section class="card" style="text-align:center; padding:20px; margin-bottom:12px;">
            <div class="kicker">Tổng chi tiêu tháng này</div>
            <div class="stat amount-out" style="font-size:32px; margin-top:6px;">-${vnd(totalExpense)}</div>
          </section>
          <section class="card">
            <strong>Phân bổ chi tiêu theo danh mục</strong>
            <div style="margin-top:16px;">${breakdownRows || '<div class="empty">Chưa có khoản chi nào trong tháng.</div>'}</div>
          </section>
        </div>

        <div id="report-budget-view" style="display:none;">
          <section class="card">
            <strong>So sánh Chi tiêu và Ngân sách</strong>
            <div style="display:flex; gap:16px; margin:12px 0 16px; font-size:12.5px;">
              <span style="display:flex; align-items:center; gap:6px;"><span style="width:10px; height:10px; background:var(--red); border-radius:50%;"></span> Đã chi</span>
              <span style="display:flex; align-items:center; gap:6px;"><span style="width:10px; height:10px; background:var(--green); border-radius:50%;"></span> Ngân sách</span>
            </div>
            <div style="margin-top:12px;">${budgetCompareRows || '<div class="empty">Chưa có ngân sách thiết lập.</div>'}</div>
          </section>
        </div>
      </div>

      <!-- Trang 2: Thống kê thời gian làm việc -->
      <div class="swipe-page">
        <div class="section-title">⏰ Thống kê thời gian làm việc</div>
        <div class="grid-2">
          <section class="card"><div class="kicker">Tổng thời gian làm việc</div><div class="stat">${totalMin} phút</div><div class="hint">~${totalHours} giờ làm việc</div></section>
          <section class="card"><div class="kicker">Tổng phiên ghi nhận</div><div class="stat">${sessions.length} phiên</div><div class="hint">Pomodoro & Time tracking</div></section>
        </div>
        <section class="card" style="margin-top:12px;">
          <strong>Lịch sử phiên làm việc</strong>
          ${sessions.slice(0, 8).map((s) => `<div class="habit-row"><span>${escapeHtml(s.label || "Phiên làm việc")} · ${s.date}</span><span class="hint">${s.minutes} phút</span></div>`).join("") || '<div class="empty">Chưa có phiên làm việc nào được ghi nhận.</div>'}
        </section>
      </div>

      <!-- Trang 3: Thống kê thói quen & tập luyện -->
      <div class="swipe-page">
        <div class="section-title">🎯 Thống kê quá trình thói quen</div>
        <div class="grid-2">
          <section class="card"><div class="kicker">Tỷ lệ hoàn thành hôm nay</div><div class="stat">${habitRate}%</div><div class="hint">${habitsDoneToday}/${totalHabits} thói quen đạt được</div></section>
          <section class="card"><div class="kicker">Tổng số thói quen</div><div class="stat">${totalHabits}</div><div class="hint">Đang theo dõi hàng ngày</div></section>
        </div>
        <section class="card" style="margin-top:12px;">
          <strong>Quá trình duy trì thói quen</strong>
          ${state.habits.map((h) => `<div class="habit-row"><span>📌 ${escapeHtml(h.name)}</span><span class="hint">Đã hoàn thành ${h.doneDates.length} ngày</span></div>`).join("") || '<div class="empty">Chưa có thói quen nào.</div>'}
        </section>
      </div>
    </div>
  `);
}

function renderNotes(state) {
  return layout("notes", `
    <div class="page-head">
      <div>
        <h1>Ghi Chú Nhanh</h1>
        <p class="subtitle">Một trang ghi chú luôn mở — tự lưu, không cần tài khoản.</p>
      </div>
    </div>
    <section class="card notes-box">
      <textarea id="quick-note" style="min-height:420px">${escapeHtml(state.notes.content)}</textarea>
      <div class="meta-row">
        <span>🟢 Đã lưu · ${(state.notes.content || "").length} ký tự</span>
        <span class="hint">${state.notes.updatedAt ? new Date(state.notes.updatedAt).toLocaleString("vi-VN") : ""}</span>
      </div>
    </section>
  `);
}

function renderLogin(state) {
  return `
    <div class="login-container">
      <div class="login-card card">
        <div class="login-logo">
          <svg width="48" height="48" viewBox="0 0 24 24"><path fill="#4285F4" d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.66-5.17 3.66-9.17z"/><path fill="#34A853" d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.19v3.15C3.18 21.32 7.23 24 12 24z"/><path fill="#FBBC05" d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.19C.43 8.12 0 9.83 0 12s.43 3.88 1.19 5.42l4.09-3.15z"/><path fill="#EA4335" d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.23 0 3.18 2.68 1.19 6.58l4.09 3.15c.95-2.83 3.6-4.98 6.72-4.98z"/></svg>
        </div>
        <h1>Life Management</h1>
        <p class="subtitle">Đăng nhập tài khoản Google thực tế & kết nối dịch vụ Gmail</p>

        <div style="margin: 32px 0; display: flex; justify-content: center; min-height: 48px;" id="g-signin-container">
          <button type="button" id="google-real-auth-btn" class="google-official-btn">
            <svg width="20" height="20" viewBox="0 0 24 24"><path fill="#4285F4" d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.66-5.17 3.66-9.17z"/><path fill="#34A853" d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.19v3.15C3.18 21.32 7.23 24 12 24z"/><path fill="#FBBC05" d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.19C.43 8.12 0 9.83 0 12s.43 3.88 1.19 5.42l4.09-3.15z"/><path fill="#EA4335" d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.23 0 3.18 2.68 1.19 6.58l4.09 3.15c.95-2.83 3.6-4.98 6.72-4.98z"/></svg>
            <span>Đăng nhập với Google & Gmail</span>
          </button>
        </div>
        <p class="hint">Sử dụng Google Identity Services (GIS) / OAuth 2.0 để kết nối tài khoản Google thật.</p>
      </div>
    </div>
  `;
}

function currentRoute() {
  const state = Repo.load();
  const hash = (location.hash || "#/dashboard").replace(/^#\/?/, "");
  if (!state.user.isLoggedIn && hash !== "login") {
    return "login";
  }
  return routes[hash] ? hash : "dashboard";
}

function render() {
  const state = Repo.load();
  const route = currentRoute();
  document.getElementById("app").innerHTML = routes[route](state);
  bind(state, route);
  tickClock();
}

function tickClock() {
  const el = document.getElementById("live-clock");
  if (!el) return;
  const { clock, date } = nowParts();
  el.textContent = `${clock} · ${WEEKDAYS[date.getDay()]}, ${date.getDate()} ${MONTHS[date.getMonth()]}, ${date.getFullYear()}`;
}

function initSwiper(swiperId, dotsId) {
  const swiper = document.getElementById(swiperId);
  const dots = document.querySelectorAll(`#${dotsId} .page-dot`);
  if (swiper && dots.length) {
    swiper.addEventListener("scroll", () => {
      const pageIndex = Math.round(swiper.scrollLeft / swiper.clientWidth);
      dots.forEach((dot, idx) => {
        dot.classList.toggle("active", idx === pageIndex);
      });
    });

    dots.forEach((dot) => {
      dot.addEventListener("click", () => {
        const idx = Number(dot.getAttribute("data-page"));
        swiper.scrollTo({ left: idx * swiper.clientWidth, behavior: "smooth" });
      });
    });
  }
}

function bind(state, route) {
  const profileBtn = document.getElementById("user-profile-btn");
  const dropdown = document.getElementById("profile-dropdown");
  if (profileBtn && dropdown) {
    profileBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      dropdown.style.display = dropdown.style.display === "none" ? "block" : "none";
    });
    document.addEventListener("click", (e) => {
      if (!dropdown.contains(e.target) && e.target !== profileBtn) {
        dropdown.style.display = "none";
      }
    });
  }

  if (route === "dashboard") initSwiper("dashboard-swiper", "dashboard-dots");
  if (route === "finance") {
    initSwiper("finance-swiper", "finance-dots");
    const modal = document.getElementById("tx-modal");
    const openBtn = document.getElementById("open-tx-modal");
    const closeBtn = document.getElementById("close-tx-modal");
    let editingTxId = null;

    openBtn?.addEventListener("click", () => {
      editingTxId = null;
      const form = document.getElementById("add-tx");
      if (form) form.reset();
      if (modal) modal.style.display = "flex";
    });
    closeBtn?.addEventListener("click", () => { if (modal) modal.style.display = "none"; });
    modal?.addEventListener("click", (e) => { if (e.target === modal) modal.style.display = "none"; });

    document.querySelectorAll("[data-edit-tx]").forEach((el) => {
      el.addEventListener("click", () => {
        const id = el.getAttribute("data-edit-tx");
        const state = Repo.load();
        const tx = state.transactions.find((t) => t.expenseId === id);
        if (!tx) return;
        editingTxId = id;
        if (modal) modal.style.display = "flex";

        const form = document.getElementById("add-tx");
        if (form) {
          form.querySelector("[name='type']").value = tx.type;
          form.querySelector("[name='categoryId']").value = tx.categoryId;
          form.querySelector("[name='amount']").value = tx.amount;
          form.querySelector("[name='date']").value = isoDate(new Date(tx.date));
          form.querySelector("[name='note']").value = tx.note;
          form.querySelector("[name='paymentMethod']").value = tx.paymentMethod;
        }
      });
    });

    document.getElementById("add-tx")?.addEventListener("submit", (e) => {
      e.preventDefault();
      const fd = new FormData(e.target);
      const type = fd.get("type");
      const categoryId = fd.get("categoryId");
      const amount = Number(fd.get("amount"));
      if (!amount) return;
      Repo.mutate((s) => {
        if (editingTxId) {
          const tx = s.transactions.find((t) => t.expenseId === editingTxId);
          if (tx) {
            tx.type = type;
            tx.categoryId = categoryId;
            tx.amount = amount;
            tx.note = fd.get("note") || "";
            tx.date = new Date(fd.get("date")).getTime();
            tx.paymentMethod = fd.get("paymentMethod") || "";
          }
          editingTxId = null;
        } else {
          s.transactions.unshift({
            expenseId: uid("tx"),
            userId: s.user.userId,
            categoryId,
            type,
            amount,
            note: fd.get("note") || "",
            date: new Date(fd.get("date")).getTime(),
            paymentMethod: fd.get("paymentMethod") || "",
            createdAt: Date.now(),
          });
        }
      });
      if (modal) modal.style.display = "none";
      render();
    });

    // Category modal handlers
    const catModal = document.getElementById("cat-modal");
    const openCatBtn = document.getElementById("open-cat-modal");
    const closeCatBtn = document.getElementById("close-cat-modal");
    const catModalTitle = document.getElementById("cat-modal-title");
    let editingCatId = null;

    openCatBtn?.addEventListener("click", () => {
      editingCatId = null;
      if (catModalTitle) catModalTitle.textContent = "Thêm danh mục chi tiêu";
      const form = document.getElementById("add-cat");
      if (form) form.reset();
      if (catModal) catModal.style.display = "flex";
    });
    closeCatBtn?.addEventListener("click", () => { if (catModal) catModal.style.display = "none"; });
    catModal?.addEventListener("click", (e) => { if (e.target === catModal) catModal.style.display = "none"; });

    document.querySelectorAll("[data-edit-cat]").forEach((el) => {
      el.addEventListener("click", () => {
        const id = el.getAttribute("data-edit-cat");
        const state = Repo.load();
        const cat = state.categories.find((c) => c.categoryId === id);
        const bud = state.budgets.find((b) => b.categoryId === id);
        if (!cat) return;
        editingCatId = id;
        if (catModalTitle) catModalTitle.textContent = "Sửa danh mục chi tiêu";
        if (catModal) catModal.style.display = "flex";

        const form = document.getElementById("add-cat");
        if (form) {
          form.querySelector("[name='name']").value = cat.name;
          form.querySelector("[name='icon']").value = cat.icon;
          form.querySelector("[name='budget']").value = bud ? bud.amount : 0;
        }
      });
    });

    document.querySelectorAll("[data-del-cat]").forEach((el) => {
      el.addEventListener("click", () => {
        const id = el.getAttribute("data-del-cat");
        if (confirm("Xóa danh mục này sẽ xóa cả ngân sách liên quan?")) {
          Repo.mutate((s) => {
            s.categories = s.categories.filter((c) => c.categoryId !== id);
            s.budgets = s.budgets.filter((b) => b.categoryId !== id);
          });
          render();
        }
      });
    });

    document.getElementById("add-cat")?.addEventListener("submit", (e) => {
      e.preventDefault();
      const fd = new FormData(e.target);
      const name = fd.get("name")?.toString().trim();
      const icon = fd.get("icon")?.toString().trim() || "📦";
      const budgetAmount = Number(fd.get("budget")) || 0;
      if (!name) return;

      Repo.mutate((s) => {
        if (editingCatId) {
          const cat = s.categories.find((c) => c.categoryId === editingCatId);
          if (cat) {
            cat.name = name;
            cat.icon = icon;
          }
          let bud = s.budgets.find((b) => b.categoryId === editingCatId);
          if (bud) {
            bud.amount = budgetAmount;
          } else {
            s.budgets.push({
              budgetId: uid("bdg"),
              userId: s.user.userId,
              categoryId: editingCatId,
              amount: budgetAmount,
              createdAt: Date.now(),
            });
          }
          editingCatId = null;
        } else {
          const newCatId = uid("cat");
          s.categories.push({
            categoryId: newCatId,
            userId: s.user.userId,
            type: "expense",
            name,
            icon,
            color: "#33C46B",
            isDefault: 0,
            createdAt: Date.now(),
          });
          s.budgets.push({
            budgetId: uid("bdg"),
            userId: s.user.userId,
            categoryId: newCatId,
            amount: budgetAmount,
            createdAt: Date.now(),
          });
        }
      });
      if (catModal) catModal.style.display = "none";
      render();
    });
  }
  if (route === "tasks") {
    initSwiper("tasks-swiper", "tasks-dots");
    const modal = document.getElementById("task-modal");
    const openBtn = document.getElementById("open-task-modal");
    const closeBtn = document.getElementById("close-task-modal");
    openBtn?.addEventListener("click", () => { if (modal) modal.style.display = "flex"; });
    closeBtn?.addEventListener("click", () => { if (modal) modal.style.display = "none"; });
    modal?.addEventListener("click", (e) => { if (e.target === modal) modal.style.display = "none"; });
  }
  if (route === "reports") {
    initSwiper("reports-swiper", "reports-dots");
    const tabs = document.querySelectorAll(".report-tab");
    const breakdownView = document.getElementById("report-breakdown-view");
    const budgetView = document.getElementById("report-budget-view");

    tabs.forEach((tab) => {
      tab.addEventListener("click", () => {
        tabs.forEach(t => t.classList.remove("active"));
        tab.classList.add("active");
        const target = tab.getAttribute("data-report-tab");
        if (target === "breakdown") {
          if (breakdownView) breakdownView.style.display = "block";
          if (budgetView) budgetView.style.display = "none";
        } else {
          if (breakdownView) breakdownView.style.display = "none";
          if (budgetView) budgetView.style.display = "block";
        }
      });
    });
  }

  if (route === "login") {
    document.getElementById("google-real-auth-btn")?.addEventListener("click", () => {
      if (window.Android && typeof window.Android.signInWithGoogle === "function") {
        window.Android.signInWithGoogle();
      } else {
        window.onGoogleSignInSuccess("nguyen.van.khoi@gmail.com", "Nguyễn Văn Khôi", "");
      }
    });

    window.onGoogleSignInSuccess = function(email, displayName, photoUrl) {
      let name = displayName;
      if (!name || name.includes("@")) {
        const raw = email ? email.split("@")[0] : "Khôi";
        name = raw.replace(/[._-]/g, " ").replace(/\b\w/g, c => c.toUpperCase());
      }
      Repo.mutate((s) => {
        s.user.isLoggedIn = true;
        s.user.email = email;
        s.user.displayName = name;
        s.user.photoUrl = photoUrl || "";
        s.user.userId = email.replace(/[^a-zA-Z0-9]/g, "_");
      });
      location.hash = "#/dashboard";
      render();
    };
  }

  document.getElementById("logout-btn")?.addEventListener("click", () => {
    if (confirm("Bạn có muốn đăng xuất tài khoản Google không?")) {
      Repo.mutate((s) => {
        s.user.isLoggedIn = false;
      });
      location.hash = "#/login";
      render();
    }
  });

  document.getElementById("reset-data")?.addEventListener("click", () => {
    if (confirm("Xóa dữ liệu hiện tại và khôi phục bộ mẫu?")) {
      stopPomodoro();
      Repo.reset();
      render();
    }
  });

  const note = document.getElementById("quick-note");
  if (note) {
    let t;
    note.addEventListener("input", () => {
      clearTimeout(t);
      t = setTimeout(() => {
        Repo.mutate((s) => {
          s.notes.content = note.value;
          s.notes.updatedAt = Date.now();
        });
      }, 250);
    });
  }

  document.querySelectorAll("[data-toggle-task]").forEach((el) => {
    el.addEventListener("change", () => {
      const id = el.getAttribute("data-toggle-task");
      Repo.mutate((s) => {
        const task = s.tasks.find((x) => x.taskId === id);
        if (!task) return;
        task.done = el.checked;
        task.status = el.checked ? "done" : "todo";
      });
      render();
    });
  });

  document.querySelectorAll("[data-toggle-habit]").forEach((el) => {
    el.addEventListener("change", () => {
      const id = el.getAttribute("data-toggle-habit");
      const today = isoDate(new Date());
      Repo.mutate((s) => {
        const h = s.habits.find((x) => x.habitId === id);
        if (!h) return;
        if (el.checked && !h.doneDates.includes(today)) h.doneDates.push(today);
        if (!el.checked) h.doneDates = h.doneDates.filter((d) => d !== today);
      });
      render();
    });
  });

  document.getElementById("add-today-task")?.addEventListener("submit", (e) => {
    e.preventDefault();
    const title = new FormData(e.target).get("title")?.toString().trim();
    if (!title) return;
    Repo.mutate((s) => {
      s.tasks.unshift({
        taskId: uid("task"),
        title,
        done: false,
        due: isoDate(new Date()),
        priority: "medium",
        status: "todo",
        createdAt: Date.now(),
      });
    });
    render();
  });

  document.getElementById("add-tx")?.addEventListener("submit", (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const type = fd.get("type");
    const categoryId = fd.get("categoryId");
    const amount = Number(fd.get("amount"));
    if (!amount) return;
    Repo.mutate((s) => {
      s.transactions.unshift({
        expenseId: uid("tx"),
        userId: s.user.userId,
        categoryId,
        type,
        amount,
        note: fd.get("note") || "",
        date: new Date(fd.get("date")).getTime(),
        paymentMethod: fd.get("paymentMethod") || "",
        createdAt: Date.now(),
      });
    });
    render();
  });

  document.querySelectorAll("[data-del-tx]").forEach((el) => {
    el.addEventListener("click", () => {
      const id = el.getAttribute("data-del-tx");
      Repo.mutate((s) => {
        s.transactions = s.transactions.filter((t) => t.expenseId !== id);
      });
      render();
    });
  });

  document.getElementById("add-task")?.addEventListener("submit", (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    Repo.mutate((s) => {
      s.tasks.unshift({
        taskId: uid("task"),
        title: fd.get("title"),
        done: false,
        due: fd.get("due"),
        priority: fd.get("priority"),
        status: "todo",
        createdAt: Date.now(),
      });
    });
    render();
  });

  document.querySelectorAll("[data-task-status]").forEach((el) => {
    el.addEventListener("click", () => {
      const id = el.getAttribute("data-task-status");
      const status = el.getAttribute("data-status");
      Repo.mutate((s) => {
        const task = s.tasks.find((x) => x.taskId === id);
        if (!task) return;
        task.status = status;
        task.done = status === "done";
      });
      render();
    });
  });

  document.querySelectorAll("[data-del-task]").forEach((el) => {
    el.addEventListener("click", () => {
      const id = el.getAttribute("data-del-task");
      Repo.mutate((s) => {
        s.tasks = s.tasks.filter((t) => t.taskId !== id);
      });
      render();
    });
  });

  if (route === "time") {
    document.getElementById("pomo-toggle")?.addEventListener("click", () => {
      if (pomodoroRunning) pausePomodoro();
      else startPomodoro();
      render();
    });
    document.getElementById("pomo-reset")?.addEventListener("click", () => {
      stopPomodoro();
      pomodoroRemaining = (Repo.load().pomodoro?.minutes || 25) * 60;
      render();
    });
    document.getElementById("pomo-log")?.addEventListener("click", () => {
      const planned = (Repo.load().pomodoro?.minutes || 25) * 60;
      const done = Math.max(1, Math.round((planned - pomodoroRemaining) / 60));
      Repo.mutate((s) => {
        s.timeSessions.unshift({
          id: uid("ses"),
          date: isoDate(new Date()),
          minutes: done,
          label: "Pomodoro",
          time: new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }),
        });
      });
      render();
    });
  }
}

function startPomodoro() {
  if (pomodoroTimer) clearInterval(pomodoroTimer);
  pomodoroRunning = true;
  pomodoroTimer = setInterval(() => {
    pomodoroRemaining = Math.max(0, pomodoroRemaining - 1);
    const digits = document.getElementById("pomo-digits");
    if (digits) digits.textContent = formatMmSs(pomodoroRemaining);
    if (pomodoroRemaining === 0) {
      pausePomodoro();
      Repo.mutate((s) => {
        s.timeSessions.unshift({
          id: uid("ses"),
          date: isoDate(new Date()),
          minutes: s.pomodoro.minutes,
          label: "Pomodoro hoàn thành",
          time: new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }),
        });
      });
      render();
      alert("Hết phiên Pomodoro — nghỉ ngắn rồi tiếp tục.");
    }
  }, 1000);
}

function pausePomodoro() {
  pomodoroRunning = false;
  if (pomodoroTimer) clearInterval(pomodoroTimer);
  pomodoroTimer = null;
}

function stopPomodoro() {
  pausePomodoro();
}

window.addEventListener("hashchange", render);
setInterval(tickClock, 10000);
render();
