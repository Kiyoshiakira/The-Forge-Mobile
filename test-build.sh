#!/bin/bash
# Try different AGP versions to find one that works
for version in "8.3.0" "8.2.0" "8.1.0" "8.0.0" "7.4.2"; do
  echo "Testing AGP version $version..."
  sed -i "s/agp = \".*\"/agp = \"$version\"/" gradle/libs.versions.toml
  timeout 60 ./gradlew --version > /dev/null 2>&1 && echo "  Gradle OK" || continue
  timeout 120 ./gradlew tasks --no-daemon > /dev/null 2>&1
  if [ $? -eq 0 ]; then
    echo "SUCCESS with AGP $version!"
    exit 0
  fi
done
echo "All versions failed"
exit 1
