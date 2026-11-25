package com.example.lifemanager.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.lifemanager.MainActivity;
import com.example.lifemanager.R;
import com.example.lifemanager.data.model.AuthResult;
import com.example.lifemanager.databinding.ActivityRegisterBinding;
import com.example.lifemanager.ui.BaseActivity;
import com.example.lifemanager.utils.ViewUtils;
import com.example.lifemanager.viewmodel.AuthViewModel;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Màn hình Đăng ký: tạo tài khoản mới trên thiết bị (tên hiển thị, tên đăng nhập, mật khẩu).
 * Đăng ký thành công sẽ đăng nhập luôn và mở màn hình chính.
 */
public class RegisterActivity extends BaseActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewUtils.applySystemBarInsets(binding.root, true, true);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.tvGoLogin.setOnClickListener(v -> finish());
        binding.btnRegister.setOnClickListener(v -> submit());
        binding.etConfirm.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
        viewModel.isLoading().observe(this, this::renderLoading);
    }

    private void submit() {
        ViewUtils.hideKeyboard(binding.getRoot());
        clearErrors();
        viewModel.register(
                LoginActivity.text(binding.etDisplayName.getText()),
                LoginActivity.text(binding.etUsername.getText()),
                LoginActivity.text(binding.etPassword.getText()),
                LoginActivity.text(binding.etConfirm.getText()),
                this::onResult);
    }

    private void onResult(@NonNull AuthResult result) {
        if (result.success && result.user != null) {
            String message = getString(R.string.msg_register_success, result.user.getDisplayName());
            if (result.adoptedExistingData) {
                message += "\n" + getString(R.string.msg_register_adopted);
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Intent main = new Intent(this, MainActivity.class)
                    .putExtra(MainActivity.EXTRA_OPEN_TAB,
                            getIntent().getIntExtra(MainActivity.EXTRA_OPEN_TAB, 0))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(main);
            finish();
            return;
        }
        TextInputLayout target = fieldLayout(result.field);
        if (target != null) {
            target.setError(result.error);
            target.requestFocus();
        } else {
            binding.tvError.setText(result.error);
            binding.tvError.setVisibility(View.VISIBLE);
        }
    }

    @Nullable
    private TextInputLayout fieldLayout(@NonNull AuthResult.Field field) {
        switch (field) {
            case DISPLAY_NAME:
                return binding.tilDisplayName;
            case USERNAME:
                return binding.tilUsername;
            case PASSWORD:
                return binding.tilPassword;
            case CONFIRM:
                return binding.tilConfirm;
            default:
                return null;
        }
    }

    private void renderLoading(@Nullable Boolean loading) {
        boolean busy = Boolean.TRUE.equals(loading);
        binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!busy);
        binding.btnRegister.setText(busy ? R.string.register_loading : R.string.register_button);
        binding.tvGoLogin.setEnabled(!busy);
    }

    private void clearErrors() {
        binding.tilDisplayName.setError(null);
        binding.tilUsername.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirm.setError(null);
        binding.tvError.setVisibility(View.GONE);
    }
}
