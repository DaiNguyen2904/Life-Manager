package com.example.lifemanager;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

import com.example.lifemanager.data.repository.SettingsRepository;
import com.example.lifemanager.databinding.ActivityMainBinding;
import com.example.lifemanager.notification.NotificationHelper;
import com.example.lifemanager.ui.BaseActivity;
import com.example.lifemanager.ui.auth.LoginActivity;
import com.example.lifemanager.ui.finance.FinanceFragment;
import com.example.lifemanager.ui.home.HomeFragment;
import com.example.lifemanager.ui.report.ReportFragment;
import com.example.lifemanager.ui.settings.SettingsActivity;
import com.example.lifemanager.ui.task.TaskFragment;
import com.example.lifemanager.utils.PrefsManager;
import com.example.lifemanager.utils.SessionManager;
import com.example.lifemanager.utils.ViewUtils;

/**
 * Activity chính: chứa thanh điều hướng dưới cùng và 4 Fragment (Trang chủ, Thu Chi, Công việc, Báo cáo).
 * Fragment được giữ lại khi chuyển tab (show / hide) nên không phải tạo lại Activity hay Fragment.
 */
public class MainActivity extends BaseActivity {

    public static final String EXTRA_OPEN_TAB = "extra_open_tab";

    private static final String KEY_SELECTED_TAB = "selected_tab";
    private static final String TAG_HOME = "tab_home";
    private static final String TAG_FINANCE = "tab_finance";
    private static final String TAG_TASKS = "tab_tasks";
    private static final String TAG_REPORT = "tab_report";
    private static final String[] ALL_TAGS = {TAG_HOME, TAG_FINANCE, TAG_TASKS, TAG_REPORT};

    private ActivityMainBinding binding;
    @IdRes
    private int selectedTabId = R.id.nav_home;

    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private ActivityResultLauncher<Intent> settingsLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SessionManager.getInstance(this).isLoggedIn()) {
            // Chưa đăng nhập: chuyển sang màn hình Đăng nhập (giữ tab cần mở nếu đến từ thông báo)
            startActivity(LoginActivity.newIntent(this, getIntent().getIntExtra(EXTRA_OPEN_TAB, 0)));
            finish();
            return;
        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewUtils.applySystemBarInsets(binding.header, true, false);

        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        SettingsRepository.getInstance(this).refreshReminders();
                    }
                });
        settingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == SettingsActivity.RESULT_DATA_RESET) {
                        // Dữ liệu đã bị xóa: khởi động lại màn hình chính với ViewModel mới
                        Intent restart = new Intent(this, MainActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(restart);
                        finish();
                    } else if (result.getResultCode() == RESULT_OK) {
                        // Định dạng tiền tệ thay đổi: vẽ lại toàn bộ giao diện
                        recreate();
                    } else {
                        updateAvatar();
                    }
                });

        binding.tvAvatar.setOnClickListener(v ->
                settingsLauncher.launch(new Intent(this, SettingsActivity.class)));

        if (savedInstanceState != null) {
            selectedTabId = savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.nav_home);
        } else {
            int requested = getIntent().getIntExtra(EXTRA_OPEN_TAB, 0);
            if (isValidTab(requested)) {
                selectedTabId = requested;
            }
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        binding.bottomNav.setOnItemReselectedListener(item -> {
            // Đang ở tab này: không làm gì
        });
        binding.bottomNav.getMenu().findItem(selectedTabId).setChecked(true);
        showTab(selectedTabId);

        requestNotificationPermissionIfNeeded();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        int requested = intent.getIntExtra(EXTRA_OPEN_TAB, 0);
        if (isValidTab(requested)) {
            navigateTo(requested);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAvatar();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TAB, selectedTabId);
    }

    /** Chuyển sang một tab (gọi từ Fragment, ví dụ nút "Xem tất cả" ở Trang chủ). */
    public void navigateTo(@IdRes int tabId) {
        if (isValidTab(tabId)) {
            binding.bottomNav.setSelectedItemId(tabId);
        }
    }

    private boolean isValidTab(int id) {
        return id == R.id.nav_home || id == R.id.nav_finance || id == R.id.nav_tasks || id == R.id.nav_report;
    }

    private void showTab(@IdRes int tabId) {
        FragmentManager fm = getSupportFragmentManager();
        String targetTag = tagFor(tabId);
        FragmentTransaction transaction = fm.beginTransaction().setReorderingAllowed(true);

        for (String tag : ALL_TAGS) {
            Fragment fragment = fm.findFragmentByTag(tag);
            if (fragment != null && !tag.equals(targetTag)) {
                transaction.hide(fragment);
                // Fragment ẩn dừng ở STARTED (gọi onPause) để không xử lý tương tác
                transaction.setMaxLifecycle(fragment, Lifecycle.State.STARTED);
            }
        }

        Fragment target = fm.findFragmentByTag(targetTag);
        if (target == null) {
            target = createFragment(tabId);
            transaction.add(R.id.fragmentContainer, target, targetTag);
        } else {
            transaction.show(target);
        }
        transaction.setMaxLifecycle(target, Lifecycle.State.RESUMED);
        transaction.commit();

        selectedTabId = tabId;
        binding.tvSubtitle.setText(subtitleFor(tabId));
    }

    @NonNull
    private String tagFor(@IdRes int tabId) {
        if (tabId == R.id.nav_finance) {
            return TAG_FINANCE;
        } else if (tabId == R.id.nav_tasks) {
            return TAG_TASKS;
        } else if (tabId == R.id.nav_report) {
            return TAG_REPORT;
        }
        return TAG_HOME;
    }

    @NonNull
    private Fragment createFragment(@IdRes int tabId) {
        if (tabId == R.id.nav_finance) {
            return new FinanceFragment();
        } else if (tabId == R.id.nav_tasks) {
            return new TaskFragment();
        } else if (tabId == R.id.nav_report) {
            return new ReportFragment();
        }
        return new HomeFragment();
    }

    private int subtitleFor(@IdRes int tabId) {
        if (tabId == R.id.nav_finance) {
            return R.string.subtitle_finance;
        } else if (tabId == R.id.nav_tasks) {
            return R.string.subtitle_tasks;
        } else if (tabId == R.id.nav_report) {
            return R.string.subtitle_report;
        }
        return R.string.subtitle_home;
    }

    private void updateAvatar() {
        binding.tvAvatar.setText(PrefsManager.getInstance(this).getUserInitial());
    }

    /** Xin quyền gửi thông báo trên Android 13+ (chỉ hỏi một lần khi mở ứng dụng). */
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        PrefsManager prefs = PrefsManager.getInstance(this);
        if (prefs.isNotificationsEnabled() && !NotificationHelper.hasPermission(this)
                && !prefs.isNotificationPermissionAsked()) {
            prefs.setNotificationPermissionAsked(true);
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}
