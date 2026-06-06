# Contributing to TamboUI

Thanks for taking the time to contribute! This guide explains how to set up your environment and follow project conventions.

## Prerequisites

- `git` command
- Java 25 is required to build
- Gradle 9+ (use `./gradlew`).

## Note on Java versions

The build require Java 25, we use Java 21+ in demos, except File Manager which is used for verifying the core library can still run on Java 8.

## Setup

With the above you should be able to get setup like this:

```bash
git clone https://github.com/tamboui/tamboui
cd tamboui
./gradlew build
```

### mise (optional, recommended)

If you have [mise](https://mise.jdx.dev) installed, it will automatically set up the correct Java and JBang versions and give you shorthand tasks for common operations:

```bash
mise install        # install Java + JBang
mise tasks          # list available tasks
mise run build      # compile + format, no tests
mise run test       # run all tests
mise run check      # full build + tests + javadoc
mise run format     # apply Spotless formatting
mise run demo <name>  # run a demo
```

Run `mise tasks` for the full list.

## IDE's 

Intellij, Visual Code Studio and Cursor is known to work with TamboUI project.

## Visual Code/Cursor setup

To make the project buildable in VS Code, it is important configure both the Java language server and Gradle to use a JDK that is version 25 or higher.

It should work autoamtically; but in case you get build errors about not using Java 25 follow these instructions:

You need to set both `java.jdt.ls.java.home` and `gradle.java.home` for it to work. 

1. Open **Settings (JSON)** in VS Code.
2. Add or update these settings:

```json
{
  "java.jdt.ls.java.home": "/absolute/path/to/jdk-25",
  "gradle.java.home": "/absolute/path/to/jdk-25"
}
```

## Build and test

Using mise (if installed):

- Build: `mise run build`
- Test: `mise run test`
- Full check: `mise run check`
- Javadoc: `mise run javadoc`
- Format: `mise run format`

Or directly with Gradle:

- Build: `./gradlew -q build`
- Test: `./gradlew -q test`
- Javadoc: `./gradlew -q javadoc`

## Code style and conventions

- Java 8 source compatibility for library modules.
- Use JUnit 5 for tests.
- Add Javadoc for all public APIs.
- [AGENTS.md](./AGENTS.md) for detailed agent guidance (module structure, build commands, code style, testing, and documentation requirements).
- For detailed developer guidance on creating custom widgets, components, and working with the codebase, see the [Developer Guide](./docs/src/docs/asciidoc/developer-guide.adoc).

## Documentation

If you change public APIs or behavior, update the docs under `docs/src/docs/asciidoc/` and run:

- `./gradlew :docs:asciidoctor`

## Submitting changes

- Keep changes focused and minimal.
- Ensure tests pass.
- Include updates to documentation when needed.

## Coding Agents/LLM usage

TamboUI would not be what it is without access to Coding agents, and we do have AGENTS.md to guide suchs agents. 

We welcome tools that help developers become more productive — including Large Language Models (LLMs) and Agents like ChatGPT, GitHub Copilot, and others.

To ensure a healthy and productive community, the following expectations apply:

### Acceptable Use of LLMs

- LLMs may be used to **assist your development** — e.g. drafting code, writing documentation, proposing fixes — as long as **you understand, validate**, and **take responsibility for the results.**
- You should only submit contributions (PRs, comments, discussions, issues) that reflect your **own understanding** and **intent**, not what an Agent/LLM "spit out."
- You may use Agents/LLMs to help you **write better**, but not to **post more**.

### Unacceptable Use

- Submitting code, tests, comments, or issues that appear to be **copied directly from an LLM with little or no human oversight** is **not acceptable**.
- Posting **large volumes of low-effort suggestions, vague issues, or links with no context** — even if technically accurate — is considered spam.
- Submitting **AI-generated tests that do not validate actual behavior** or meaningfully cover functionality is not helpful and will be rejected.
- Using bots, agents, or automated tools to open PRs, file issues, or post content **without human authorship and responsibility** is not allowed.

### If in Doubt

If you're unsure whether your use of Agent/LLMs is acceptable — ask! We're happy to help contributors learn how to use AI tools effectively **without creating noise**.



