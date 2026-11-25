package com.example.lifemanager.ui.auth;

import android.content.Context;
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
import com.example.lifemanager.databinding.ActivityLoginBinding;
import com.example.lifemanager.ui.BaseActivity;
import com.example.lifemanager.utils.SessionManager;
import com.example.lifemanager.utils.ViewUtils;
import com.example.lifemanager.viewmodel.AuthViewModel;

/**
 * Màn hình Đăng nhập: nhập tên đăng nhập và mật khẩu của tài khoản trên thiết bị.
 * Khi đăng nhập thành công, mở màn hình chính với dữ liệu riêng của tài khoản.
 */
public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;

    /** Mở màn hình Đăng nhập, xóa toàn bộ màn hình đang mở (dùng khi chưa đăng nhập hoặc đăng xuất). */
    @NonNull
    public static Intent newIntent(@NonNull Context context, int openTab) {
        return new Intent(context, LoginActivity.class)
                .putExtra(MainActivity.EXTRA_OPEN_TAB, openTab)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SessionManager.getInstance(this).isLoggedIn()) {
            openMain();
            return;
        }
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewUtils.applySystemBarInsets(binding.root, true, true);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        if (savedInstanceState == null) {
            binding.etUsername.setText(viewModel.getLastUsername());
            if (!viewModel.getLastUsername().isEmpty()) {
                binding.etPassword.requestFocus();
            }
        }

        binding.btnLogin.setOnClickListener(v -> submit());
        binding.etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
        binding.tvGoRegister.setOnClickListener(v -> startActivity(
                new Intent(this, RegisterActivity.class).putExtras(getIntent())));
        viewModel.isLoading().observe(this, this::renderLoading);
    }

    private void submit() {
        ViewUtils.hideKeyboard(binding.getRoot());
        clearErrors();
        viewModel.login(text(binding.etUsername.getText()), text(binding.etPassword.getText()), this::onResult);
    }

    private void onResult(@NonNull AuthResult result) {
        if (result.success && result.user != null) {
            Toast.makeText(this, getString(R.string.msg_login_success, result.user.getDisplayName()),
                    Toast.LENGTH_SHORT).show();
            openMain();
            return;
        }
        if (result.field == AuthResult.Field.USERNAME) {
            binding.tilUsername.setError(result.error);
        } else if (result.field == AuthResult.Field.PASSWORD) {
            binding.tilPassword.setError(result.error);
            binding.etPassword.setText(null);
        } else {
            binding.tvError.setText(result.error);
            binding.tvError.setVisibility(View.VISIBLE);
        }
    }

    private void renderLoading(@Nullable Boolean loading) {
        boolean busy = Boolean.TRUE.equals(loading);
        binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!busy);
        binding.btnLogin.setText(busy ? R.string.login_loading : R.string.login_button);
        binding.tvGoRegister.setEnabled(!busy);
    }

    private void clearErrors() {
        binding.tilUsername.setError(null);
        binding.tilPassword.setError(null);
        binding.tvError.setVisibility(View.GONE);
    }

    /** Mở màn hình chính (giữ tab được yêu cầu, ví dụ khi mở từ thông báo nhắc nhở). */
    private void openMain() {
        Intent main = new Intent(this, MainActivity.class)
                .putExtra(MainActivity.EXTRA_OPEN_TAB, getIntent().getIntExtra(MainActivity.EXTRA_OPEN_TAB, 0))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(main);
        finish();
    }

    @NonNull
    static String text(@Nullable CharSequence value) {
        return value == null ? "" : value.toString();
    }
}
