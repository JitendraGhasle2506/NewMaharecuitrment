# Java constructor errors in VS Code

This project targets Java 21. If VS Code reports these errors on Lombok
annotations such as `@AllArgsConstructor`, `@RequiredArgsConstructor` or `@Builder`:

- `The Java feature 'Flexible Constructor Bodies' is only available with source level 25 and above`
- `Cannot assign field '...' in an early construction context, because it has an initializer`

check the installed **Language Support for Java by Red Hat** extension version.
Version 1.57.0 ships an Eclipse compiler affected by a constructor AST regression.
The same error can occur with Lombok 1.18.48; upgrading Lombok alone does not
resolve this compiler regression.

The upstream reports describe the affected compiler and the compatibility fix:
[Lombok #4086](https://github.com/projectlombok/lombok/issues/4086) and
[Eclipse JDT #5490](https://github.com/eclipse-jdt/eclipse.jdt.core/pull/5490).

## Workaround

Use Java extension **1.56.0**, released before the compiler regression:

1. Open Extensions and select **Language Support for Java by Red Hat**.
2. Select the gear menu, **Install Another Version...**, then **1.56.0**.
3. Disable automatic updates for this extension temporarily, so 1.57.0 is not
   installed again.
4. Run **Developer: Reload Window**.
5. Run **Java: Clean Java Language Server Workspace** and allow it to restart.

The equivalent installation command is:

```powershell
code --install-extension redhat.java@1.56.0 --force --do-not-sync
```

Return to a newer Java extension once its bundled Eclipse compiler includes the
upstream compatibility fix. Keep the Maven Java release at 21 and retain the
Lombok annotations and entity field initializers.

## Verify the build

Set `JAVA_HOME` to an installed JDK 21 and run from the repository root:

```powershell
.\mvnw.cmd -DskipTests compile
```

Use `-o` if the required dependencies are already cached and an offline build is
needed. Maven uses javac, so this build can succeed while the affected Eclipse
compiler reports editor errors.

The pasted diagnostics also include warnings about null annotations, unused
code, deprecated APIs, Spring Data repositories and Spring Boot versions. Those
are separate from this constructor regression and require their own review.
