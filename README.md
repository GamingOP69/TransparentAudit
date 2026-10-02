# TransparentAudit 1.2.0 — Minecraft 1.8.8

A server-wide transparency/audit plugin for Spigot/Paper/Bukkit-compatible Minecraft 1.8.8 servers.

## What this version adds

- Exact owner lock: `NOTGAMINGOP` (case-sensitive).
- Player command attempts broadcast to online players.
- Console/server command dispatches broadcast to online players.
- Command-block sender detection where exposed by Bukkit.
- Sensitive command detection for commands such as `give`, `i`, `effect`, `gamemode`, `gm`, `fly`, `op`, `deop`, `tp`, `setblock`, `fill`, `summon`, `reload`, `stop`, and more.
- Sensitive commands produce a title, subtitle, loud high-pitch sound, and red chat alert.
- Namespaced command detection (`minecraft:give`, `essentials:fly`, etc.).
- Persistent `plugins/TransparentAudit/audit.log`.
- Session counters and recent in-memory command history.
- Join/quit audit entries.
- `/ta status`, `/ta stats`, `/ta recent [1-25]`, `/ta list`, `/ta reload`, `/ta verify`, and `/ta help` for the exact owner.
- No owner command exists to disable the audit, clear the audit log, or bypass sensitive alerts.
- GitHub Actions CI that runs on **every branch push, every tag push, pull requests, merge-queue runs, and manual dispatch**.
- GitHub Actions uses current verified action releases: checkout v7.0.1, setup-java v6.0.1, setup-node v7.0.1, upload-artifact v7.0.1, and attest v4.2.2.
- Node.js 26 is used for repository tooling checks; it is not a runtime dependency of the Minecraft plugin.
- Dependabot updates Maven and GitHub Actions dependencies weekly.
- SHA-256 release checksums and GitHub artifact attestations.
- Releases can be created from the GitHub web/mobile Actions UI without a local PC or locally pushed tag.

## Installation

1. Put `TransparentAudit-1.2.0.jar` into the server's `plugins/` directory.
2. Restart the server.
3. Edit `plugins/TransparentAudit/config.yml` to customize the alert and sensitive-command list.
4. Run `/ta reload` as `NOTGAMINGOP` after configuration changes.

## Build locally

Maven is the primary build path:

```bash
mvn clean package
```

The build targets Java 8 and uses `spigot-api:1.8.8-R0.1-SNAPSHOT` as a provided dependency.

The included `build.bat` and `build.sh` remain available for direct compilation against the exact server JAR you run.

## GitHub CI — works without a PC

You do **not** need to push a tag from a local computer.

The CI workflow is configured for:

- every commit pushed to every branch;
- every tag push;
- pull requests;
- GitHub merge-queue (`merge_group`) runs;
- manual `workflow_dispatch` runs from the GitHub web/mobile UI.

For normal code changes, simply commit or edit files on GitHub. GitHub Actions will build the plugin automatically.

The workflow intentionally keeps Java 8 for the 1.8.8 plugin, while using current GitHub action releases and current Node.js tooling. The modern tooling must not be confused with the old Minecraft server API: upgrading the plugin runtime API itself would break the 1.8.8 target.

## Create a release without a PC

From GitHub:

1. Open the repository's **Actions** tab.
2. Select **Release**.
3. Choose **Run workflow**.
4. Enter a semantic tag such as `v1.2.1`.
5. Choose whether it is a pre-release.
6. Run the workflow.

The workflow validates the tag, creates and pushes it using `GITHUB_TOKEN`, builds the Java 8 plugin, creates the source ZIP, creates SHA-256 checksums, creates build attestations, and publishes the GitHub Release.

You can also create a tag manually in GitHub and let the tag-triggered release path run.

See `GITHUB_TAGGING.md` and `MOBILE_GITHUB.md` for details.

## GitHub release tags

Releases use semantic version tags in the form `vMAJOR.MINOR.PATCH`.

Example from a local Git client:

```bash
git tag -a v1.2.1 -m "TransparentAudit v1.2.1"
git push origin v1.2.1
```

But this local command is optional. The GitHub Actions **Release** workflow can create the tag for you from the browser/mobile UI.

## Important command-audit limitation

Bukkit/Spigot 1.8.8 does not expose a universal event for every internal operation performed by every plugin. This plugin audits the command/event surfaces that Bukkit exposes. A plugin that changes player state internally without dispatching a command may not appear as a command audit entry.

## Privacy/security warning

This plugin intentionally broadcasts command text. Do not use commands containing passwords, API keys, database credentials, or other secrets on the server.
