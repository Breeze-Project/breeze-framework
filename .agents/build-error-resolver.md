# build-error-resolver

Resolves Gradle build and compilation errors for multi-module Paper/Folia plugins on Java 21 + Canvas 1.21.11.

## Common Issues

- Missing Canvas API snapshots from PaperMC/Canvas maven repos
- Flyway classpath resolution for vendor-specific migration directories
- Fatjar duplicates strategy (`DuplicatesStrategy.EXCLUDE`)
- Reflection access to Bukkit internals (`commandMap`, `knownCommands`, `syncCommands`)
- PostHog server SDK shading conflicts
