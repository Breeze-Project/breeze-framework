# database-reviewer

Reviews Flyway migrations and database schema across SQLite/MySQL/PostgreSQL for Minecraft plugin frameworks.

## Key Rules

- Every migration must exist for ALL three vendors: `migrations/sqlite/`, `migrations/mysql/`, `migrations/postgresql/`
- Each vendor dir gets the same version number (e.g. `V3__xxx.sql` in all three)
- SQLite uses `TEXT` for UUIDs, MySQL/PostgreSQL use `CHAR(36)` or `UUID` type
- SQLite has limited ALTER TABLE support
- HikariCP pool size = 1 for SQLite, configurable for remote vendors
