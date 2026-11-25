# Quy tắc ProGuard/R8 cho Life-Manager.
# Hiện tại bản release tắt minify. Nếu bật minifyEnabled, giữ lại các lớp sau:

# Room entity / DAO được sinh code tự động
-keep class com.example.lifemanager.data.entity.** { *; }
-keep class com.example.lifemanager.data.model.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
