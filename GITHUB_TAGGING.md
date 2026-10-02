# GitHub tags, CI, and PC-free releases

## CI runs on every code push

`.github/workflows/ci.yml` is intentionally broad:

- `push` to **every branch**;
- `push` of **every tag**;
- every normal pull request event that GitHub sends to the workflow;
- `merge_group` for GitHub merge queues;
- `workflow_dispatch` for manual runs.

That means a normal commit to `main`, `dev`, `feature/...`, or any other branch automatically builds the plugin. A tag push also gets a normal CI build in addition to the release workflow.

## Normal tag release

A semantic version tag should look like:

```text
v1.2.1
v1.3.0
v2.0.0
```

From any Git client:

```bash
git add .
git commit -m "release: v1.2.1"
git tag -a v1.2.1 -m "TransparentAudit v1.2.1"
git push origin main
git push origin v1.2.1
```

Pushing `v1.2.1` starts `.github/workflows/release.yml`.

## PC-free release from GitHub web/mobile

A local Git installation is **not required**.

1. Open the repository on GitHub.
2. Open **Actions**.
3. Open the **Release** workflow.
4. Tap/click **Run workflow**.
5. Enter the next version, for example `v1.2.1`.
6. Select pre-release status if needed.
7. Run the workflow.

The workflow then:

1. checks that the tag matches `vMAJOR.MINOR.PATCH`;
2. checks that the tag does not already exist;
3. creates the annotated tag using GitHub Actions;
4. pushes the tag;
5. checks out the tag;
6. builds the Java 8 / Spigot 1.8.8 plugin;
7. creates the plugin JAR, source ZIP, and `SHA256SUMS.txt`;
8. creates GitHub build attestations;
9. publishes the GitHub Release.

Because the tag is created by the release workflow itself, you can do the entire release from a phone/browser.

## Version consistency

For a true plugin version bump, update the version in:

- `pom.xml`;
- `src/main/resources/plugin.yml`;
- any fixed artifact-name checks in `.github/workflows/ci.yml`;
- `PLUGIN_VERSION` in `.github/workflows/release.yml`;
- user-facing version text in `README.md`.

Then release the matching tag.

The current project remains intentionally Java 8-compatible because the runtime target is Minecraft 1.8.8. Current GitHub Actions and Node.js tooling are only for build/release infrastructure.
