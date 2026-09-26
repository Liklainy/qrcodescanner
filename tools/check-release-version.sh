#!/usr/bin/env bash
# Checks that the release tag matches gradle.properties before anything is built:
#   - the tag is v<appVersionName>
#   - appVersionCode is a number, higher than the one at the previous v* tag
#     (AppGallery rejects a re-used versionCode)
# Both release workflows run it, so the GitHub Release and the store upload build
# the same version. Needs the full history and tags (checkout with fetch-depth: 0).
#
# Usage: tools/check-release-version.sh <tag>
set -euo pipefail

tag=${1:?usage: $0 <tag>}

# Reads a key the way java.util.Properties would for this file: spaces around
# '=' or ':' are allowed, CRLF endings are tolerated, and the last entry wins.
prop() {
  awk -v key="$1" '
    { sub(/\r$/, "") }
    /^[ \t]*[#!]/ { next }
    {
      line = $0
      sub(/^[ \t]+/, "", line)
      if (substr(line, 1, length(key)) != key) next
      rest = substr(line, length(key) + 1)
      if (rest !~ /^[ \t]*[=:]/ && rest !~ /^[ \t]+[^ \t]/) next
      sub(/^[ \t]*[=:]?[ \t]*/, "", rest)
      sub(/[ \t]+$/, "", rest)
      value = rest
    }
    END { print value }
  '
}

fail() { echo "::error::$*"; exit 1; }

name=$(prop appVersionName < gradle.properties)
code=$(prop appVersionCode < gradle.properties)

[ -n "$name" ] || fail "appVersionName is missing from gradle.properties."
[ "v$name" = "$tag" ] ||
  fail "Tag $tag does not match appVersionName=$name in gradle.properties. Bump appVersionCode/appVersionName and release_notes.txt, commit, then tag v<appVersionName>."
[[ "$code" =~ ^[0-9]+$ ]] || fail "appVersionCode='$code' in gradle.properties is not a number."

if prev=$(git describe --tags --abbrev=0 --match 'v*' "$tag^" 2>/dev/null); then
  prev_code=$(git show "$prev:gradle.properties" | prop appVersionCode)
  if [[ "$prev_code" =~ ^[0-9]+$ ]] && [ "$code" -le "$prev_code" ]; then
    fail "appVersionCode=$code is not higher than $prev_code at $prev. Bump it in gradle.properties."
  fi
  echo "Version $name ($code); previous release $prev had code $prev_code."
else
  echo "Version $name ($code); no earlier v* tag to compare with."
fi
