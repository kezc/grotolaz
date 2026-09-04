#!/usr/bin/env bash
# Resolve the latest STABLE version of a Gradle plugin or Maven artifact.
#
# "Stable" = a plain numeric version (e.g. 1.2.3) with no -alpha/-beta/-rc/-SNAPSHOT
# suffix. This matters because a repository's own <release>/<latest> field can point
# at a prerelease (the ComposePWA plugin is a live example: <release> is an -alpha
# while the newest real release is older).
#
# Usage:
#   latest_version.sh --plugin dev.yuyuyuyuyu.composepwa
#   latest_version.sh --artifact org.jetbrains.kotlinx:kotlinx-coroutines-core
#   latest_version.sh --plugin <id> --allow-prerelease    # newest of anything
#
# Prints the resolved version to stdout; diagnostics go to stderr. Exit non-zero
# only if the metadata could not be fetched at all.
set -euo pipefail

usage() {
  echo "Usage: $0 --plugin <plugin.id> | --artifact <group:artifact> [--allow-prerelease]" >&2
  exit 2
}

mode=""; coord=""; allow_pre=0
while [ $# -gt 0 ]; do
  case "$1" in
    --plugin) mode=plugin; coord="${2:-}"; shift 2;;
    --artifact) mode=artifact; coord="${2:-}"; shift 2;;
    --allow-prerelease) allow_pre=1; shift;;
    -h|--help) usage;;
    *) usage;;
  esac
done
[ -n "$mode" ] && [ -n "$coord" ] || usage

if [ "$mode" = plugin ]; then
  # The Plugin Portal publishes a "marker" artifact: group = plugin id,
  # artifact = "<id>.gradle.plugin".
  gpath="${coord//.//}"
  url="https://plugins.gradle.org/m2/${gpath}/${coord}.gradle.plugin/maven-metadata.xml"
else
  group="${coord%%:*}"; artifact="${coord##*:}"
  [ "$group" != "$coord" ] || usage   # must contain a ':'
  gpath="${group//.//}"
  url="https://repo1.maven.org/maven2/${gpath}/${artifact}/maven-metadata.xml"
fi

xml="$(curl -fsSL --max-time 30 "$url")" || {
  echo "ERROR: could not fetch $url" >&2
  echo "       Look the version up manually (Gradle Plugin Portal / Maven Central)." >&2
  exit 1
}

versions="$(printf '%s' "$xml" | grep -oE '<version>[^<]+</version>' | sed -E 's#</?version>##g')"
[ -n "$versions" ] || { echo "ERROR: no <version> entries at $url" >&2; exit 1; }

if [ "$allow_pre" -eq 1 ]; then
  candidates="$versions"
else
  # Keep only plain numeric versions: digits and dots, nothing else.
  candidates="$(printf '%s\n' "$versions" | grep -E '^[0-9]+(\.[0-9]+)*$' || true)"
fi

if [ -z "$candidates" ]; then
  latest_all="$(printf '%s\n' "$versions" | sort -V | tail -1)"
  echo "WARN: no stable release found for $coord; newest overall is $latest_all" >&2
  echo "$latest_all"
  exit 0
fi

latest_stable="$(printf '%s\n' "$candidates" | sort -V | tail -1)"
newest_all="$(printf '%s\n' "$versions" | sort -V | tail -1)"
if [ "$latest_stable" != "$newest_all" ]; then
  echo "note: latest stable is $latest_stable (newer prerelease exists: $newest_all)" >&2
fi
echo "$latest_stable"
