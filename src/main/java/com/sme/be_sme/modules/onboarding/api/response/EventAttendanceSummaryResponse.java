package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class EventAttendanceSummaryResponse {
    private String eventInstanceId;
    private Integer totalInvited;
    private Integer attendedCount;
    private Integer notAttendedCount;
    private Double attendanceRate;
    private List<AttendeeItem> attendees = new ArrayList<>();

    @Getter
    @Setter
    public static class AttendeeItem {
        private String userId;
        private String fullName;
        private Boolean attended;
        private Integer doneTaskCount;
        private Integer totalTaskCount;
    }
}
