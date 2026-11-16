# Leave and tour approvals: who sees and who decides

Approvers open **Leave & Tour Approvals** (`/hod1/leaveApprovals`) or the mobile approvals API.
Both use `LeaveApplicationServiceImpl` / `TourApplicationServiceImpl`.

## Visibility: the whole team below you

An approver sees pending and processed leave/tour requests of their **team at every level**:

1. Direct authority: employees whose reporting mapping names the approver as manager
   (`manager_employee_id`) or HOD (`hod_user_id`), plus cell-authority employees without an
   explicit mapping (`ReportingManagerService.getEffectiveEmployeeIdsForAuthority`).
2. Every descendant of those employees, following each employee's newest PRIMARY mapping
   (`ReportingManagerService.getTeamEmployeeIdsForAuthority`). A mapping without a manager
   hangs under its HOD's employee record, as in the hierarchy views. Reporting cycles are cut
   and the approver's own requests are never included.

Example: Rohit reports to Mihir, and Mihir reports to Sachin. Rohit's request is visible to
Mihir, to Sachin and to anyone above Sachin.

## Decisions

A visible request shows **Approve/Reject** only when the signed-in approver may decide it.
Otherwise it shows **View only** with the reason as a tooltip.

| Request route | Who may decide |
|---|---|
| Two-level (recruitment type has an active `approval_recruitment_type_rule`, e.g. MAHAIT) | Only the stored approver of the current stage (manager, then HOD), or their additional-charge holder. Others in the chain view only. |
| Legacy (no active rule, e.g. INTERNAL) | Any approver whose team includes the employee. The first decision closes the request. |

INTERNAL employees' requests additionally need one of the **internal approver roles** set on
**Administration > Internal Approver Roles** (`/admin/internal-approver-roles`, ROLE_ADMIN only).
An approver without one of these roles views the request only; the View only tooltip names the
current roles. Give a user a role in user/role management.

## Managing internal approver roles

- Tick the roles that may approve or reject INTERNAL employees' leave and tour requests and save.
  At least one role must stay selected. A reason is optional and is stored with the change.
- The roles are read on every list and decision, so a change applies to the next approve/reject
  action, including requests already pending. Nobody needs to sign in again.
- The role only grants the decision; the approver must still have the employee in their team.
  MAHAIT and other recruitment types are not affected.
- **Change log**: each save that changes something records who, when, IP address, user agent,
  roles added/removed, roles before/after and the reason (`internal_approver_role_audit_log`).
  Saving an unchanged selection writes nothing.
- **Internal employee decisions**: the same screen lists approvals, rejections and forwards on
  INTERNAL employees' requests from `attendance_request_audit_log`, with the roles the approver held
  at the time. It filters on the employee's current recruitment type.

Deployment: restart to run post-schema migration **V160**. It creates `internal_approver_role`
(seeded with PM, STM, COO, HOD, MD, CTO and CFO, the previous fixed rule, so behaviour does not
change on deploy) and `internal_approver_role_audit_log` (with one SEEDED row), and adds the
Administration submenu for ROLE_ADMIN. Administrators see the menu after signing in again.

Server-side checks in `updateLeaveStatus` / `updateTourStatus` enforce the same rules for web
and mobile. A request that is view-only on screen is also rejected if submitted directly.

## Not changed

- Team attendance and the HOD dashboard still use their existing scopes.
- Stored approver IDs on two-level requests are still fixed when the employee applies.

## Checks

`ReportingManagerServiceImplTest`, `LeaveApplicationServiceImplTest`,
`TourApplicationServiceImplTest`, `AdditionalChargeApprovalServiceTest`,
`AdditionalChargeApprovalQueryTest`, `LeaveApprovalsTemplateTest`, `MobileLeaveServiceImplTest`,
`InternalEmployeeApprovalPolicyTest`, `InternalApproverRoleServiceTest`, `InternalApproverRoleQueryTest`,
`AdminInternalApproverRolePageControllerTest`, `V160InternalApproverRolesTest`.
