package com.sme.be_sme.modules.onboarding.api.response;

import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskScheduleCalendarResponse {
    private String targetUserId;
    private boolean selfView;
    private int totalCount;
    private int page;
    private int size;
    private List<CalendarItem> items;

    @Getter
    @Setter
    public static class CalendarItem {
        private Date scheduledStartAt;
        private Date scheduledEndAt;
        private String taskId;
        private String title;
        private String status;
        private String onboardingId;
        private String checklistName;
    }
}
