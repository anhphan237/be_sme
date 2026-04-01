# Onboarding Task Schedule Operations

## Collection variables
- `{{baseUrl}}`
- `{{token}}`
- `{{taskId}}`

## Propose schedule
`POST {{baseUrl}}/gateway`

```json
{
  "operation": "com.sme.onboarding.task.schedule.propose",
  "payload": {
    "taskId": "{{taskId}}",
    "scheduledStartAt": "2026-04-03T08:00:00Z",
    "scheduledEndAt": "2026-04-03T09:00:00Z"
  }
}
```

## Confirm schedule
`POST {{baseUrl}}/gateway`

```json
{
  "operation": "com.sme.onboarding.task.schedule.confirm",
  "payload": {
    "taskId": "{{taskId}}"
  }
}
```

## Reschedule
`POST {{baseUrl}}/gateway`

```json
{
  "operation": "com.sme.onboarding.task.schedule.reschedule",
  "payload": {
    "taskId": "{{taskId}}",
    "scheduledStartAt": "2026-04-04T08:00:00Z",
    "scheduledEndAt": "2026-04-04T09:00:00Z",
    "reason": "Manager unavailable at original slot"
  }
}
```

## Cancel schedule
`POST {{baseUrl}}/gateway`

```json
{
  "operation": "com.sme.onboarding.task.schedule.cancel",
  "payload": {
    "taskId": "{{taskId}}",
    "reason": "Candidate requested another day"
  }
}
```

## Mark no-show
`POST {{baseUrl}}/gateway`

```json
{
  "operation": "com.sme.onboarding.task.schedule.markNoShow",
  "payload": {
    "taskId": "{{taskId}}",
    "reason": "Assignee did not join meeting"
  }
}
```

## DONE guard behavior
- If schedule metadata exists (`scheduledStartAt`/`scheduleStatus`) and status is not `CONFIRMED`, `status=DONE` returns:
  - `BAD_REQUEST`
  - message: `confirm task schedule before marking done`
