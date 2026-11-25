package com.example.lifemanager.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import java.util.Objects;
import java.util.function.Function;

/**
 * Các phép biến đổi LiveData viết bằng Java thuần (map, switchMap, distinctUntilChanged).
 */
public final class LiveDataUtils {

    private LiveDataUtils() {
    }

    /** Biến đổi giá trị của LiveData nguồn. */
    @NonNull
    public static <X, Y> LiveData<Y> map(@NonNull LiveData<X> source, @NonNull Function<X, Y> mapper) {
        MediatorLiveData<Y> result = new MediatorLiveData<>();
        result.addSource(source, value -> result.setValue(mapper.apply(value)));
        return result;
    }

    /** Mỗi khi trigger thay đổi, chuyển sang theo dõi một LiveData mới. */
    @NonNull
    public static <X, Y> LiveData<Y> switchMap(@NonNull LiveData<X> trigger,
                                               @NonNull Function<X, LiveData<Y>> mapper) {
        MediatorLiveData<Y> result = new MediatorLiveData<>();
        result.addSource(trigger, new Observer<X>() {
            @Nullable
            private LiveData<Y> current;

            @Override
            public void onChanged(X value) {
                LiveData<Y> next = mapper.apply(value);
                if (current == next) {
                    return;
                }
                if (current != null) {
                    result.removeSource(current);
                }
                current = next;
                if (current != null) {
                    result.addSource(current, result::setValue);
                }
            }
        });
        return result;
    }

    /** Chỉ phát ra giá trị khi khác giá trị trước đó. */
    @NonNull
    public static <X> LiveData<X> distinctUntilChanged(@NonNull LiveData<X> source) {
        MediatorLiveData<X> result = new MediatorLiveData<>();
        result.addSource(source, new Observer<X>() {
            private boolean first = true;
            private X previous;

            @Override
            public void onChanged(X value) {
                if (first || !Objects.equals(previous, value)) {
                    first = false;
                    previous = value;
                    result.setValue(value);
                }
            }
        });
        return result;
    }
}
