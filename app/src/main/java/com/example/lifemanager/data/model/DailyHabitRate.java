package com.example.lifemanager.data.model;

/**
 * Tỷ lệ hoàn thành thói quen của một ngày (dùng cho biểu đồ tuần / tháng).
 */
public class DailyHabitRate {

    public final long epochDay;
    public final int done;
    public final int scheduled;

    public DailyHabitRate(long epochDay, int done, int scheduled) {
        this.epochDay = epochDay;
        this.done = done;
        this.scheduled = scheduled;
    }

    public float getPercent() {
        return scheduled == 0 ? 0f : done * 100f / scheduled;
    }
}
