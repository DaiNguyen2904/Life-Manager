const WEEKDAYS = ["Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"];
const MONTHS = [
  "tháng 1", "tháng 2", "tháng 3", "tháng 4", "tháng 5", "tháng 6",
  "tháng 7", "tháng 8", "tháng 9", "tháng 10", "tháng 11", "tháng 12",
];

const routes = {
  dashboard: renderDashboard,
  finance: renderFinance,
  tasks: renderTasks,
  time: renderTime,
  notes: renderNotes,
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
    ["time", "Thời Gian"],
    ["notes", "Ghi chú"],
  ];
  return `
    <nav class="nav-mini">
      ${items.map(([id, label]) => `<a href="#/${id}" class="${active === id ? "active" : ""}">${label}</a>`).join("")}
      <button type="button" id="reset-data">Đặt lại dữ liệu mẫu</button>
    </nav>
  `;
}

function topClock() {
  const { clock, date } = nowParts();
  const line = `${clock} · ${WEEKDAYS[date.getDay()]}, ${date.getDate()} ${MONTHS[date.getMonth()]}, ${date.getFullYear()}`;
  return `<div class="clock-line" id="live-clock">${escapeHtml(line)}</div>`;
}

function layout(active, body) {
  return `
    <header class="topbar">
      ${topClock()}
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
    <p class="subtitle">Hôm nay là một ngày mới — chọn một mảng để bắt đầu, hoặc xem nhanh tóm tắt bên dưới.</p>

    <div class="grid-2">
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
          <span class="kicker">Tuần này</span>
          <span class="hint">Tuần ${wk}</span>
        </div>
        <div class="big">${wp}%</div>
        <div class="hint">Đã trôi qua trong tuần</div>
        <div class="progress"><span style="width:${wp}%"></span></div>
      </section>
    </div>

    <div class="section-title">Tổng quan nhanh</div>
    <div class="grid-4">
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
        <div class="stat">${remain}</div>
        <div class="hint">ngày · ${noteLen} ký tự note</div>
      </section>
    </div>

    <div class="section-title">Hệ thống quản lý</div>
    <div class="grid-4">
      <a class="card module accent-yellow" href="#/finance">
        <span class="arrow">→</span>
        <div class="icon-wrap">💰</div>
        <h3>Thu Chi</h3>
        <p>Sổ tài chính cá nhân: theo dõi thu nhập, chi tiêu, ngân sách và tiết kiệm.</p>
        <div class="pills">
          <span class="pill warn">Có số liệu</span>
          <span class="pill muted">Dư ${vnd(income - expense)}</span>
        </div>
      </a>
      <a class="card module accent-green" href="#/tasks">
        <span class="arrow">→</span>
        <div class="icon-wrap">✅</div>
        <h3>Công Việc</h3>
        <p>To-do, kanban board, deadline, priority — quản lý task hàng ngày và dự án.</p>
        <div class="pills">
          <span class="pill">${openTasks.length} đang mở</span>
          <span class="pill muted">${todayTasks.filter((t) => !t.done).length} hết hạn hôm nay</span>
        </div>
      </a>
      <a class="card module accent-orange" href="#/time">
        <span class="arrow">→</span>
        <div class="icon-wrap">⏰</div>
        <h3>Thời Gian</h3>
        <p>Time blocking, pomodoro, lịch tuần, theo dõi thời gian và năng suất.</p>
        <div class="pills">
          <span class="pill warn">${sessionsToday.length} hôm nay</span>
          <span class="pill muted">${focusMin} phút tập trung</span>
        </div>
      </a>
      <a class="card module accent-mint" href="#/notes">
        <span class="arrow">→</span>
        <div class="icon-wrap">📝</div>
        <h3>Ghi Chú Nhanh</h3>
        <p>Ghi nhận mọi ý tưởng, chi tiết công việc, tự lưu liền tay.</p>
        <div class="pills">
          <span class="pill">Luôn sẵn sàng</span>
          <span class="pill muted">${noteLen} ký tự</span>
        </div>
      </a>
    </div>

    <div class="section-title"></div>
    <div class="grid-split">
      <section class="card notes-box">
        <div class="row-between">
          <strong>📝 Ghi chú nhanh</strong>
          <span class="hint">Tự lưu trong trình duyệt</span>
        </div>
        <textarea id="quick-note">${escapeHtml(state.notes.content)}</textarea>
        <div class="meta-row">
          <span>🟢 Đã lưu</span>
          <a class="linkish" href="#/notes">Xem hết</a>
        </div>
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
    </div>
    ${banner}
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
      <div style="margin-bottom:12px">
        <div class="row-between"><span>${c.icon} ${escapeHtml(c.name)}</span><span class="hint">${vnd(spent)} / ${vnd(budget)}</span></div>
        <div class="progress ${klass}"><span style="width:${pct}%"></span></div>
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
          <td><button class="btn danger" data-del-tx="${t.expenseId}">Xóa</button></td>
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
        <p class="subtitle">Port từ mô hình Money-Wise: danh mục, giao dịch, ngân sách theo tháng.</p>
      </div>
    </div>
    <div class="grid-4">
      <section class="card"><div class="kicker">Thu tháng này</div><div class="stat amount-in">${vnd(income)}</div></section>
      <section class="card"><div class="kicker">Chi tháng này</div><div class="stat amount-out">${vnd(expense)}</div></section>
      <section class="card"><div class="kicker">Còn lại</div><div class="stat">${vnd(income - expense)}</div></section>
      <section class="card"><div class="kicker">Giao dịch</div><div class="stat">${state.transactions.length}</div></section>
    </div>
    <div class="section-title">Thêm giao dịch</div>
    <section class="card">
      <form id="add-tx" class="stack">
        <div class="grid-4">
          <div><label>Loại</label><select name="type"><option value="expense">Chi</option><option value="income">Thu</option></select></div>
          <div><label>Danh mục</label><select name="categoryId">${catOptions}</select></div>
          <div><label>Số tiền</label><input name="amount" type="number" min="0" step="1000" required /></div>
          <div><label>Ngày</label><input name="date" type="date" value="${isoDate(new Date())}" required /></div>
        </div>
        <div class="grid-2">
          <div><label>Ghi chú</label><input name="note" type="text" placeholder="Nội dung" /></div>
          <div><label>Thanh toán</label><input name="paymentMethod" type="text" placeholder="Momo, tiền mặt..." /></div>
        </div>
        <button class="btn primary" type="submit">Lưu giao dịch</button>
      </form>
    </section>
    <div class="grid-split" style="margin-top:14px">
      <section class="card">
        <strong>Ngân sách danh mục (tháng)</strong>
        <div style="margin-top:12px">${catRows}</div>
      </section>
      <section class="card">
        <strong>Giao dịch gần đây</strong>
        <table class="table">
          <thead><tr><th>Ngày</th><th>Danh mục</th><th>Ghi chú</th><th>Số tiền</th><th></th></tr></thead>
          <tbody>${rows || '<tr><td colspan="5" class="empty">Chưa có giao dịch.</td></tr>'}</tbody>
        </table>
      </section>
    </div>
  `);
}

function renderTasks(state) {
  const cols = [
    ["todo", "Cần làm"],
    ["doing", "Đang làm"],
    ["done", "Hoàn thành"],
  ];
  const boards = cols.map(([status, title]) => {
    const items = state.tasks.filter((t) => (t.status || (t.done ? "done" : "todo")) === status);
    return `
      <section class="card kanban-col">
        <div class="row-between"><strong>${title}</strong><span class="hint">${items.length}</span></div>
        ${items.map((t) => `
          <article class="task-card">
            <div>${escapeHtml(t.title)}</div>
            <small>Hạn ${t.due || "—"} · ${t.priority}</small>
            <div class="pills" style="margin-top:8px">
              ${status !== "todo" ? `<button class="btn" data-task-status="${t.taskId}" data-status="todo">Todo</button>` : ""}
              ${status !== "doing" ? `<button class="btn" data-task-status="${t.taskId}" data-status="doing">Doing</button>` : ""}
              ${status !== "done" ? `<button class="btn" data-task-status="${t.taskId}" data-status="done">Done</button>` : ""}
              <button class="btn danger" data-del-task="${t.taskId}">Xóa</button>
            </div>
          </article>
        `).join("")}
      </section>
    `;
  }).join("");

  return layout("tasks", `
    <div class="page-head">
      <div>
        <h1>Công Việc</h1>
        <p class="subtitle">Kanban, hạn chót và độ ưu tiên — kéo tiến độ bằng một click.</p>
      </div>
    </div>
    <section class="card">
      <form id="add-task" class="stack">
        <div class="grid-4">
          <div><label>Tiêu đề</label><input name="title" required placeholder="Việc cần làm" /></div>
          <div><label>Hạn</label><input name="due" type="date" value="${isoDate(new Date())}" /></div>
          <div><label>Ưu tiên</label>
            <select name="priority"><option value="low">Thấp</option><option value="medium" selected>Trung bình</option><option value="high">Cao</option></select>
          </div>
          <div style="align-self:end"><button class="btn primary" type="submit">Thêm task</button></div>
        </div>
      </form>
    </section>
    <div class="kanban" style="margin-top:14px">${boards}</div>
  `);
}

function formatMmSs(total) {
  const m = Math.floor(total / 60);
  const s = total % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

function renderTime(state) {
  const today = isoDate(new Date());
  const sessions = state.timeSessions.filter((s) => s.date === today);
  const total = sessions.reduce((a, s) => a + s.minutes, 0);
  if (!pomodoroRunning && pomodoroRemaining === 25 * 60) {
    pomodoroRemaining = (state.pomodoro?.minutes || 25) * 60;
  }

  return layout("time", `
    <div class="page-head">
      <div>
        <h1>Thời Gian</h1>
        <p class="subtitle">Pomodoro để tập trung sâu, ghi nhận phiên làm việc trong ngày.</p>
      </div>
    </div>
    <div class="grid-split">
      <section class="card timer">
        <div class="kicker">Pomodoro</div>
        <div class="digits" id="pomo-digits">${formatMmSs(pomodoroRemaining)}</div>
        <div class="hint">${pomodoroRunning ? "Đang chạy" : "Sẵn sàng"} · ${state.pomodoro.minutes} phút / nghỉ ${state.pomodoro.breakMinutes} phút</div>
        <div class="timer-actions">
          <button class="btn primary" id="pomo-toggle">${pomodoroRunning ? "Tạm dừng" : "Bắt đầu"}</button>
          <button class="btn" id="pomo-reset">Reset</button>
          <button class="btn" id="pomo-log">Ghi phiên hiện tại</button>
        </div>
      </section>
      <section class="card">
        <strong>Phiên hôm nay</strong>
        <div class="stat">${total} phút</div>
        <div class="hint">${sessions.length} phiên tập trung</div>
        ${sessions.map((s) => `<div class="habit-row"><span>${s.label || "Pomodoro"}</span><span class="hint">${s.minutes}p · ${s.time}</span></div>`).join("") || '<div class="empty">Chưa có phiên nào.</div>'}
      </section>
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

function currentRoute() {
  const hash = (location.hash || "#/dashboard").replace(/^#\/?/, "");
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

function bind(state, route) {
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
