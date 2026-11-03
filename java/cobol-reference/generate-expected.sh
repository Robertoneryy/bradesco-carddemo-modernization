#!/usr/bin/env bash
# Runs the original COBOL program (app/cbl/CBACT04C.cbl) with GnuCOBOL for every test
# scenario and stores its output as the expected result for the Java tests.
#
# Requires GnuCOBOL 3.x with indexed file support (Ubuntu: apt-get install gnucobol3).
# Usage: java/cobol-reference/generate-expected.sh
set -euo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
JAVA_DIR=$(dirname "$HERE")
REPO_DIR=$(dirname "$JAVA_DIR")
SAMPLE_DIR="$REPO_DIR/app/data/ASCII"
SCENARIOS_DIR="$JAVA_DIR/src/test/resources/scenarios"
# The Java tests run with a fixed clock that produces this timestamp.
FIXED_TS="0001-01-01-00.00.00.000000"

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

cobc -x -fsign=EBCDIC -I "$REPO_DIR/app/cpy" -o "$WORK/cbref" \
  "$HERE/CBREFDRV.cbl" "$REPO_DIR/app/cbl/CBACT04C.cbl" "$HERE/CEE3ABD.cbl"

for dir in "$SCENARIOS_DIR"/*/; do
  name=$(basename "$dir")
  run="$WORK/$name"
  mkdir -p "$run"
  for f in tcatbal cardxref acctdata discgrp; do
    src="$dir/input/$f.txt"
    [ -f "$src" ] || src="$SAMPLE_DIR/$f.txt"
    tr -d '\r' < "$src" > "$run/$f.txt"
  done
  parm=$(tr -d '\r\n' < "$dir/parm.txt")

  rc=0
  (
    cd "$run"
    export DD_INTCATB=tcatbal.txt DD_INXREF=cardxref.txt DD_INACCT=acctdata.txt \
      DD_INDISC=discgrp.txt DD_OUTACCT=acct.out DD_TCATBALF=tcatbal.idx \
      DD_XREFFILE=xref.idx DD_ACCTFILE=acct.idx DD_DISCGRP=discgrp.idx \
      DD_TRANSACT=systran.dat
    "$WORK/cbref" "$parm" > sysout.raw 2>&1
  ) || rc=$?

  expected="$dir/expected"
  rm -rf "$expected"
  mkdir -p "$expected"
  grep -v -e '^CEE3ABD' -e '^libcob: ' "$run/sysout.raw" | sed 's/ *$//' > "$expected/sysout.txt"
  if grep -q '^CEE3ABD' "$run/sysout.raw"; then
    echo "ABENDED" > "$expected/result.txt"
  elif [ "$rc" -ne 0 ]; then
    echo "Scenario $name failed with exit code $rc" >&2
    cat "$run/sysout.raw" >&2
    exit 1
  else
    echo "COMPLETED" > "$expected/result.txt"
    # TRANSACT is a fixed 350-byte record file without line breaks.
    { fold -w350 "$run/systran.dat"; [ -s "$run/systran.dat" ] && echo; } \
      | grep -v '^$' \
      | awk -v ts="$FIXED_TS" '{ print substr($0, 1, 278) ts ts substr($0, 331) }' \
      | sed 's/ *$//' > "$expected/systran.txt"
    sed 's/ *$//' "$run/acct.out" > "$expected/acctdata.txt"
  fi
  echo "$name: $(cat "$expected/result.txt")"
done
