package com.cloud.arch.aggregate.reflection;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class Converter {

    public static final BigDecimal BIG_DECIMAL_ZERO = BigDecimal.ZERO;

    public static boolean convert2boolean(Object fromInstance) {
        if (fromInstance == null) {
            return false;
        }
        return convertToBoolean(fromInstance);
    }

    /**
     * Convert from the passed in instance to a Boolean.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static Boolean convertToBoolean(Object fromInstance) {
        if (fromInstance instanceof Boolean) {
            return (Boolean) fromInstance;
        } else if (fromInstance instanceof String) {
            // faster equals check "true" and "false"
            if ("true".equals(fromInstance)) {
                return true;
            } else if ("false".equals(fromInstance)) {
                return false;
            }

            return "true".equalsIgnoreCase((String) fromInstance);
        } else if (fromInstance instanceof Number) {
            return ((Number) fromInstance).longValue() != 0;
        } else if (fromInstance instanceof AtomicBoolean) {
            return ((AtomicBoolean) fromInstance).get();
        }
        nope(fromInstance, "Boolean");
        return null;
    }

    public static BigDecimal convert2BigDecimal(Object fromInstance) {
        if (fromInstance == null) {
            return BIG_DECIMAL_ZERO;
        }
        return convertToBigDecimal(fromInstance);
    }

    /**
     * Convert from the passed in instance to a BigDecimal.  If null is passed in, this method will return null.  If ""
     * is passed in, this method will return a BigDecimal with the value of 0.  Possible inputs are String (base10
     * numeric values in string), BigInteger, any primitive/primitive wrapper, Boolean/AtomicBoolean (returns
     * BigDecimal of 0 or 1), Date, Calendar, LocalDate, LocalDateTime, ZonedDateTime (returns BigDecimal with the
     * value of number of milliseconds since Jan 1, 1970), and Character (returns integer value of character).
     */
    public static BigDecimal convertToBigDecimal(Object fromInstance) {
        try {
            if (fromInstance instanceof String) {
                if (StringUtils.isEmpty((String) fromInstance)) {
                    return BigDecimal.ZERO;
                }
                return new BigDecimal(((String) fromInstance).trim());
            } else if (fromInstance instanceof BigDecimal) {
                return (BigDecimal) fromInstance;
            } else if (fromInstance instanceof BigInteger) {
                return new BigDecimal((BigInteger) fromInstance);
            } else if (fromInstance instanceof Long) {
                return new BigDecimal((Long) fromInstance);
            } else if (fromInstance instanceof AtomicLong) {
                return new BigDecimal(((AtomicLong) fromInstance).get());
            } else if (fromInstance instanceof Number) {
                return new BigDecimal(String.valueOf(fromInstance));
            } else if (fromInstance instanceof Boolean) {
                return (Boolean) fromInstance ? BigDecimal.ONE : BigDecimal.ZERO;
            } else if (fromInstance instanceof AtomicBoolean) {
                return ((AtomicBoolean) fromInstance).get() ? BigDecimal.ONE : BigDecimal.ZERO;
            } else if (fromInstance instanceof Date) {
                return new BigDecimal(((Date) fromInstance).getTime());
            } else if (fromInstance instanceof LocalDate) {
                return new BigDecimal(localDateToMillis((LocalDate) fromInstance));
            } else if (fromInstance instanceof LocalDateTime) {
                return new BigDecimal(localDateTimeToMillis((LocalDateTime) fromInstance));
            } else if (fromInstance instanceof ZonedDateTime) {
                return new BigDecimal(zonedDateTimeToMillis((ZonedDateTime) fromInstance));
            } else if (fromInstance instanceof Calendar) {
                return new BigDecimal(((Calendar) fromInstance).getTime().getTime());
            } else if (fromInstance instanceof Character) {
                return new BigDecimal(((Character) fromInstance));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'BigDecimal'", e);
        }
        nope(fromInstance, "BigDecimal");
        return null;
    }

    public static long localDateToMillis(LocalDate localDate) {
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * @param localDateTime A Java LocalDateTime
     *
     * @return a long representing the localDateTime as the number of milliseconds since the
     * number of milliseconds since Jan 1, 1970
     */
    public static long localDateTimeToMillis(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * @param zonedDateTime A Java ZonedDateTime
     *
     * @return a long representing the zonedDateTime as the number of milliseconds since the
     * number of milliseconds since Jan 1, 1970
     */
    public static long zonedDateTimeToMillis(ZonedDateTime zonedDateTime) {
        return zonedDateTime.toInstant().toEpochMilli();
    }

    private static String nope(Object fromInstance, String targetType) {
        if (fromInstance == null) {
            return null;
        }
        throw new IllegalArgumentException("Unsupported value type [" + name(fromInstance) + "] attempting to convert to '" + targetType + "'");
    }

    private static String name(Object fromInstance) {
        return fromInstance.getClass().getName() + " (" + fromInstance.toString() + ")";
    }


}
