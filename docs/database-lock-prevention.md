# Database lock prevention

The employee onboarding guard now checks PostgreSQL catalogs before changing data or constraints.
A completed schema produces no updates or `ALTER TABLE`. The pre-onboarding guard similarly
checks existing columns and nullability before making changes.

Employee onboarding repairs use an explicit transaction, a five-second local lock timeout,
and a thirty-second statement timeout. The caller commits or rolls back when it owns that
transaction; Flyway retains transaction ownership for migrations. Catalog failures propagate
instead of being mistaken for missing columns.

All Flyway connections use a five-second lock timeout. Their previous setting is restored
before the connection returns to Hikari. An unfinished migration transaction is rolled back
before cleanup; a connection whose settings cannot be restored is discarded.

New application connections identify themselves with `spring.application.name` and set
`idle_in_transaction_session_timeout=120s`. This releases abandoned application transactions,
not ordinary idle pooled connections. It does not fix an external SQL editor with an open
transaction. Keep those tools in auto-commit mode for diagnostics and explicitly complete
intentional transactions.

## Inspect a blocker without changing database state

Run `tools/diagnostics/DatabaseLockDiagnostics.java` with Java 21 and the PostgreSQL JDBC jar
on the classpath from the repository root. It reads the existing local datasource configuration,
prints session identifiers, application/client details, transaction age and blocking PIDs,
and does not print passwords or query literals. It never cancels or terminates sessions.
Add `maharecruitment-web/target/classes` to the classpath and pass `--check-guard` to verify
the employee guard on a read-only connection. The current database returned
`readonly_employee_guard_changed=false`, confirming that the repaired guard skips alteration.

If the prior blocker has disconnected, its owner cannot be reconstructed from
`pg_stat_activity`; database logs are needed. Capture the diagnostic output while a wait is
present. For an identified external session, have its owner commit or roll back. Do not
terminate a transaction containing uncommitted work without the owner's approval.

## Controlled migrations and Hibernate validation

V134 reconciles employee onboarding and V135 reconciles the legacy pre-onboarding repairs
once through the existing `flyway_post_schema_history` table. Neither migration rewrites
already-completed schema changes.

The opt-in `schema-validate` profile runs the registered Flyway migrations **before** Hibernate
validation, disables the legacy schema guards, and refuses automatic migration-history repair.
For a database whose baseline and migration coverage have been verified, launch with:

```text
--spring.profiles.active=local,schema-validate
```

Use the appropriate environment profile instead of `local`. A validation failure identifies
a schema difference that must receive a reviewed versioned migration. Test this profile on
a database copy before rollout. Do not use it to provision an empty database: the existing
migrations assume that Hibernate-created baseline tables already exist. The default remains
`ddl-auto=update` until complete baseline coverage is verified; automatically switching every
environment would break databases with missing tables or columns.

Normal startup has not been run against the shared database to apply these migrations as part
of this change. The read-only inspection found no current blocked/idle transactions and confirmed
that `employee_master.mahait_onboarding_date` already has its `NOT NULL` constraint.
