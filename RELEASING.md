# Releasing TamboUI

Releases are automated by the **`Release`** GitHub Actions workflow
(`.github/workflows/release.yml`). You do **not** write a changelog by hand and
you do **not** run Gradle publishing locally — the workflow does everything.

## TL;DR

```bash
# from anywhere with gh + repo access
gh workflow run release.yml \
  -f version=0.5.0 \
  -f next-version=0.6.0-SNAPSHOT
```

Or: GitHub → **Actions → Release → Run workflow**, fill in `version` and
`next-version`.

That's the whole release. Everything below is context and pre-flight.

## Do I need to write a changelog?

**No.** JReleaser generates the GitHub release notes automatically from commit
history using the **conventional-commits** preset (`jreleaser.yml`). Merge
commits, `dependabot`, and all-contributors bookkeeping are filtered out.

The only thing you owe the changelog is **good commit / PR titles**:

- Use conventional-commit prefixes: `feat:`, `fix:`, `docs:`, `test:`,
  `refactor:`, `chore:`, `perf:`, `build:`.
- The PR title becomes the changelog line (squash-merge keeps it clean).
- Anything without a recognized prefix still appears but lands uncategorized.

Optional polish: after the release runs, edit the generated GitHub Release notes
by hand for a human-friendly summary at the top. Not required.

## What the workflow does (in order)

1. **Bump to final** — sets `version=<version>` in `gradle.properties`, commits
   `[release] Releasing version <version>`, pushes to `main`.
2. **Publish to Maven Central** — `./gradlew publishToSonatype
   closeAndReleaseSonatypeStagingRepository` (GPG-signed).
3. **GitHub release** — JReleaser `full-release`: tag + release + auto changelog.
4. **Publish docs** — triggers `documentation.yml`.
5. **Bump to next** — sets `version=<next-version>`, commits
   `[release] Bumping version to <next-version>`, pushes to `main`.

There is **no manual approval gate** once triggered, and it pushes to `main`
twice. Make sure `main` is green and ready before you fire it.

## Pre-release checklist

- [ ] Target milestone is empty (all intended PRs/issues merged or moved).
- [ ] `main` CI is green (`gradle.yml`).
- [ ] Merged PR titles use conventional-commit prefixes (drives the changelog).
- [ ] Decide the two versions:
  - `version` = release, e.g. `0.5.0` (just drop `-SNAPSHOT` from the current
    `gradle.properties`).
  - `next-version` = next dev cycle, e.g. `0.6.0-SNAPSHOT`.

## Required repository secrets

The publish step is skipped if `CENTRAL_USERNAME` is unset, so these must exist:

- `CENTRAL_USERNAME`, `CENTRAL_PASSWORD` — Maven Central (Sonatype) portal creds.
- `GPG_SIGNING_KEY`, `GPG_SIGNING_PASSWORD` — artifact signing.
- `GITHUB_TOKEN` — provided by Actions (release + docs trigger).
- `ZERNIO_TOKEN`, `ZERNIO_PROFILE_ID` — social announce (see below). Optional:
  if unset, the Zernio announcer just fails/skips; the rest of the release is
  unaffected.

## Social announce (Zernio)

Final releases auto-announce via JReleaser's native **`zernio`** announcer
(`announce.zernio` in `jreleaser.yml`), which posts to every account connected
to the configured Zernio profile — one message, all platforms. It's
`active: RELEASE`, so snapshots/prereleases don't announce.

- Set repo secrets `ZERNIO_TOKEN` (API key) and `ZERNIO_PROFILE_ID` (the profile
  whose connected accounts should receive the post).
- The message lives in `jreleaser.yml` (`announce.zernio.message`); for richer
  posts drop a template at `src/jreleaser/templates/zernio.tpl`.
- To include a video/image, upload it via Zernio and reference it — the
  announcer posts text by default.

## After the release

- Verify the artifacts on Maven Central (`dev.tamboui`) — Central publishing can
  take a while to propagate.
- Verify the GitHub Release + tag exist and the changelog looks right.
- Confirm the docs site rebuilt (`documentation.yml` run).
- Confirm `gradle.properties` on `main` is back to `<next-version>`.

## If something goes wrong

- **Publish step skipped** → `CENTRAL_USERNAME` secret missing.
- **Central close/release failed** → check the staging repo in the Sonatype
  portal; the GitHub release may still have been created. Re-running is safest
  from a clean state (the `release.github.overwrite: true` setting lets JReleaser
  recreate the GitHub release).
- **Wrong version pushed** → fix `gradle.properties` on `main` with a follow-up
  commit; drop/re-tag as needed.
