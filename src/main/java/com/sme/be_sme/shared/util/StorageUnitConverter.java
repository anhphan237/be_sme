package com.sme.be_sme.shared.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class StorageUnitConverter {

    private static final BigDecimal BYTES_PER_MB = BigDecimal.valueOf(1024L * 1024L);
    private static final BigDecimal BYTES_PER_GB = BigDecimal.valueOf(1024L * 1024L * 1024L);

    private StorageUnitConverter() {}

    public static Double toMb(Long bytes) {
        if (bytes == null || bytes < 0L) {
            return null;
        }
        return BigDecimal.valueOf(bytes)
                .divide(BYTES_PER_MB, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static Double toMb(long bytes) {
        if (bytes < 0L) {
            return null;
        }
        return BigDecimal.valueOf(bytes)
                .divide(BYTES_PER_MB, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static Double toGb(Long bytes) {
        if (bytes == null || bytes < 0L) {
            return null;
        }
        return BigDecimal.valueOf(bytes)
                .divide(BYTES_PER_GB, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static Double toGb(long bytes) {
        if (bytes < 0L) {
            return null;
        }
        return BigDecimal.valueOf(bytes)
                .divide(BYTES_PER_GB, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
