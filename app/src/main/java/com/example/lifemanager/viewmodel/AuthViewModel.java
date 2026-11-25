package com.example.lifemanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lifemanager.data.model.AuthResult;
import com.example.lifemanager.data.repository.AuthRepository;
import com.example.lifemanager.utils.ResultCallback;

/**
 * ViewModel dùng chung cho màn hình Đăng nhập và Đăng ký.
 * Giữ trạng thái đang xử lý để không gửi yêu cầu trùng và không mất trạng thái khi xoay màn hình.
 */
public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository repository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = AuthRepository.getInstance(application);
    }

    public LiveData<Boolean> isLoading() {
        return loading;
    }

    @NonNull
    public String getLastUsername() {
        return repository.getLastUsername();
    }

    public void login(@Nullable String username, @Nullable String password,
                      @NonNull ResultCallback<AuthResult> callback) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        loading.setValue(true);
        repository.login(username, password, result -> {
            loading.setValue(false);
            callback.onResult(result);
        });
    }

    public void register(@Nullable String displayName, @Nullable String username, @Nullable String password,
                         @Nullable String confirm, @NonNull ResultCallback<AuthResult> callback) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        loading.setValue(true);
        repository.register(displayName, username, password, confirm, result -> {
            loading.setValue(false);
            callback.onResult(result);
        });
    }
}
