package com.example.lifemanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lifemanager.data.repository.AuthRepository;
import com.example.lifemanager.data.repository.SettingsRepository;
import com.example.lifemanager.utils.PrefsManager;

/**
 * ViewModel cho màn hình Cài đặt.
 */
public class SettingsViewModel extends AndroidViewModel {

    private final SettingsRepository repository;
    private final AuthRepository authRepository;
    private final PrefsManager prefs;
    private final MutableLiveData<Boolean> resetting = new MutableLiveData<>(false);

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        repository = SettingsRepository.getInstance(application);
        authRepository = AuthRepository.getInstance(application);
        prefs = PrefsManager.getInstance(application);
    }

    /** Tên đăng nhập của tài khoản hiện tại. */
    @Nullable
    public String getUsername() {
        return authRepository.getCurrentUsername();
    }

    public void logout(@NonNull Runnable onDone) {
        authRepository.logout(onDone);
    }

    public LiveData<Boolean> isResetting() {
        return resetting;
    }

    public PrefsManager getPrefs() {
        return prefs;
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.setNotificationsEnabled(enabled);
        repository.refreshReminders();
    }

    public void setDailyReminderMinutes(int minutes) {
        prefs.setDailyReminderMinutes(minutes);
        repository.refreshReminders();
    }

    public void setCurrencyStyle(int style) {
        prefs.setCurrencyStyle(style);
    }

    public void setUserName(@NonNull String name) {
        prefs.setUserName(name);
        authRepository.updateDisplayName(name);
    }

    public void resetAllData(@NonNull Runnable onDone) {
        resetting.setValue(true);
        repository.resetAllData(() -> {
            resetting.setValue(false);
            onDone.run();
        });
    }
}
