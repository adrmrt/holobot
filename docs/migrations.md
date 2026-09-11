# Database migrations

Rules for writing and maintaining Flyway migrations in `src/main/resources/db/migration`.

## Idempotency

A migration MUST be safe to run more than once against the same database without error or side effect, wherever SQLite's grammar makes that possible.
Flyway itself only runs a given migration once per database, so this rule exists for two other cases: re-running a migration by hand while debugging, and applying the same migration to multiple environments that may already be partway migrated.

Use the guarded form of each DDL statement instead of the bare form, where SQLite supports one:

| Instead of                  | Write                                     |
|-----------------------------|-------------------------------------------|
| `CREATE TABLE t (...)`      | `CREATE TABLE IF NOT EXISTS t (...)`      |
| `DROP TABLE t`              | `DROP TABLE IF EXISTS t`                  |
| `CREATE INDEX i ON t (...)` | `CREATE INDEX IF NOT EXISTS i ON t (...)` |

> [!WARNING]
> SQLite's `ALTER TABLE ADD COLUMN` and `DROP COLUMN` have no `IF NOT EXISTS` / `IF EXISTS` clause — that guard only exists for `CREATE TABLE`, `DROP TABLE`, and `CREATE INDEX`. A column-adding or column-dropping migration cannot be made idempotent through plain SQL.

A migration that cannot be made idempotent MUST say so in a comment at the top of the file, along with the reason — this applies to essentially every `ADD COLUMN` / `DROP COLUMN` migration in this project.
If a specific migration truly needs to be re-runnable (for example, a repeated hand-fix in a broken environment), write it as a Flyway Java migration that checks `PRAGMA table_info(t)` before altering, instead of relying on plain SQL.

## Immutability once merged

A migration file MUST NOT be modified after it has been merged to `main`.
Any database that already ran the old version recorded its checksum; changing the file breaks validation for every database that applied it, including production.

> [!WARNING]
> If a merged migration turns out to be wrong, write a new migration that corrects it. Never edit the old one, even to fix a typo in a comment.

This rule applies from the moment a migration lands on `main`, not from the moment it is written.
Migrations on a feature branch that has not merged yet MAY be edited or renumbered freely, since no shared database has recorded them.

## One logical change per migration file

A migration file MUST cover one schema change with one clear purpose: one new table with its indexes, one column addition, one constraint change.
Bundling unrelated changes into a single file makes it harder to review, harder to explain in the schema history, and harder to isolate if one part fails.

This rule governs the file, not the PR.
A migration SHOULD ship in the same PR as the application code that depends on it — splitting those into separate PRs risks a deploy where the code and schema are out of sync.
The rule only applies when two schema changes do not depend on each other and do not serve the same feature, even if they were both written while working on the same branch.
If a table, its indexes, and several related columns all exist to serve one feature, that is still one migration.

## Forward-only

Migrations MUST NOT rely on a corresponding "down" migration existing or ever being written.
To undo a change, write a new forward migration that reverts it.

## No dependence on application code or current data shape

A migration MUST work correctly regardless of what data already exists in the columns it touches.
A backfill or data migration MUST handle `NULL` and unexpected existing values defensively, since it may run against databases seeded at different points in the project's history.

## Destructive changes

A migration that drops a column, drops a table, or otherwise discards data MUST call this out explicitly in its PR description.
Destructive changes MUST NOT be combined with additive changes in the same file (see "one logical change per migration").

## Naming and numbering

Migration files MUST follow Flyway's `V<version>__<description>.sql` naming convention, with the description in `snake_case`.
Version numbers MUST increase sequentially with no gaps and MUST NOT be reused, even for a migration that was reverted before merging.

## Testing

A migration SHOULD be tested against a database that already has data in it, not only a freshly baselined one.
A fresh database can hide problems — such as a `NOT NULL` column addition failing against existing rows — that only appear against realistic, already-populated data.
