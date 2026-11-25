package com.example.lifemanager.ui.report;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.lifemanager.R;
import com.example.lifemanager.utils.CurrencyUtils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.List;
import java.util.Locale;

/**
 * Cấu hình giao diện chung cho biểu đồ MPAndroidChart.
 */
final class ChartHelper {

    private ChartHelper() {
    }

    static void setupPieChart(@NonNull PieChart chart, @NonNull Context context) {
        int textPrimary = ContextCompat.getColor(context, R.color.lm_text_primary);
        chart.getDescription().setEnabled(false);
        chart.setUsePercentValues(true);
        chart.setDrawEntryLabels(false);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.WHITE);
        chart.setHoleRadius(56f);
        chart.setTransparentCircleRadius(60f);
        chart.setRotationEnabled(false);
        chart.setHighlightPerTapEnabled(true);
        chart.setCenterTextSize(13f);
        chart.setCenterTextColor(textPrimary);
        chart.setNoDataText(context.getString(R.string.chart_no_data));
        chart.setNoDataTextColor(ContextCompat.getColor(context, R.color.lm_text_secondary));
        chart.setExtraOffsets(4f, 4f, 4f, 4f);

        Legend legend = chart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setWordWrapEnabled(true);
        legend.setDrawInside(false);
        legend.setTextColor(textPrimary);
        legend.setTextSize(11f);
        legend.setForm(Legend.LegendForm.CIRCLE);
    }

    /** Đổ dữ liệu cho biểu đồ tròn; danh sách rỗng sẽ hiển thị "Chưa có dữ liệu". */
    static void setPieData(@NonNull PieChart chart, @NonNull List<PieEntry> entries,
                           @NonNull List<Integer> colors, @NonNull CharSequence centerText) {
        if (entries.isEmpty()) {
            chart.clear();
            return;
        }
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(5f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(11f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Ẩn nhãn của lát quá nhỏ để biểu đồ không bị rối
                return value < 4f ? "" : String.format(Locale.US, "%.1f%%", value);
            }
        });
        chart.setData(new PieData(dataSet));
        chart.setCenterText(centerText);
        chart.highlightValues(null);
        chart.animateY(600);
        chart.invalidate();
    }

    static void setupBarChart(@NonNull BarChart chart, @NonNull Context context) {
        int textSecondary = ContextCompat.getColor(context, R.color.lm_text_secondary);
        chart.getDescription().setEnabled(false);
        chart.setNoDataText(context.getString(R.string.chart_no_data));
        chart.setNoDataTextColor(textSecondary);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setScaleEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setExtraBottomOffset(6f);
        chart.getAxisRight().setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(textSecondary);
        xAxis.setTextSize(10f);

        YAxis left = chart.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setTextColor(textSecondary);
        left.setGridColor(ContextCompat.getColor(context, R.color.lm_divider));
        left.setDrawAxisLine(false);

        Legend legend = chart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setDrawInside(false);
        legend.setTextColor(ContextCompat.getColor(context, R.color.lm_text_primary));
        legend.setForm(Legend.LegendForm.CIRCLE);
    }

    /** Định dạng trục Y theo tiền rút gọn (500k, 1.5tr). */
    static final ValueFormatter MONEY_AXIS_FORMATTER = new ValueFormatter() {
        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            return CurrencyUtils.formatCompact(value);
        }
    };

    /** Định dạng trục Y theo phần trăm. */
    static final ValueFormatter PERCENT_AXIS_FORMATTER = new ValueFormatter() {
        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            return Math.round(value) + "%";
        }
    };

    /**
     * Nhãn trục X theo chỉ số.
     *
     * @param grouped biểu đồ cột nhóm (nhãn nằm giữa nhóm, dùng floor thay vì round).
     */
    @NonNull
    static ValueFormatter labelFormatter(@NonNull List<String> labels, boolean grouped) {
        return new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int index = grouped ? (int) Math.floor(value) : Math.round(value);
                if (index < 0 || index >= labels.size()) {
                    return "";
                }
                return labels.get(index);
            }
        };
    }

    /** Rút gọn tên danh mục cho nhãn trục. */
    @NonNull
    static String shorten(@NonNull String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
