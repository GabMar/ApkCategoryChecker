# Run `just` to list the recipes.

image := "apk-category-checker"
jar := "build/libs/ApkCategoryChecker.jar"

[private]
default:
    @just --list

# Compile, check the code style and run the tests
build:
    ./gradlew build

# Run the tests
test:
    ./gradlew test

# Check the code style (also part of `build`)
lint:
    ./gradlew ktlintCheck

# Fix the code style where possible
format:
    ./gradlew ktlintFormat

# Build the runnable jar
jar:
    ./gradlew shadowJar

# Analyze APKs with the local build, e.g. `just run ~/apks -f json`
run *args: jar
    java -jar {{jar}} {{args}}

# Build the Docker image
docker-build:
    docker build -t {{image}} .

# Analyze APKs in the current directory with the Docker image, e.g. `just docker-run app.apk`
docker-run *args:
    docker run --rm -v "$PWD:/data" --user "$(id -u):$(id -g)" {{image}} {{args}}

# Delete the build outputs
clean:
    ./gradlew clean

# Tag and push a release (e.g. `just release 2.1.0`); GitHub Actions builds and publishes it
release version:
    #!/usr/bin/env bash
    set -euo pipefail
    [[ "{{version}}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo "Version must look like 1.2.3"; exit 1; }
    [[ -z "$(git status --porcelain)" ]] || { echo "Commit your changes first"; exit 1; }
    [[ "$(git branch --show-current)" == "master" ]] || { echo "Release from master"; exit 1; }
    git fetch --quiet origin
    [[ "$(git rev-parse HEAD)" == "$(git rev-parse origin/master)" ]] || { echo "Push master first"; exit 1; }
    ./gradlew build
    git tag "v{{version}}"
    git push origin "v{{version}}"
