#!/bin/sh
cd "$(dirname "$0")" || exit 1
rm -rf build
mkdir -p build/paint/i18n
echo "Compilation..."
javac -encoding UTF-8 --release 8 -d build src/paint/*.java 2>/dev/null \
    || javac -encoding UTF-8 -d build src/paint/*.java || exit 1
cp src/paint/i18n/*.properties build/paint/i18n/
echo "Creation de JPaint.jar..."
jar cfe JPaint.jar paint.Main -C build . || exit 1
rm -rf build
echo "JPaint.jar a ete cree."
