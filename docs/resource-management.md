# Resource management

Open **Manage Resources** in the sidebar or the employee dashboard, or visit
`/reporting-manager/resources` under the application's context path.

The responsive workspace shows primary resources mapped directly to the signed-in
employee and to their descendants. The free-resource view contains active,
non-relieved employees with no record in `employee_reporting_mapping` of any
reporting type. It excludes the signed-in employee. Search covers name, employee
code, designation, department and reporting-person name; results are paginated.

Choose **Map resource** to assign an available person to yourself or one of your
subordinates. Choose **Release** to remove a person's primary reporting assignment
after confirmation. Releasing a manager preserves assignments of their own reports;
that branch leaves the former manager's reporting scope. Other reporting types are
preserved, so an employee with another assignment does not enter the free list.

## Access and assignment rules

- Requires EMPLOYEE, HOD, STM, PM, COO, CTO, CFO or MD authority and an active employee
  linked to the session user. Identity comes from the session, never a request user ID.
- Permanent stored PRIMARY reporting links determine write access. Additional-charge
  coverage alone does not authorize permanent mapping changes.
- Only the current user's descendants can be released; only the current user or a
  descendant can be selected as a new reporting person.
- Existing mappings of any reporting type block new mapping. Self-assignment and
  reporting cycles are rejected. A circular existing chain fails closed with a
  message asking HR to correct it.
- Newest PRIMARY rows determine ownership. Release removes all PRIMARY rows for the
  selected employee to prevent an older assignment from reappearing.
- Mutations are transactional and obtain a pessimistic lock on the resource employee
  before checking availability and scope. Concurrent assignments through this screen
  serialize per resource. Legacy HR writers do not acquire this new lock; a global
  guarantee across those writers would require a shared locking policy or a database
  constraint after cleaning up any historical duplicate rows.
- Existing CSRF protection applies to both POST actions. Successful mutations log
  actor/resource IDs through the application's logger.

## Structure and performance

`ResourceManagementController` handles HTTP/session/validation concerns.
`ResourceManagementService` handles listing and transactional assignment operations.
`ResourceReportingScopeResolver` resolves reporting ownership independently, using
repository interfaces and constructor injection. UI markup, styles and event handlers
live in separate files, with no inline JavaScript or unescaped employee HTML.

The resolver uses two bulk projection queries and iterative linear graph traversal,
without recursive calls or one query per subordinate. Listing adds a distinct mapped-ID
query and returns a bounded page (maximum 100 resources). The snapshot is recomputed
for each request so releases and new assignments are reflected immediately. Very large
installations may need recursive database queries and database-level pagination; the
current strategy follows the existing hierarchy implementation.

No schema migration is needed. Normal application rebuild/restart is required.

## Verification

From the repository root, using JDK 21:

```powershell
.\mvnw.cmd -o -pl maharecruitment-web -am '-Dtest=ResourceManagementServiceTest,ResourceManagementControllerTest,EmployeeHierarchyQueryTest,EmployeeHierarchyServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
node maharecruitment-web/src/test/js/resource-management-browser-test.mjs
```

The browser test requires Node 22+ and Chrome/Edge (`CHROME_BIN` can override its path).
It uses synthetic API data and checks desktop/tablet/mobile widths, mapping, release,
cancel, search, pagination, CSRF headers, escaped names and retry. It writes screenshots
to a temporary directory. The Java tests validate scope rules, conflicts, role access,
session identity, input validation and query compilation against the entity model.
They do not connect to the production database or validate live application data.
