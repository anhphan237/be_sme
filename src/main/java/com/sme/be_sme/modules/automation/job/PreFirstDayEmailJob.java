package com.sme.be_sme.modules.automation.job;

import com.sme.be_sme.modules.automation.service.EmailSenderService;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapper;
import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
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
 * Daily job: send PRE_FIRST_DAY email to employees whose onboarding start_date is tomorrow.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PreFirstDayEmailJob {

    private static final String TEMPLATE_PRE_FIRST_DAY = "PRE_FIRST_DAY";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final OnboardingInstanceMapperExt onboardingInstanceMapperExt;
    private final EmployeeProfileMapper employeeProfileMapper;
    private final CompanyMapper companyMapper;
    private final EmailSenderService emailSenderService;

    @Scheduled(cron = "${app.automation.pre-first-day.cron:0 0 8 * * ?}")
    public void run() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Date startDate = Date.from(tomorrow.atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<OnboardingInstanceEntity> instances = onboardingInstanceMapperExt.selectByStartDateAndActive(startDate);
        if (instances == null || instances.isEmpty()) return;
        log.info("PreFirstDayEmailJob: sending {} pre-first-day emails", instances.size());
        for (OnboardingInstanceEntity instance : instances) {
            try {
                sendPreFirstDay(instance, tomorrow);
            } catch (Exception e) {
                log.warn("PreFirstDayEmailJob: failed for onboarding {}: {}", instance.getOnboardingId(), e.getMessage());
            }
        }
    }

    private void sendPreFirstDay(OnboardingInstanceEntity instance, LocalDate startDate) {
        if (!StringUtils.hasText(instance.getEmployeeId())) return;
        EmployeeProfileEntity employee = employeeProfileMapper.selectByPrimaryKey(instance.getEmployeeId());
        if (employee == null || !StringUtils.hasText(employee.getEmployeeEmail())) return;
        String companyName = "";
        CompanyEntity company = companyMapper.selectByPrimaryKey(instance.getCompanyId());
        if (company != null && StringUtils.hasText(company.getName())) companyName = company.getName();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("employeeName", StringUtils.hasText(employee.getEmployeeName()) ? employee.getEmployeeName() : "there");
        placeholders.put("companyName", companyName);
        placeholders.put("startDate", startDate.format(DATE_FMT));
        emailSenderService.sendWithTemplate(instance.getCompanyId(), TEMPLATE_PRE_FIRST_DAY, employee.getEmployeeEmail(),
                placeholders, employee.getUserId(), instance.getOnboardingId());
    }
}
