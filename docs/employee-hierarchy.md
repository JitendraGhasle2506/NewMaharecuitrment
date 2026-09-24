# HR employee hierarchy

Open `/hr/employee-hierarchy`, or select **Employee hierarchy** on `/hr/reportingManager`.
The page and all hierarchy/photo APIs require `ROLE_HR`.
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

Returns `{success: true, message, data}`. Each recursive node includes employee ID, name, code, designation, department, protected photo URL, `children`, `hasChildren`, `totalChildren`, `filterMatch`, and `nextOffset`.
`nextOffset: null` means all direct children are included; `nextOffset: 0` with `hasChildren: true` means the branch is not loaded yet, not a leaf.
Requests contain at most 500 nodes. There is no logical hierarchy-depth limit: continue at any descendant using `nodeId`, keeping the original HOD ID in the URL.

`GET /api/employees/hierarchy/{hodEmployeeId}/search?q=EMP001` accepts the same filters and returns at most 20 matches with their root-to-employee paths, including unloaded branches.
`GET /api/employees/hierarchy/options` supplies HOD/department/designation selectors.

Two bulk projection queries build a graph per tree/search request; traversal is iterative and does not issue one database query per employee. Rendering and JSON payloads are lazy/paginated. For extremely large installations the snapshot queries may warrant a database recursive-CTE implementation or cache with mapping-change invalidation.

## Deployment

Restart/redeploy so post-schema migration **V133** runs. It adds `reporting_type`, backfills existing mappings as `PRIMARY`, and adds an index. Supported read filters are `PRIMARY`, `ADMINISTRATIVE`, `PROJECT`, `LEAVE_APPROVAL`. Existing mapping forms continue to create PRIMARY mappings; this change does not add an editor for other types or alter existing approval routing.

Employee photos use the existing managed upload policy through an HR-only proxy. Missing or invalid photos fall back to initials; filesystem paths are never sent to the browser.

Tests: `EmployeeHierarchyServiceTest`, `EmployeeHierarchyQueryTest`, `EmployeeHierarchyControllerTest`, `EmployeeHierarchyTemplateTest`, and the migration registration test. The query test compiles against the real Hibernate entity model without connecting to a database.

Run `node maharecruitment-web/src/test/js/employee-hierarchy-browser-test.mjs` from the repository root for dependency-free headless Chrome/Chromium tests with synthetic API responses. It checks expansion, collapse, search paths, duplicate-free branch merging, connectors, non-overlap, zoom, and desktop/tablet/mobile widths. Set `CHROME_BIN` if the browser is not in a standard location. Screenshots are generated in a temporary directory.
