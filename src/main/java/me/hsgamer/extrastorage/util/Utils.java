package me.hsgamer.extrastorage.util;

import net.md_5.bungee.api.ChatColor;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Utils {

    private static final Pattern HEX_PATTERN = Pattern.compile("(?<!\\\\)(#[a-fA-F0-9]{6})"),
            STRIP_COLOR_PATTERN = Pattern.compile("(?i)&|§[0-9A-FK-ORX]"),
            CAPITALIZE_PATTERN = Pattern.compile("\\b(.)(.*?)\\b");

    private Utils() {
    }

    public static String capitalizeAll(String input) {
        if ((input == null) || input.isEmpty()) return input;

        Matcher matcher = CAPITALIZE_PATTERN.matcher(input);
        if (!matcher.find()) return input;
        matcher.reset();
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String firstChar = matcher.group(1).toUpperCase();
            String rest = matcher.group(2);
            result.append(firstChar).append(rest);
        }
        return result.toString();
    }

    public static String formatName(String key) {
        return capitalizeAll(key.replace('_', ' ').toLowerCase(Locale.ENGLISH));
    }

    public static String getRegex(String... inputs) {
        StringBuilder builder = new StringBuilder();
        builder.append("(?ium)(");
        Arrays.stream(inputs).forEach(input ->
                builder.append("\\{").append(input).append("\\}")
                        .append('|')
                        .append("\\%").append(input).append("\\%")
                        .append('|')
        );
        builder.deleteCharAt(builder.length() - 1).append(")");
        return builder.toString();
    }

    public static String stripColor(String input) {
        if (input == null) return null;
        return STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }

    public static String colorize(String input) {
        Matcher matcher = HEX_PATTERN.matcher(input);
        while (matcher.find()) {
            String color = matcher.group(0);
            input = input.replace(color, "" + ChatColor.of(color));
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static List<?> colorize(List<?> input) {
        if ((input == null) || input.isEmpty()) return input;
        return input.stream().map(key -> colorize(String.valueOf(key))).collect(Collectors.toList());
    }

}
