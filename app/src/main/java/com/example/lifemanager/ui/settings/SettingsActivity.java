package com.example.lifemanager.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.example.lifemanager.BuildConfig;
import com.example.lifemanager.R;
import com.example.lifemanager.databinding.ActivitySettingsBinding;
import com.example.lifemanager.databinding.DialogTextInputBinding;
import com.example.lifemanager.notification.NotificationHelper;
import com.example.lifemanager.notification.ReminderScheduler;
import com.example.lifemanager.ui.BaseActivity;
import com.example.lifemanager.ui.auth.LoginActivity;
import com.example.lifemanager.utils.CurrencyUtils;
import com.example.lifemanager.utils.DateUtils;
import com.example.lifemanager.utils.PrefsManager;
import com.example.lifemanager.utils.ViewUtils;
import com.example.lifemanager.viewmodel.SettingsViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

/**
 * Màn hình Cài đặt: tên hiển thị, thông báo, giờ nhắc nhở, định dạng tiền tệ, xóa dữ liệu, thông tin ứng dụng.
 */
public class SettingsActivity extends BaseActivity {

    /** Kết quả trả về khi người dùng đã xóa toàn bộ dữ liệu. */
    public static final int RESULT_DATA_RESET = RESULT_FIRST_USER + 1;

    private ActivitySettingsBinding binding;
    private SettingsViewModel viewModel;
    private PrefsManager prefs;
    private boolean currencyChanged;
    private boolean dataReset;
    private boolean bindingState;

    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewUtils.applySystemBarInsets(binding.root, true, true);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        prefs = viewModel.getPrefs();

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (!granted) {
                Toast.makeText(this, R.string.msg_permission_denied, Toast.LENGTH_LONG).show();
            }
            viewModel.setNotificationsEnabled(prefs.isNotificationsEnabled());
        });

        binding.toolbar.setNavigationOnClickListener(v -> finishWithResult());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishWithResult();
            }
        });

        setupProfile();
        setupNotifications();
        setupCurrency();
        setupData();
        binding.tvVersion.setText(getString(R.string.settings_version,
                BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        viewModel.isResetting().observe(this, resetting ->
                binding.progressReset.setVisibility(Boolean.TRUE.equals(resetting) ? View.VISIBLE : View.GONE));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateExactAlarmRow();
    }

    private void finishWithResult() {
        if (dataReset) {
            setResult(RESULT_DATA_RESET);
        } else if (currencyChanged) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    // ------------------------------------------------------------------ Hồ sơ

    private void setupProfile() {
        binding.tvUserName.setText(prefs.getUserName());
        String username = viewModel.getUsername();
        binding.tvUsername.setText(username == null ? "" : "@" + username);
        binding.rowLogout.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_logout_confirm_title)
                .setMessage(R.string.settings_logout_confirm_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.settings_logout, (dialog, which) -> {
                    binding.rowLogout.setEnabled(false);
                    viewModel.logout(() -> {
                        Toast.makeText(this, R.string.msg_logged_out, Toast.LENGTH_SHORT).show();
                        startActivity(LoginActivity.newIntent(this, 0));
                        finish();
                    });
                })
                .show());
        binding.rowUserName.setOnClickListener(v -> {
            DialogTextInputBinding input = DialogTextInputBinding.inflate(getLayoutInflater());
            input.tilInput.setHint(R.string.settings_user_name);
            input.etInput.setText(prefs.getUserName());
            AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.settings_user_name_title)
                    .setView(input.getRoot())
                    .setPositiveButton(R.string.action_save, null)
                    .setNegativeButton(R.string.action_cancel, null)
                    .create();
            dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
                String name = input.etInput.getText() == null ? "" : input.etInput.getText().toString().trim();
                if (name.isEmpty()) {
                    input.tilInput.setError(getString(R.string.error_name_required));
                    return;
                }
                viewModel.setUserName(name);
                binding.tvUserName.setText(name);
                dialog.dismiss();
            }));
            dialog.show();
        });
    }

    // ------------------------------------------------------------------ Thông báo

    private void setupNotifications() {
        bindingState = true;
        binding.switchNotifications.setChecked(prefs.isNotificationsEnabled());
        bindingState = false;
        binding.switchNotifications.setOnCheckedChangeListener((button, checked) -> {
            if (bindingState) {
                return;
            }
            viewModel.setNotificationsEnabled(checked);
            if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && !NotificationHelper.hasPermission(this)) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
            updateReminderRows();
        });

        binding.tvReminderTime.setText(DateUtils.formatMinutesOfDay(prefs.getDailyReminderMinutes()));
        binding.rowReminderTime.setOnClickListener(v -> {
            int minutes = prefs.getDailyReminderMinutes();
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(minutes / 60)
                    .setMinute(minutes % 60)
                    .setTitleText(R.string.settings_reminder_time)
                    .build();
            picker.addOnPositiveButtonClickListener(b -> {
                int selected = picker.getHour() * 60 + picker.getMinute();
                viewModel.setDailyReminderMinutes(selected);
                binding.tvReminderTime.setText(DateUtils.formatMinutesOfDay(selected));
                Toast.makeText(this, getString(R.string.msg_reminder_time_set,
                        DateUtils.formatMinutesOfDay(selected)), Toast.LENGTH_SHORT).show();
            });
            picker.show(getSupportFragmentManager(), "daily_reminder_picker");
        });

        binding.rowExactAlarm.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName()));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, R.string.msg_cannot_open_settings, Toast.LENGTH_SHORT).show();
                }
            }
        });
        updateReminderRows();
    }

    private void updateReminderRows() {
        boolean enabled = prefs.isNotificationsEnabled();
        binding.rowReminderTime.setEnabled(enabled);
        binding.rowReminderTime.setAlpha(enabled ? 1f : 0.4f);
        updateExactAlarmRow();
    }

    private void updateExactAlarmRow() {
        boolean show = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && prefs.isNotificationsEnabled()
                && !ReminderScheduler.canScheduleExact(this);
        binding.rowExactAlarm.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // ------------------------------------------------------------------ Tiền tệ

    private void setupCurrency() {
        int style = prefs.getCurrencyStyle();
        if (style == CurrencyUtils.STYLE_CODE) {
            binding.radioCurrency.check(R.id.rbCurrencyCode);
        } else if (style == CurrencyUtils.STYLE_SHORT) {
            binding.radioCurrency.check(R.id.rbCurrencyShort);
        } else {
            binding.radioCurrency.check(R.id.rbCurrencySymbol);
        }
        binding.radioCurrency.setOnCheckedChangeListener((group, checkedId) -> {
            int selected;
            if (checkedId == R.id.rbCurrencyCode) {
                selected = CurrencyUtils.STYLE_CODE;
            } else if (checkedId == R.id.rbCurrencyShort) {
                selected = CurrencyUtils.STYLE_SHORT;
            } else {
                selected = CurrencyUtils.STYLE_SYMBOL;
            }
            if (selected != prefs.getCurrencyStyle()) {
                viewModel.setCurrencyStyle(selected);
                currencyChanged = true;
            }
        });
    }

    // ------------------------------------------------------------------ Dữ liệu

    private void setupData() {
        binding.rowClearData.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_clear_data_confirm_title)
                .setMessage(R.string.settings_clear_data_confirm_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.settings_clear_data_confirm_button, (dialog, which) ->
                        viewModel.resetAllData(() -> {
                            dataReset = true;
                            Toast.makeText(this, R.string.msg_data_cleared, Toast.LENGTH_LONG).show();
                        }))
                .show());
    }
}
