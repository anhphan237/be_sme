package com.sme.be_sme.modules.billing.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.SubscriptionHistoryRequest;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * Resolves overlap filter range for subscription plan history / timeline from
 * optional {@code year}, {@code fromDate}, {@code toDate} (yyyy-MM-dd).
 */
public final class SubscriptionHistoryQuerySupport {

    private static final int MIN_YEAR = 1970;
    private static final int MAX_YEAR = 2100;

    private SubscriptionHistoryQuerySupport() {
    }

    /**
     * Converts gateway payload to request and reapplies {@code year} from raw JSON when needed
     * (some Jackson configs or mixed clients omit {@code year} on POJO binding).
     */
    public static SubscriptionHistoryRequest parseRequest(JsonNode payload, ObjectMapper mapper) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            return new SubscriptionHistoryRequest();
        }
        SubscriptionHistoryRequest req = mapper.convertValue(payload, SubscriptionHistoryRequest.class);
        if (req == null) {
            req = new SubscriptionHistoryRequest();
        }
        JsonNode yearNode = payload.get("year");
        if (yearNode != null && !yearNode.isNull()) {
            if (yearNode.isNumber()) {
                req.setYear(yearNode.intValue());
            } else if (yearNode.isTextual()) {
                try {
                    req.setYear(Integer.parseInt(yearNode.asText().trim()));
                } catch (NumberFormatException ignored) {
                    // leave as-is from convertValue
                }
            }
        }
        return req;
    }

    public record DateRange(Date fromTs, Date toTs) {
    }

    /**
     * When {@code year} is set, restricts to that calendar year intersected with any fromDate/toDate bounds.
     *
     * @return fromTs/toTs may be null = no date bound on that side
     */
    public static DateRange resolve(SubscriptionHistoryRequest request) {
        ZoneId zone = ZoneId.systemDefault();
        Date fromTs = null;
        Date toTs = null;
        if (request != null) {
            fromTs = parseStartOfDay(request.getFromDate(), "fromDate", zone);
            toTs = parseEndOfDay(request.getToDate(), "toDate", zone);
        }

        Integer year = request != null ? request.getYear() : null;
        if (year != null) {
            if (year < MIN_YEAR || year > MAX_YEAR) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "year must be between " + MIN_YEAR + " and " + MAX_YEAR);
            }
            LocalDate yStart = LocalDate.of(year, 1, 1);
            LocalDate yEnd = LocalDate.of(year, 12, 31);
            Date yFrom = Date.from(yStart.atStartOfDay(zone).toInstant());
            Date yTo = endOfLocalDay(yEnd, zone);

            if (fromTs == null) {
                fromTs = yFrom;
            } else {
                fromTs = maxDate(fromTs, yFrom);
            }
            if (toTs == null) {
                toTs = yTo;
            } else {
                toTs = minDate(toTs, yTo);
            }
        }

        if (fromTs != null && toTs != null && fromTs.after(toTs)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "fromDate, toDate, and year produce an empty range");
        }
        return new DateRange(fromTs, toTs);
    }

    private static Date maxDate(Date a, Date b) {
        return a.after(b) ? a : b;
    }

    private static Date minDate(Date a, Date b) {
        return a.before(b) ? a : b;
    }

    private static Date endOfLocalDay(LocalDate date, ZoneId zone) {
        return Date.from(date.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1));
    }

    private static Date parseStartOfDay(String value, String fieldName, ZoneId zone) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(value.trim());
            return Date.from(date.atStartOfDay(zone).toInstant());
        } catch (DateTimeParseException e) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, fieldName + " must be yyyy-MM-dd");
        }
    }

    private static Date parseEndOfDay(String value, String fieldName, ZoneId zone) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(value.trim());
            return endOfLocalDay(date, zone);
        } catch (DateTimeParseException e) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, fieldName + " must be yyyy-MM-dd");
        }
    }
}
