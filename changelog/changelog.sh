#!/usr/bin/env bash
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

trap 'exit_code=$?; [ $exit_code -ne 0 ] && echo "Error on line ${LINENO} of ${0}"; exit $exit_code' EXIT;
set -o errexit -o nounset -o pipefail;

ADDED=added
CHANGED=changed
DEPRECATED=deprecated
REMOVED=removed
FIXED=fixed
NEW=new
GEN=gen
RELEASE=release
UNRELEASED=unreleased
RELEASE_DATE=RELEASE-DATE


usage() {
    printf "Usage: $0 COMMAND COMMAND_OPTIONS\n\n"
    printf "    Commands:\n\n"
    printf "        $NEW OPTION NAME     create and edit a new changelog entry file\n\n"
    printf "            example: $0 $NEW $ADDED foo-bar.md\n\n"
    printf "            OPTION:\n"
    printf "              $ADDED         for new features\n"
    printf "              $CHANGED       for changes in existing functionality\n"
    printf "              $DEPRECATED    for soon-to-be-removed features\n"
    printf "              $REMOVED       for now removed features\n"
    printf "              $FIXED         for any bug fixes\n\n"
    printf "            NAME            short name for your new changelog entry file (use .md suffix)\n\n"
    printf "        $GEN                 generate the changelog, print to STDOUT\n\n"
    printf "        $RELEASE VERSION     rename the 'unreleased' dir to the given VERSION (e.g. 1.2.3)\n\n"
    exit 1
}

cmd=${1:-}

case "$cmd" in
    $NEW)
        option=${2:-}
        case "$option" in
            $ADDED)
                ;;
            $CHANGED)
                ;;
            $DEPRECATED)
                ;;
            $REMOVED)
                ;;
            $FIXED)
                ;;
            *)
                echo "invalid option: '$option'"
                usage
                ;;
        esac
        name=${3:-}
        if [[ ! "$name" =~ ^[a-zA-Z0-9\.\-_]+\.md$ ]]; then
            echo "name must be alphanumeric (including periods, hyphens, and underscores) and use .md suffix"
            exit 1
        fi
        fname="$UNRELEASED/$option/$name"
        if [[ -e "$fname" ]]; then
            echo "the chosen name '$name' already exists at $fname"
            exit 1
        fi
        mkdir -p "$UNRELEASED/$option"
        touch $fname
        $EDITOR "$fname"
        ;;
    $GEN)
        cat << EOF
# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/).

EOF

        for d in $(ls | sort -r); do
            if [[ ! -d "$d" ]]; then
                continue
            fi
            if [[ "$d" = "$UNRELEASED" ]]; then
                echo "## [$UNRELEASED]"
            elif [[ "$d" =~ ^[0-9]+\.[0-9]+\.[0-9]+-[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
                rel_ver=$(cut -d'-' -f1 <<< "$d")
                rel_date=$(cut -d'-' -f2-4 <<< "$d")
                echo "## [$rel_ver] - $rel_date"
            fi
            for section in $(ls $d); do
                cap="$(tr '[:lower:]' '[:upper:]' <<< ${section:0:1})${section:1}"
                echo "### $cap"
                cat $d/$section/*.md
                echo
            done
        done
        cat << EOF
[unreleased]: https://github.com/apache/trafficcontrol/compare/RELEASE-5.0.0...HEAD
[5.0.0]: https://github.com/apache/trafficcontrol/compare/RELEASE-4.1.0...RELEASE-5.0.0
[4.1.0]: https://github.com/apache/trafficcontrol/compare/RELEASE-4.0.0...RELEASE-4.1.0
[4.0.0]: https://github.com/apache/trafficcontrol/compare/RELEASE-3.0.0...RELEASE-4.0.0
[3.0.0]: https://github.com/apache/trafficcontrol/compare/RELEASE-2.2.0...RELEASE-3.0.0
[2.2.0]: https://github.com/apache/trafficcontrol/compare/RELEASE-2.1.0...RELEASE-2.2.0
EOF
        ;;
    $RELEASE)
        ver=${2:-}
        if [[ ! "$ver" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
            echo "version '$ver' is not formatted correctly (e.g. '1.2.3')"
            exit 1
        fi
        if [[ ! -d "$UNRELEASED" ]]; then
            echo "no $UNRELEASED dir found"
            exit 1
        fi
        target="$ver-$(date +%Y-%m-%d)"
        if [[ -d "$target" ]]; then
            echo "release dir '$target' already exists"
            exit 1
        fi
        mv $UNRELEASED $target
        ;;
    *)
        echo "invalid command: '$cmd'"
        usage
esac

