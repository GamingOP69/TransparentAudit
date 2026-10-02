# Upstream/tooling stack notes (researched 2026-10-02)

## Runtime compatibility

TransparentAudit deliberately targets the Bukkit/Spigot 1.8.8 API and emits Java 8 bytecode. The plugin declares `spigot-api:1.8.8-R0.1-SNAPSHOT` as `provided`, so the server supplies the API at runtime and the plugin does not bundle it.

Spigot's Maven repository still lists the `1.8.8-R0.1-SNAPSHOT` API and its timestamped artifacts:

https://hub.spigotmc.org/nexus/service/rest/repository/browse/snapshots/org/spigotmc/spigot-api/1.8.8-R0.1-SNAPSHOT/

## Build tooling

`maven-compiler-plugin` is set to `3.16.0` and the project uses the compiler `release` value `8`. This updates the build tooling without changing the Minecraft 1.8.8 runtime compatibility target.

## Maven Wrapper

Apache Maven Wrapper 3.3.4 is the current stable wrapper release. The package documents this version, but does not include a downloaded wrapper binary because this build environment cannot fetch external binaries. Generate the wrapper locally with the Maven Wrapper 3.3.4 tool if you want `./mvnw`/`mvnw.cmd` checked into your own repository.

https://maven.apache.org/tools/wrapper/

## GitHub Actions

The workflows use current first-party action tags checked on 2026-10-02:

- `actions/checkout@v7.0.1`
- `actions/setup-java@v6.0.1`
- `actions/setup-node@v7.0.1`
- `actions/upload-artifact@v7.0.1`
- `actions/attest@v4.2.2`

GitHub Actions moved JavaScript actions away from Node 20; current runners use Node 24 for action execution. The repository's separate toolchain check uses Node 26, which is the current Node.js release line as of this version of the project. Node 24 is the LTS line.

## Why no modern runtime library was added

Adding Adventure, modern Paper APIs, or another current server framework solely to make the plugin look newer would undermine the 1.8.8 compatibility goal. The implementation therefore stays on the stable Bukkit/Spigot API surface and modernizes the *build/release/security tooling* around it instead.
