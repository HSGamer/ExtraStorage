package me.hsgamer.extrastorage.util;

import java.text.DecimalFormat;
import java.util.concurrent.ThreadLocalRandom;

public final class Digital {

    private static final DecimalFormat FORMAT = new DecimalFormat("##.##"), THOUSANDS_FORMAT = new DecimalFormat("###,###");
    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    private Digital() {
    }

    public static boolean isNumber(String input) {
        return input.matches("\\d+");
    }

    public static int getBetween(int min, int max, int value) {
        return Math.min(Math.max(min, value), max);
    }

    public static long getBetween(long min, long max, long value) {
        return Math.min(Math.max(min, value), max);
    }

    public static double getBetween(double min, double max, double value) {
        return Math.min(Math.max(min, value), max);
    }

    public static float getBetween(float min, float max, float value) {
        return Math.min(Math.max(min, value), max);
    }

    public static int random(int min, int max) {
        return (RANDOM.nextInt(max - min + 1) + min);
    }

    public static long random(long min, long max) {
        return (RANDOM.nextLong(max - min + 1) + min);
    }

    public static double random(double min, double max) {
        double rnd = ThreadLocalRandom.current().nextDouble();
        return (rnd * (max - min) + min);
    }

    public static double formatDouble(double value) {
        return Double.parseDouble(FORMAT.format(value).replace(',', '.'));
    }

    public static String formatDouble(String pattern, double value) {
        DecimalFormat format = new DecimalFormat(pattern);
        return format.format(value);
    }

    public static float formatFloat(float value) {
        return Float.parseFloat(FORMAT.format(value).replace(',', '.'));
    }

    public static float formatFloat(String pattern, float value) {
        DecimalFormat format = new DecimalFormat(pattern);
        return Float.parseFloat(format.format(value).replace(',', '.'));
    }

    public static String formatThousands(long value) {
        return THOUSANDS_FORMAT.format(value);
    }

}
