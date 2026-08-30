package com.rpscene.scene;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses roleplay duration tokens such as {@code 10s}, {@code 30m},
 * {@code 2h}, {@code 7d} into milliseconds.
 * <p>
 * A duration argument of {@code 0} or an unparsable token is treated as
 * "not a duration", which lets the /do command tell timed scenes apart
 * from persistent ones and from plain message text.
 */
public final class DurationParser {

    private static final Pattern PATTERN = Pattern.compile("^(\\d+)(s|m|h|d)$", Pattern.CASE_INSENSITIVE);

    private DurationParser() {
    }

    /**
     * Attempts to parse a token like {@code 30m} into milliseconds.
     * Returns empty if the token does not match the duration grammar.
     */
    public static Optional<Long> parseMillis(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }
        Matcher matcher = PATTERN.matcher(token.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2).toLowerCase(Locale.ROOT);
        long unitMillis = switch (unit) {
            case "s" -> 1000L;
            case "m" -> 60_000L;
            case "h" -> 3_600_000L;
            case "d" -> 86_400_000L;
            default -> -1L;
        };
        if (unitMillis <= 0 || amount <= 0) {
            return Optional.empty();
        }
        return Optional.of(amount * unitMillis);
    }

    /**
     * Quick check used to decide whether a token is a duration and should
     * therefore be consumed from the /do argument list rather than treated
     * as the start of the message.
     */
    public static boolean looksLikeDuration(String token) {
        return token != null && PATTERN.matcher(token.trim()).matches();
    }

    /**
     * Formats a remaining-time duration (milliseconds) into a short human
     * readable string such as "27 minutes", "3 hours", "45 seconds".
     */
    public static String formatRemaining(long millis) {
        if (millis <= 0) {
            return "expiring";
        }
        long seconds = millis / 1000;
        if (seconds < 60) {
            return seconds + (seconds == 1 ? " second" : " seconds");
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute" : " minutes");
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + (hours == 1 ? " hour" : " hours");
        }
        long days = hours / 24;
        return days + (days == 1 ? " day" : " days");
    }
}
