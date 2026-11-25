package com.example.lifemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.lifemanager.data.entity.Task;
import com.example.lifemanager.data.model.TaskStats;
import com.example.lifemanager.data.model.TransactionFilter;
import com.example.lifemanager.utils.CurrencyUtils;
import com.example.lifemanager.utils.DateUtils;
import com.example.lifemanager.utils.TextNormalizer;

import org.junit.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;

/**
 * Kiểm thử các tiện ích: tiền tệ, tìm kiếm không dấu, trạng thái công việc, bộ lọc giao dịch.
 */
public class UtilsTest {

    @Test
    public void currency_formatAndParse() {
        CurrencyUtils.setStyle(CurrencyUtils.STYLE_SYMBOL);
        assertEquals("1.500.000 ₫", CurrencyUtils.format(1_500_000));
        assertEquals("0 ₫", CurrencyUtils.format(0));
        CurrencyUtils.setStyle(CurrencyUtils.STYLE_CODE);
        assertEquals("2.000.000 VNĐ", CurrencyUtils.format(2_000_000));
        CurrencyUtils.setStyle(CurrencyUtils.STYLE_SYMBOL);

        assertEquals(1_500_000, CurrencyUtils.parse("1.500.000"));
        assertEquals(-1, CurrencyUtils.parse(""));
        assertEquals(-1, CurrencyUtils.parse(null));
        assertEquals("+50.000 ₫", CurrencyUtils.formatSigned(50_000, true));
        assertEquals("-50.000 ₫", CurrencyUtils.formatSigned(50_000, false));
    }

    @Test
    public void textNormalizer_removesVietnameseDiacritics() {
        assertEquals("an uong", TextNormalizer.normalize("Ăn uống"));
        assertEquals("di chuyen", TextNormalizer.normalize("Di chuyển"));
        assertEquals("dau tu", TextNormalizer.normalize("Đầu tư"));
        assertTrue(TextNormalizer.matches(TextNormalizer.normalize("hoa don"), "Hóa đơn & Tiện ích"));
        assertFalse(TextNormalizer.matches("xyz", "Mua sắm"));
    }

    @Test
    public void task_effectiveStatusDetectsOverdue() {
        long today = LocalDate.of(2026, 11, 17).toEpochDay();
        Task task = new Task();
        task.setStatus(Task.STATUS_IN_PROGRESS);
        task.setDueDate(today - 1);
        assertTrue(task.isOverdue(today));
        assertEquals(Task.STATUS_OVERDUE, task.getEffectiveStatus(today));

        task.setDueDate(today);
        assertFalse(task.isOverdue(today));
        assertEquals(Task.STATUS_IN_PROGRESS, task.getEffectiveStatus(today));

        task.setDueDate(today - 5);
        task.setStatus(Task.STATUS_DONE);
        assertFalse(task.isOverdue(today));
        assertEquals(Task.STATUS_DONE, task.getEffectiveStatus(today));
    }

    @Test
    public void taskStats_completionRate() {
        long today = LocalDate.of(2026, 11, 17).toEpochDay();
        Task done = new Task();
        done.setStatus(Task.STATUS_DONE);
        Task overdue1 = new Task();
        overdue1.setDueDate(today - 2);
        Task overdue2 = new Task();
        overdue2.setDueDate(today - 1);
        TaskStats stats = TaskStats.from(Arrays.asList(done, overdue1, overdue2), today);
        assertEquals(3, stats.total);
        assertEquals(1, stats.done);
        assertEquals(2, stats.overdue);
        assertEquals(0, stats.todo);
        assertEquals(33, stats.getCompletionRate());
    }

    @Test
    public void transactionFilter_monthRange() {
        TransactionFilter filter = TransactionFilter.defaultFilter().withMonth(YearMonth.of(2026, 2));
        assertEquals(LocalDate.of(2026, 2, 1).toEpochDay(), filter.getFromDay());
        assertEquals(LocalDate.of(2026, 2, 28).toEpochDay(), filter.getToDay());
        assertTrue(TransactionFilter.defaultFilter().isDefault());
        assertFalse(filter.isDefault());
    }

    @Test
    public void dateUtils_vietnameseFormats() {
        LocalDate date = LocalDate.of(2026, 11, 17);
        assertEquals("Thứ Ba, 17 Tháng 11, 2026", DateUtils.formatFullDate(date));
        assertEquals(321, DateUtils.dayOfYear(date));
        assertEquals(365, DateUtils.daysInYear(date));
        assertEquals("17/11/2026", DateUtils.formatDate(date.toEpochDay()));
        assertEquals("Quá hạn 2 ngày", DateUtils.describeDue(date.toEpochDay() - 2, date.toEpochDay()));
        assertEquals("Ngày mai", DateUtils.describeDue(date.toEpochDay() + 1, date.toEpochDay()));
    }
}
