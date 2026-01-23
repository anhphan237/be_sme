package com.sme.be_sme.shared.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Time-based String ID generator.
 *
 * Format: yyyyMMddHHmmssSSS + seq(3)
 * Example: 20260122221930123001
 *
 * Notes:
 * - Sortable by time (lexicographically).
 * - Thread-safe within a single JVM.
 * - Not multi-node unique (Snowflake can replace later).
 */
public final class UuidGenerator {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    // sequence resets when millisecond changes
    private static long lastMillis = -1L;
    private static int seq = 0;

    private UuidGenerator() {
    }

    public static String generate() {
        long now = System.currentTimeMillis();
        int localSeq;

        synchronized (UuidGenerator.class) {
            if (now != lastMillis) {
                lastMillis = now;
                seq = 0;
            } else {
                seq++;
                // keep within 3 digits (000-999)
                if (seq > 999) {
                    // busy-wait until next millisecond to avoid overflow duplicates
                    do {
                        now = System.currentTimeMillis();
                    } while (now == lastMillis);
                    lastMillis = now;
                    seq = 0;
                }
            }
            localSeq = seq;
        }

        String ts = SDF.format(new Date(now));
        return ts + String.format("%03d", localSeq);
    }

    /**
     * Optional: generate with a custom date (mostly for tests).
     */
    public static String generate(Date date) {
        if (date == null) {
            return generate();
        }
        long now = date.getTime();
        int localSeq;

        synchronized (UuidGenerator.class) {
            if (now != lastMillis) {
                lastMillis = now;
                seq = 0;
            } else {
                seq++;
                if (seq > 999) {
                    // cannot advance external provided date, so fallback to system time next ms
                    return generate();
                }
            }
            localSeq = seq;
        }

        String ts = SDF.format(date);
        return ts + String.format("%03d", localSeq);
    }
}

