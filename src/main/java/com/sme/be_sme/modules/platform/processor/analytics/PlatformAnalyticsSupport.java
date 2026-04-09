package com.sme.be_sme.modules.platform.processor.analytics;

import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class PlatformAnalyticsSupport {

    static final String GROUP_BY_DAY = "DAY";
    static final String GROUP_BY_MONTH = "MONTH";
    static final String GROUP_BY_YEAR = "YEAR";

    private PlatformAnalyticsSupport() {
    }

    public static Date parseDate(String isoDate, boolean startOfDay) {
        if (!StringUtils.hasText(isoDate)) {
            return null;
        }
        LocalDate ld = LocalDate.parse(isoDate);
        if (startOfDay) {
            return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        return Date.from(ld.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDate parseLocalDate(String isoDate, LocalDate fallback) {
        if (!StringUtils.hasText(isoDate)) {
            return fallback;
        }
        return LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static boolean inRange(Date value, Date start, Date endExclusive) {
        if (value == null) {
            return false;
        }
        if (start != null && value.before(start)) {
            return false;
        }
        if (endExclusive != null && !value.before(endExclusive)) {
            return false;
        }
        return true;
    }

    public static String normalizeGroupBy(String groupBy) {
        if (!StringUtils.hasText(groupBy)) {
            return GROUP_BY_MONTH;
        }
        String normalized = groupBy.trim().toUpperCase(Locale.ROOT);
        if (GROUP_BY_DAY.equals(normalized) || GROUP_BY_MONTH.equals(normalized) || GROUP_BY_YEAR.equals(normalized)) {
            return normalized;
        }
        return GROUP_BY_MONTH;
    }

    static List<String> buildBuckets(LocalDate start, LocalDate end, String groupBy) {
        groupBy = normalizeGroupBy(groupBy);
        List<String> buckets = new ArrayList<>();
        if (start == null || end == null || end.isBefore(start)) {
            return buckets;
        }
        LocalDate cursor = alignStart(start, groupBy);
        LocalDate boundary = alignStart(end, groupBy);
        while (!cursor.isAfter(boundary)) {
            buckets.add(formatBucket(cursor, groupBy));
            cursor = step(cursor, groupBy, 1);
        }
        return buckets;
    }

    public static LocalDate previousPeriodStart(LocalDate start, LocalDate end, String groupBy) {
        long units = bucketDistance(start, end, groupBy) + 1;
        return step(alignStart(start, groupBy), groupBy, -units);
    }

    public static LocalDate previousPeriodEnd(LocalDate start, LocalDate end, String groupBy) {
        return step(alignStart(start, groupBy), groupBy, -1);
    }

    static String bucketOf(Date date, String groupBy) {
        if (date == null) {
            return null;
        }
        LocalDate local = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return formatBucket(local, groupBy);
    }

    static String formatBucket(LocalDate date, String groupBy) {
        groupBy = normalizeGroupBy(groupBy);
        if (GROUP_BY_DAY.equals(groupBy)) {
            return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (GROUP_BY_YEAR.equals(groupBy)) {
            return String.valueOf(date.getYear());
        }
        return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
    }

    static LocalDate bucketToDate(String bucket, String groupBy) {
        groupBy = normalizeGroupBy(groupBy);
        if (GROUP_BY_DAY.equals(groupBy)) {
            return LocalDate.parse(bucket, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (GROUP_BY_YEAR.equals(groupBy)) {
            return LocalDate.of(Integer.parseInt(bucket), 1, 1);
        }
        String[] parts = bucket.split("-");
        return LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 1);
    }

    static LocalDate alignStart(LocalDate date, String groupBy) {
        groupBy = normalizeGroupBy(groupBy);
        if (GROUP_BY_YEAR.equals(groupBy)) {
            return LocalDate.of(date.getYear(), 1, 1);
        }
        if (GROUP_BY_MONTH.equals(groupBy)) {
            return LocalDate.of(date.getYear(), date.getMonthValue(), 1);
        }
        return date;
    }

    static LocalDate step(LocalDate date, String groupBy, long amount) {
        groupBy = normalizeGroupBy(groupBy);
        if (GROUP_BY_YEAR.equals(groupBy)) {
            return date.plusYears(amount);
        }
        if (GROUP_BY_MONTH.equals(groupBy)) {
            return date.plusMonths(amount);
        }
        return date.plusDays(amount);
    }

    static long bucketDistance(LocalDate start, LocalDate end, String groupBy) {
        groupBy = normalizeGroupBy(groupBy);
        LocalDate s = alignStart(start, groupBy);
        LocalDate e = alignStart(end, groupBy);
        if (GROUP_BY_YEAR.equals(groupBy)) {
            return ChronoUnit.YEARS.between(s, e);
        }
        if (GROUP_BY_MONTH.equals(groupBy)) {
            return ChronoUnit.MONTHS.between(s, e);
        }
        return ChronoUnit.DAYS.between(s, e);
    }

    public static Double growth(Number current, Number previous) {
        double c = current == null ? 0.0 : current.doubleValue();
        double p = previous == null ? 0.0 : previous.doubleValue();
        if (p == 0.0) {
            return c == 0.0 ? 0.0 : 1.0;
        }
        return (c - p) / p;
    }

    public static String companyId(OnboardingInstanceEntity entity) {
        return readString(entity, "getCompanyId", "companyId");
    }

    static String companyId(UserEntity entity) {
        return entity != null ? entity.getCompanyId() : null;
    }

    static String companyId(SubscriptionEntity entity) {
        return entity != null ? entity.getCompanyId() : null;
    }

    static String companyName(CompanyEntity entity) {
        return entity != null ? entity.getName() : null;
    }

    static String planCode(PlanEntity plan) {
        return readString(plan, "getCode", "code");
    }

    static String planName(PlanEntity plan) {
        return readString(plan, "getName", "name");
    }

    static String planId(PlanEntity plan) {
        return plan != null ? plan.getPlanId() : null;
    }

    static String role(UserEntity user) {
        return readString(user, "getRoleCode", "roleCode", "getRole", "role");
    }

    public static boolean isEmployee(UserEntity user) {
        String role = role(user);
        if (!StringUtils.hasText(role)) {
            return true;
        }
        return !"ADMIN".equalsIgnoreCase(role)
                && !"HR".equalsIgnoreCase(role)
                && !"HR_ADMIN".equalsIgnoreCase(role)
                && !"MANAGER".equalsIgnoreCase(role)
                && !"IT".equalsIgnoreCase(role)
                && !"PLATFORM_ADMIN".equalsIgnoreCase(role);
    }

    static String safePlanCode(PlanEntity plan) {
        String code = planCode(plan);
        return StringUtils.hasText(code) ? code : "UNKNOWN";
    }

    static String safePlanName(PlanEntity plan) {
        String name = planName(plan);
        return StringUtils.hasText(name) ? name : "Unknown";
    }

    static String readString(Object target, String... methodsOrFields) {
        if (target == null) {
            return null;
        }
        for (String name : methodsOrFields) {
            if (name == null) {
                continue;
            }
            try {
                if (name.startsWith("get")) {
                    Method method = target.getClass().getMethod(name);
                    Object value = method.invoke(target);
                    if (value != null) {
                        return String.valueOf(value);
                    }
                } else {
                    Field field = target.getClass().getDeclaredField(name);
                    field.setAccessible(true);
                    Object value = field.get(target);
                    if (value != null) {
                        return String.valueOf(value);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static Integer readInteger(Object target, String... methodsOrFields) {
        String value = readString(target, methodsOrFields);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    static Map<String, Integer> seedIntMap(List<String> buckets) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String bucket : buckets) {
            map.put(bucket, 0);
        }
        return map;
    }

    static Map<String, Double> seedDoubleMap(List<String> buckets) {
        Map<String, Double> map = new LinkedHashMap<>();
        for (String bucket : buckets) {
            map.put(bucket, 0.0);
        }
        return map;
    }
}
