# TamboUI documentation

## Building the docs

From the project root:

```bash
./gradlew :docs:asciidoctor
```

Output goes to `docs/build/docs/`.

## Previewing the built docs

Serve the current build (single version, no version dropdown unless `versions.json` is present):

```bash
./gradlew :docs:asciidoctor
jbang httpd@jbangdev -d build/docs
```

Then open e.g. <http://127.0.0.1:8000/> (or the port jbang prints).

## Previewing the full site (as on gh-pages)

Build and copy into the git-publish clone (same layout as after publish), then serve that directory:

```bash
./gradlew :docs:gitPublishCopy
jbang httpd@jbangdev -d build/gitPublish
```

Then open e.g. <http://127.0.0.1:8000/docs/main/> to see the versioned docs with the version selector. Requires network (and SSH for the repo) so the plugin can clone/fetch.

## Publishing to gh-pages

```bash
./gradlew :docs:gitPublishPush
```

Publishes to the `gh-pages` branch of the configured docs repo.
