package com.fundtransfer.support;

import java.util.Locale;

public final class Money {

    private Money() {
    }

    /** ParaBank prints balances as $515.50 or -$10.00. */
    public static double parse(String text) {
        return Double.parseDouble(text.replaceAll("[^0-9.-]", ""));
    }

    /** Locale.US keeps the dot. A European Windows locale would print 50,00 and the row would not match. */
    public static String format(double amount) {
        String sign = amount < 0 ? "-" : "";
        return sign + "$" + String.format(Locale.US, "%.2f", Math.abs(amount));
    }

    public static long toCents(double amount) {
        return Math.round(amount * 100);
    }
}
