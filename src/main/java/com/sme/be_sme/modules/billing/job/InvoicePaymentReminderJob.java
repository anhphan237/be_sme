package com.sme.be_sme.modules.billing.job;

import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserRoleMapperExt;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.notification.service.NotificationCreateParams;
import com.sme.be_sme.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Daily job: remind about invoices due in 1-3 days (status ISSUED). Notifies company ADMIN users.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvoicePaymentReminderJob {

    private static final String TEMPLATE_PAYMENT_REMINDER = "PAYMENT_REMINDER";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final InvoiceMapperExt invoiceMapperExt;
    private final UserRoleMapperExt userRoleMapperExt;
    private final UserMapperExt userMapperExt;
    private final NotificationService notificationService;

    @Scheduled(cron = "${app.billing.payment-reminder.cron:0 0 9 * * ?}")
    public void run() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.plusDays(1);
        LocalDate to = today.plusDays(3);
        Date fromDate = Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDate = Date.from(to.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());
        List<InvoiceEntity> invoices = invoiceMapperExt.selectDueBetweenAndStatusIssued(fromDate, toDate);
        if (invoices == null || invoices.isEmpty()) return;
        log.info("InvoicePaymentReminderJob: sending {} payment reminders", invoices.size());
        for (InvoiceEntity invoice : invoices) {
            try {
                sendReminder(invoice);
            } catch (Exception e) {
                log.warn("InvoicePaymentReminderJob: failed for invoice {}: {}", invoice.getInvoiceId(), e.getMessage());
            }
        }
    }

    private void sendReminder(InvoiceEntity invoice) {
        String companyId = invoice.getCompanyId();
        List<String> adminUserIds = userRoleMapperExt.selectUserIdsByCompanyAndRoleCode(companyId, "ADMIN");
        if (adminUserIds == null || adminUserIds.isEmpty()) return;
        String dueStr = invoice.getDueAt() != null
                ? invoice.getDueAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FMT)
                : "";
        String invoiceNo = StringUtils.hasText(invoice.getInvoiceNo()) ? invoice.getInvoiceNo() : invoice.getInvoiceId();
        int amount = invoice.getAmountTotal() != null ? invoice.getAmountTotal() : 0;
        String currency = StringUtils.hasText(invoice.getCurrency()) ? invoice.getCurrency() : "VND";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("invoiceNo", invoiceNo);
        placeholders.put("dueDate", dueStr);
        placeholders.put("amountTotal", String.valueOf(amount));
        placeholders.put("currency", currency);
        for (String userId : adminUserIds) {
            UserEntity user = userMapperExt.selectByCompanyIdAndUserId(companyId, userId);
            if (user == null) continue;
            NotificationCreateParams params = NotificationCreateParams.builder()
                    .companyId(companyId)
                    .userId(userId)
                    .type("PAYMENT_REMINDER")
                    .title("Invoice " + invoiceNo + " due on " + dueStr)
                    .content("Invoice " + invoiceNo + " (" + amount + " " + currency + ") is due on " + dueStr + ".")
                    .refType("INVOICE")
                    .refId(invoice.getInvoiceId())
                    .sendEmail(true)
                    .emailTemplate(TEMPLATE_PAYMENT_REMINDER)
                    .emailPlaceholders(placeholders)
                    .toEmail(user.getEmail())
                    .build();
            notificationService.create(params);
        }
    }
}
