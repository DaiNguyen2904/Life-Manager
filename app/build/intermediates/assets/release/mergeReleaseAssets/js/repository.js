/**
 * Local-first repository — port từ kiến trúc Money-Wise:
 * Entity (Category, Expense/Transaction, Budget, User) + Single Source of Truth.
 * Room/Firestore được thay bằng localStorage để chạy ngay trên trình duyệt.
 */
const STORAGE_KEY = "life-management.v1";

const DEFAULT_EXPENSE_CATEGORIES = [
  { name: "Ăn uống", icon: "🍜", color: "#FF5733" },
  { name: "Phương tiện", icon: "🚗", color: "#33C46B" },
  { name: "Giải trí", icon: "🎬", color: "#4D7CFF" },
  { name: "Nhà ở", icon: "🏠", color: "#FF33A1" },
  { name: "Y tế", icon: "💊", color: "#A133FF" },
  { name: "Học tập", icon: "📚", color: "#33FFF6" },
  { name: "Mỹ phẩm", icon: "💄", color: "#F6C133" },
  { name: "Quần áo", icon: "👕", color: "#FF8C33" },
  { name: "Khác", icon: "📦", color: "#808080" },
];

const DEFAULT_INCOME_CATEGORIES = [
  { name: "Lương", icon: "💼", color: "#3DD68C" },
  { name: "Freelance", icon: "💻", color: "#6EA8FF" },
  { name: "Khác (thu)", icon: "➕", color: "#8B97A8" },
];

function uid(prefix = "id") {
  return `${prefix}_${Math.random().toString(36).slice(2, 10)}_${Date.now().toString(36)}`;
}

function startOfDay(date) {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  return d;
}

function endOfDay(date) {
  const d = new Date(date);
  d.setHours(23, 59, 59, 999);
  return d;
}

function startOfMonth(date) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

function endOfMonth(date) {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0, 23, 59, 59, 999);
}

function isoDate(date) {
  const d = new Date(date);
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${m}-${day}`;
}

function addDays(date, n) {
  const d = new Date(date);
  d.setDate(d.getDate() + n);
  return d;
}

function createSeed() {
  const now = Date.now();
  const today = isoDate(new Date());
  const weekend = isoDate(addDays(new Date(), 6 - new Date().getDay() || 7));

  const user = {
    userId: "local-user",
    email: "khoi@local",
    displayName: "Khôi",
    createdAt: now,
  };

  const expenseCats = DEFAULT_EXPENSE_CATEGORIES.map((c) => ({
    categoryId: uid("cat"),
    userId: user.userId,
    type: "expense",
    name: c.name,
    icon: c.icon,
    color: c.color,
    isDefault: 1,
    createdAt: now,
  }));

  const incomeCats = DEFAULT_INCOME_CATEGORIES.map((c) => ({
    categoryId: uid("cat"),
    userId: user.userId,
    type: "income",
    name: c.name,
    icon: c.icon,
    color: c.color,
    isDefault: 1,
    createdAt: now,
  }));

  const food = expenseCats.find((c) => c.name === "Ăn uống");
  const transport = expenseCats.find((c) => c.name === "Phương tiện");
  const study = expenseCats.find((c) => c.name === "Học tập");
  const salary = incomeCats.find((c) => c.name === "Lương");

  const transactions = [
    {
      expenseId: uid("tx"),
      userId: user.userId,
      categoryId: salary.categoryId,
      type: "income",
      amount: 18000000,
      note: "Lương tháng",
      date: startOfMonth(new Date()).getTime() + 86400000,
      paymentMethod: "Chuyển khoản",
      createdAt: now,
    },
    {
      expenseId: uid("tx"),
      userId: user.userId,
      categoryId: food.categoryId,
      type: "expense",
      amount: 85000,
      note: "Cà phê + bánh",
      date: Date.now() - 3600000,
      paymentMethod: "Tiền mặt",
      createdAt: now,
    },
    {
      expenseId: uid("tx"),
      userId: user.userId,
      categoryId: food.categoryId,
      type: "expense",
      amount: 120000,
      note: "Cơm trưa",
      date: Date.now() - 86400000,
      paymentMethod: "Momo",
      createdAt: now,
    },
    {
      expenseId: uid("tx"),
      userId: user.userId,
      categoryId: transport.categoryId,
      type: "expense",
      amount: 45000,
      note: "Grab đi họp",
      date: Date.now() - 2 * 86400000,
      paymentMethod: "Momo",
      createdAt: now,
    },
    {
      expenseId: uid("tx"),
      userId: user.userId,
      categoryId: study.categoryId,
      type: "expense",
      amount: 350000,
      note: "Khóa học online",
      date: Date.now() - 5 * 86400000,
      paymentMethod: "Thẻ",
      createdAt: now,
    },
  ];

  const budgets = expenseCats.map((c) => ({
    budgetId: uid("bdg"),
    userId: user.userId,
    categoryId: c.categoryId,
    amount:
      c.name === "Ăn uống" ? 3000000 :
      c.name === "Phương tiện" ? 800000 :
      c.name === "Học tập" ? 1000000 :
      c.name === "Giải trí" ? 700000 : 500000,
    createdAt: now,
  }));

  const tasks = [
    { taskId: uid("task"), title: "Họp team 9h sáng", done: true, due: today, priority: "high", status: "done", createdAt: now },
    { taskId: uid("task"), title: "Đọc tài liệu dự án Q2", done: true, due: today, priority: "medium", status: "done", createdAt: now },
    { taskId: uid("task"), title: "Gọi điện cho khách hàng A", done: false, due: today, priority: "high", status: "doing", createdAt: now },
    { taskId: uid("task"), title: "Viết blog post tuần này", done: false, due: today, priority: "low", status: "todo", createdAt: now },
    { taskId: uid("task"), title: "Đi gym buổi tối", done: false, due: today, priority: "medium", status: "todo", createdAt: now },
    { taskId: uid("task"), title: "Họp team cuối tuần", done: false, due: today, priority: "high", status: "todo", createdAt: now },
    { taskId: uid("task"), title: "Báo cáo C2 gửi sếp", done: false, due: isoDate(addDays(new Date(), 2)), priority: "high", status: "doing", createdAt: now },
    { taskId: uid("task"), title: "Lên kế hoạch nghỉ phép tháng 7", done: false, due: isoDate(addDays(new Date(), 5)), priority: "medium", status: "todo", createdAt: now },
    { taskId: uid("task"), title: "Trả lời email anh Tuấn", done: false, due: isoDate(addDays(new Date(), 1)), priority: "medium", status: "todo", createdAt: now },
    { taskId: uid("task"), title: "Review ngân sách tháng", done: false, due: isoDate(addDays(new Date(), 3)), priority: "low", status: "todo", createdAt: now },
    { taskId: uid("task"), title: "Dọn inbox Notion", done: false, due: isoDate(addDays(new Date(), 4)), priority: "low", status: "todo", createdAt: now },
    { taskId: uid("task"), title: "Chuẩn bị slide standup", done: false, due: weekend, priority: "medium", status: "todo", createdAt: now },
    { taskId: uid("task"), title: "Mua quà sinh nhật bạn", done: false, due: isoDate(addDays(new Date(), 8)), priority: "low", status: "todo", createdAt: now },
  ];

  const habits = [
    { habitId: uid("hb"), name: "Uống nước 2L", doneDates: [today] },
    { habitId: uid("hb"), name: "Đọc 20 trang", doneDates: [today] },
    { habitId: uid("hb"), name: "Tập 20 phút", doneDates: [today] },
    { habitId: uid("hb"), name: "Viết nhật ký", doneDates: [today] },
  ];

  const notes = {
    content:
      "✓ Tuần này cần xong:\n" +
      "- Báo cáo C2 (gửi sếp T6)\n" +
      "- Lên kế hoạch nghỉ phép tháng 7\n" +
      "- Trả lời email anh Tuấn\n\n" +
      "💡 Idea hay:\n" +
      "Thử Pomodoro 25/10 thay vì 25/5 cho task deep work.",
    updatedAt: now,
  };

  return {
    user,
    categories: [...expenseCats, ...incomeCats],
    transactions,
    budgets,
    tasks,
    habits,
    notes,
    timeSessions: [],
    pomodoro: { minutes: 25, breakMinutes: 5 },
  };
}

const Repo = {
  load() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        const seed = createSeed();
        this.save(seed);
        return seed;
      }
      return JSON.parse(raw);
    } catch {
      const seed = createSeed();
      this.save(seed);
      return seed;
    }
  },

  save(state) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  },

  mutate(fn) {
    const state = this.load();
    fn(state);
    this.save(state);
    return state;
  },

  reset() {
    localStorage.removeItem(STORAGE_KEY);
    return this.load();
  },
};

window.Repo = Repo;
window.uid = uid;
window.startOfDay = startOfDay;
window.endOfDay = endOfDay;
window.startOfMonth = startOfMonth;
window.endOfMonth = endOfMonth;
window.isoDate = isoDate;
window.addDays = addDays;
