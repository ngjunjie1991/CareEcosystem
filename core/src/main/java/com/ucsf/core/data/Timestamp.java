package com.ucsf.core.data;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides a unified way to get timestamps.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public abstract class Timestamp {
    private static final Pattern YYYYMMDD_HHMMSS_PATTERN =
            Pattern.compile("(\\d+)(\\d{2})(\\d{2})-(\\d{2})(\\d{2})(\\d{2})");
    private static final Pattern YY_MM_DDTHH_MM_SS_MS_PATTERN =
            Pattern.compile("(\\d+)-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})\\.\\d+");
    private static final Pattern MMDDYY_HHMMSS_PATTERN =
            Pattern.compile("(\\d{2})/(\\d{2})/(\\d{2})-(\\d{2}):(\\d{2}):(\\d{2})");

    /** Enumeration of all the possible timestamp format. */
    public enum Format {
        Seconds,                /**< Seconds since the epoch */
        YYYYMMDD_HHMMSS,        /**< yyyymmdd-hhmmss */
        YY_MM_DDTHH_MM_SS_MS,   /**< yy-mm-ddThh:mm:ss.milliseconds */
        MMDDYY_HHMMSS           /**< mm/dd/yy-hh:mm:ss */
    }

    /** Default timestamp format used by the application. */
    public static final Format DEFAULT_FORMAT = Format.Seconds;

    /**
     * Returns time since the epoch in seconds.
     */
    public static String getTimestamp() {
        return getTimestamp(DEFAULT_FORMAT);
    }

    /**
     * Returns time since the epoch in seconds with the given offset (in milliseconds).
     */
    public static String getTimestamp(long offset) {
        return getTimestamp(offset, DEFAULT_FORMAT);
    }

    /**
     * Returns a timestamp using the given time (in milliseconds).
     */
    public static String getTimestampFromTime(long time) {
        return getTimestampFromCalendar(getCalendarFromTime(time), DEFAULT_FORMAT);
    }

    /**
     * Returns time since the epoch with the given format.
     */
    public static String getTimestamp(Format format) {
        return getTimestampFromCalendar(new GregorianCalendar(), format);
    }

    /**
     * Returns time since the epoch with the given format, using the given offset (in milliseconds).
     */
    private static String getTimestamp(long offset, Format format) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(System.currentTimeMillis() + offset);
        return getTimestampFromCalendar(calendar, format);
    }

    /**
     * Converts the given calendar to a timestamps using the default format.
     */
    public static String getTimestampFromCalendar(Calendar calendar) {
        return getTimestampFromCalendar(calendar, DEFAULT_FORMAT);
    }

    /**
     * Converts the given calendar to a timestamps using the given format.
     */
    public static String getTimestampFromCalendar(Calendar calendar, Format format) {
        switch (format) {
            case Seconds:
                return String.valueOf(calendar.getTimeInMillis() / 1000);
            case YYYYMMDD_HHMMSS:
                return String.format("%d%02d%02d-%02d%02d%02d",
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DAY_OF_MONTH),
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        calendar.get(Calendar.SECOND));
            case YY_MM_DDTHH_MM_SS_MS:
                return String.format("%d-%02d-%02dT%02d:%02d:%02d.000",
                        calendar.get(Calendar.YEAR) % 100,
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DAY_OF_MONTH),
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        calendar.get(Calendar.SECOND));
            case MMDDYY_HHMMSS:
                return String.format("%02d/%02d/%02d-%02d:%02d:%02d",
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DAY_OF_MONTH),
                        calendar.get(Calendar.YEAR) % 100,
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        calendar.get(Calendar.SECOND));
        }
        return "";
    }

    /**
     * Returns a calendar corresponding to the given time (in milliseconds).
     */
    public static Calendar getCalendarFromTime(long time) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(time);
        return calendar;
    }

    /**
     * Returns a calendar corresponding to the given timestamp returned by getTimestamp().
     */
    public static Calendar getCalendarFromTimestamp(String timestamp) {
        return getCalendarFromTimestamp(timestamp, DEFAULT_FORMAT);
    }

    /**
     * Returns a calendar corresponding to the given timestamp.
     */
    public static Calendar getCalendarFromTimestamp(String timestamp, Format format) {
        GregorianCalendar calendar = new GregorianCalendar();
        Matcher m;
        switch (format) {
            case Seconds:
                calendar.setTimeInMillis(Long.valueOf(timestamp) * 1000);
                break;
            case YYYYMMDD_HHMMSS:
                m = YYYYMMDD_HHMMSS_PATTERN.matcher(timestamp);
                if (m.matches()) {
                    calendar.set(Calendar.YEAR, Integer.valueOf(m.group(1)));
                    calendar.set(Calendar.MONTH, Integer.valueOf(m.group(2)) - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, Integer.valueOf(m.group(3)));
                    calendar.set(Calendar.HOUR_OF_DAY, Integer.valueOf(m.group(4)));
                    calendar.set(Calendar.MINUTE, Integer.valueOf(m.group(5)));
                    calendar.set(Calendar.SECOND, Integer.valueOf(m.group(6)));
                }
                break;
            case YY_MM_DDTHH_MM_SS_MS:
                m = YY_MM_DDTHH_MM_SS_MS_PATTERN.matcher(timestamp);
                if (m.matches()) {
                    calendar.set(Calendar.YEAR, Integer.valueOf(m.group(1)) + 2000);
                    calendar.set(Calendar.MONTH, Integer.valueOf(m.group(2)) - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, Integer.valueOf(m.group(3)));
                    calendar.set(Calendar.HOUR_OF_DAY, Integer.valueOf(m.group(4)));
                    calendar.set(Calendar.MINUTE, Integer.valueOf(m.group(5)));
                    calendar.set(Calendar.SECOND, Integer.valueOf(m.group(6)));
                }
                break;
            case MMDDYY_HHMMSS:
                m = MMDDYY_HHMMSS_PATTERN.matcher(timestamp);
                if (m.matches()) {
                    calendar.set(Calendar.YEAR, Integer.valueOf(m.group(3)) + 2000);
                    calendar.set(Calendar.MONTH, Integer.valueOf(m.group(1)) - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, Integer.valueOf(m.group(2)));
                    calendar.set(Calendar.HOUR_OF_DAY, Integer.valueOf(m.group(4)));
                    calendar.set(Calendar.MINUTE, Integer.valueOf(m.group(5)));
                    calendar.set(Calendar.SECOND, Integer.valueOf(m.group(6)));
                }
                break;
        }
        return calendar;
    }

}
