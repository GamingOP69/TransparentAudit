# GitHub Actions overview

## CI

`.github/workflows/ci.yml` builds TransparentAudit on every branch push, every tag push, pull requests, merge-queue events, and manual dispatch.

## Release

`.github/workflows/release.yml` publishes releases from `vMAJOR.MINOR.PATCH` tags and can also be started manually. Manual execution is designed for users who do not have a PC: enter a release tag in GitHub Actions and the workflow creates the tag and release.

## Toolchain

- Minecraft runtime target: Spigot/Bukkit API 1.8.8
- Java bytecode target: Java 8
- Build: Maven
- Repository tooling: Node.js 26
- GitHub action releases: checkout v7.0.1, setup-java v6.0.1, setup-node v7.0.1, upload-artifact v7.0.1, attest v4.2.2

The plugin stays on Java 8 because Minecraft 1.8.8 is the compatibility target. Modern Node.js and GitHub Actions are infrastructure tooling only.
