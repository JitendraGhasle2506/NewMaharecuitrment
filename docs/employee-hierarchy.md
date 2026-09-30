# HR employee hierarchy

Open `/hr/employee-hierarchy`, or select **Employee hierarchy** on `/hr/reportingManager`.
The page and all hierarchy/photo APIs require `ROLE_HR`.
The responsive workspace includes collapsible filters, visible-node/level counts, live name/code search with keyboard navigation, full-screen viewing where supported, and a floating zoom toolbar. Drag empty canvas space with a mouse, or scroll on touch devices. Mobile opens at 100% zoom with additional filters collapsed; **Fit** provides an overview of the entire visible tree.
On the HR page, the selected HOD is placed at the left and reporting levels flow left to right. Direct reports are stacked vertically around their manager, with connectors joining the manager's right edge to each report's left edge. The chart owns both scroll axes so large hierarchies do not widen the page. Employee, HOD, and executive hierarchy views retain their vertical layout.
To open a particular employee as the root, use `?hodEmployeeId=1001` (an **employee ID**, not a user ID).

## Data rules

- Uses `employee_master` and `employee_reporting_mapping`, not the separate position/team chart.
- `manager_employee_id` identifies the immediate manager. If absent, `hod_user_id` is resolved through `employee_master.user_id`.
- HOD choices include active employees with `ROLE_HOD` or whose user ID is referenced as an HOD in the reporting mappings. A user must have a linked employee record to appear.
- The newest mapping for an employee and reporting type wins. Invalid/self/circular links are omitted; an inactive/missing manager does not cause a report to be attached to another employee.
- Only ACTIVE employees with an active linked account (or no linked account) appear. Completed/relieved exits and non-cancelled/non-rejected exits on or before today are excluded. Pending future exits remain visible.
- Department/designation filters preserve connecting ancestors, displayed with a dashed border.
- Cell-level approval authorities are not treated as employee reporting assignments: L1/L2 approval rules do not by themselves establish a PRIMARY reporting chain.

## API

`GET /api/employees/hierarchy/{hodEmployeeId}?reportingType=PRIMARY`

Optional parameters: `departmentId`, `designationId`, `nodeId` (branch within the selected root), `offset` (default 0), `limit` (default 25, maximum 100), `depth` (default 1, maximum 8 **per response**).

Returns `{success: true, message, data}`. Each recursive node includes employee ID, name, code, designation, department, protected photo URL, `children`, `hasChildren`, `totalChildren`, `totalSubordinates`, `filterMatch`, and `nextOffset`.
`totalChildren` counts direct reports. `totalSubordinates` counts all direct and indirect descendants in the selected reporting type and filtered hierarchy, including connecting ancestors and collapsed/unloaded branches, excluding the employee themselves. Leaves return zero. Counts are aggregated once, bottom-up, before pagination; no additional database queries are needed.
`nextOffset: null` means all direct children are included; `nextOffset: 0` with `hasChildren: true` means the branch is not loaded yet, not a leaf.
Requests contain at most 500 nodes. There is no logical hierarchy-depth limit: continue at any descendant using `nodeId`, keeping the original HOD ID in the URL.

`GET /api/employees/hierarchy/{hodEmployeeId}/search?q=EMP001` accepts the same filters and returns at most 20 matches with their root-to-employee paths, including unloaded branches.
`GET /api/employees/hierarchy/options` supplies HOD/department/designation selectors.

Two bulk projection queries build a graph per tree/search request; traversal is iterative and does not issue one database query per employee. Rendering and JSON payloads are lazy/paginated. For extremely large installations the snapshot queries may warrant a database recursive-CTE implementation or cache with mapping-change invalidation.

## Deployment

Restart/redeploy so post-schema migration **V133** runs. It adds `reporting_type`, backfills existing mappings as `PRIMARY`, and adds an index. Supported read filters are `PRIMARY`, `ADMINISTRATIVE`, `PROJECT`, `LEAVE_APPROVAL`. Existing mapping forms continue to create PRIMARY mappings; this change does not add an editor for other types or alter existing approval routing.

The compact, transparent chart nodes display a circular photo, name, designation, and total subordinate count. Employee codes, departments, and badges are not rendered on nodes. Small icon-only controls retain expansion and pagination, while employee-code search and department filtering remain available in the toolbar. Expanding, collapsing, searching, or loading another page does not reduce the total subordinate count to just the visible employees.

Employee photos use the existing managed upload policy through an HR-only proxy. Missing or invalid photos fall back to the local default SVG avatar; filesystem paths are never sent to the browser.

Tests: `EmployeeHierarchyServiceTest`, `EmployeeHierarchyQueryTest`, `EmployeeHierarchyControllerTest`, `EmployeeHierarchyTemplateTest`, and the migration registration test. The query test compiles against the real Hibernate entity model without connecting to a database.

Run `node maharecruitment-web/src/test/js/employee-hierarchy-browser-test.mjs` from the repository root for dependency-free headless Chrome/Chromium tests with synthetic API responses. It checks the HR-only horizontal placement and connector endpoints, preserved vertical employee layout, expansion, collapse, stationary managers, search paths, duplicate-free branch merging, non-overlap, internal scrolling, zoom, and desktop/tablet/mobile widths. Set `CHROME_BIN` if the browser is not in a standard location. Screenshots are generated in a temporary directory.
