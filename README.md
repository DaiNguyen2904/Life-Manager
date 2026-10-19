# Life Management - Ứng dụng Android Quản lý Cá nhân

Ứng dụng Android quản lý đời sống cá nhân hiện đại kết hợp kiến trúc Hybrid (Native Android Java + Local Web App), mang lại trải nghiệm mượt mà, trực quan và tối ưu cho điện thoại.

## 🎯 Phân tích & Xác định Mục tiêu Dự án

- **Vấn đề thực tế**: Người dùng hiện đại thường phải sử dụng nhiều ứng dụng rời rạc để quản lý tài chính, lập kế hoạch công việc, theo dõi thói quen và ghi chú cá nhân, gây bất tiện và thiếu đồng bộ dữ liệu.
- **Mục tiêu dự án**: Xây dựng một hệ sinh thái quản lý đời sống cá nhân khép kín, trực quan và tối ưu trên nền tảng Android.
- **Định hướng giải pháp**: 
  - Tích hợp đa phân hệ (Thu chi, Công việc, Thói quen, Báo cáo thống kê) trong giao diện kéo vuốt ngang hiện đại.
  - Đảm bảo bảo mật và cá nhân hóa với hệ thống xác thực tài khoản Google thực tế.
  - Tối ưu hiệu năng ngoại tuyến với SQLite cục bộ và hỗ trợ đồng bộ đám mây thời gian thực qua Firestore.

## 🌟 Các Tính năng & Công nghệ Nổi bật

1. **Xác thực Google Thực tế (Credential Manager & Google Play Services Auth)**:
   - Đăng nhập bảo mật bằng tài khoản Google/Gmail thật thông qua Android Credential Manager API và Google Account Picker.
   - Hiển thị tên tài khoản Google chính xác của người dùng trên màn hình xin chào và menu thông tin tài khoản (góc phải trên).

2. **Lưu trữ Cục bộ SQLite & Đồng bộ Real-time (Firestore)**:
   - Tích hợp **SQLite Database** (`DatabaseHelper.java`) đảm bảo hiệu năng và tính ổn định ngoại tuyến (offline-first).
   - Sẵn sàng đồng bộ dữ liệu thời gian thực giữa các thiết bị qua **Firebase Firestore**.

3. **Phân hệ Quản lý Toàn diện**:
   - **Tổng quan (Dashboard)**: Trang chủ tích hợp sẵn lịch ngày, danh sách việc hôm nay, thói quen và khung ghi chú nhanh tự động lưu.
   - **Thu Chi**: Sổ tài chính cá nhân, quản lý giao dịch, ngân sách theo tháng.
   - **Báo Cáo Thống Kê**: Giao diện báo cáo chuyên nghiệp gồm **Tỷ trọng Chi tiêu** (phân bổ danh mục) và **So sánh Ngân sách** (biểu đồ so sánh Đã chi vs Ngân sách).
   - **Công Việc**: Quản lý task với bảng Kanban (Cần làm, Đang làm, Hoàn thành) cùng nút dấu cộng (`+`) nổi (FAB) mở hộp thoại thêm task.

## 📱 Chạy ứng dụng Android bằng Android Studio

1. Mở **Android Studio** → **Open** → chọn thư mục `Life-Management`.
2. Chờ Gradle Sync hoàn tất (SDK 35 / MinSdk 24).
3. Chọn thiết bị giả lập (Emulator) hoặc điện thoại Android thật đã bật USB Debugging.
4. Nhấn nút **Run** (Shift + F10) để cài đặt và trải nghiệm ứng dụng `Life Management`.

## 🗂 Cấu trúc Mã nguồn

- **Tầng Native Android (Java)**:
  - `app/src/main/java/com/life/management/ui/MainActivity.java`: Quản lý WebView, Credential Manager Google Sign-In & JavaScript Bridge.
  - `app/src/main/java/com/life/management/data/DatabaseHelper.java`: Quản lý cơ sở dữ liệu SQLite cục bộ.
- **Tầng Giao diện & Logic (Assets)**:
  - `app/src/main/assets/index.html`: Khung khởi chạy ứng dụng (tích hợp Google Identity Services).
  - `app/src/main/assets/js/app.js`: Quản lý router, render các trang vuốt ngang, biểu đồ báo cáo và sự kiện.
  - `app/src/main/assets/js/repository.js`: Local-first repository & quản lý state.
  - `app/src/main/assets/css/styles.css`: Giao diện Light Theme, horizontal swiper & modal dialogs.
