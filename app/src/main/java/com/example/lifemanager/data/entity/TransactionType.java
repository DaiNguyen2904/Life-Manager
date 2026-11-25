package com.example.lifemanager.data.entity;

/**
 * Hằng số loại giao dịch / loại danh mục.
 */
public final class TransactionType {

    public static final String INCOME = "INCOME";
    public static final String EXPENSE = "EXPENSE";

    private TransactionType() {
    }

    public static boolean isValid(String type) {
        return INCOME.equals(type) || EXPENSE.equals(type);
    }
}
