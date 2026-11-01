/**
 * semantic-release configuration.
 *
 * Versions follow Conventional Commits: any breaking change is a major, feat a minor, and fix,
 * perf, revert, refactor and build(deps) a patch. Commits that leave the shipped mod untouched
 * (docs, tests, CI, style, chores) do not release: every release is an update players download.
 */
const releaseRules = [
  // Must come first: commit-analyzer only falls back to its default rules when no custom rule
  // matches, so without this a "fix!:" or "refactor!:" commit would be released as a patch.
  { breaking: true, release: "major" },
  { type: "feat", release: "minor" },
  { type: "fix", release: "patch" },
  { type: "perf", release: "patch" },
  { type: "revert", release: "patch" },
  { type: "refactor", release: "patch" },
  // Other build changes match no rule and do not release. An explicit "release: false" here
  // would win over the deps rule above, since a matching false rule blocks the release.
  { type: "build", scope: "deps", release: "patch" },
  { type: "docs", release: false },
  { type: "style", release: false },
  { type: "test", release: false },
  { type: "ci", release: false },
  { type: "chore", release: false },
];

module.exports = {
  branches: ["master"],
  plugins: [
    [
      "@semantic-release/commit-analyzer",
      {
        preset: "conventionalcommits",
        releaseRules,
      },
    ],
    [
      "@semantic-release/release-notes-generator",
      {
        preset: "conventionalcommits",
        presetConfig: {
          // Breaking changes get their own section automatically.
          types: [
            { type: "feat", section: "Features" },
            { type: "fix", section: "Bug Fixes" },
            { type: "perf", section: "Performance Improvements" },
            { type: "revert", section: "Reverts" },
            { type: "refactor", section: "Code Refactoring" },
            { type: "build", section: "Build System" },
            { type: "docs", hidden: true },
            { type: "style", hidden: true },
            { type: "test", hidden: true },
            { type: "ci", hidden: true },
            { type: "chore", hidden: true },
          ],
        },
      },
    ],
    [
      "@semantic-release/changelog",
      {
        changelogTitle:
          "# Bee Converter\n\nAll notable changes to this project will be documented in this file. See\n[Conventional Commits](https://conventionalcommits.org) for commit guidelines.",
      },
    ],
    [
      "@semantic-release/exec",
      {
        // Stamp the version into the mod and build the jar attached to the GitHub release.
        prepareCmd:
          "sed -i 's/^mod_version=.*/mod_version=${nextRelease.version}/' gradle.properties && ./gradlew build",
      },
    ],
    [
      "@semantic-release/github",
      {
        assets: [
          {
            path: "build/libs/*.jar",
            label: "Bee Converter ${nextRelease.version} (NeoForge 1.21.1)",
          },
        ],
      },
    ],
    [
      "@semantic-release/git",
      {
        message: "chore(release): ${nextRelease.version} [skip ci]\n\n${nextRelease.notes}",
        assets: ["CHANGELOG.md", "gradle.properties"],
      },
    ],
  ],
};
