# syntax=docker/dockerfile:1

# The jar is platform independent: build it once on the build machine's platform.
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build
WORKDIR /src

# Download Gradle and the dependencies in a layer of their own.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon --quiet dependencies > /dev/null

COPY src/main src/main
ARG VERSION=2.0.0-SNAPSHOT
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon --quiet shadowJar -PreleaseVersion="$VERSION"


FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="ApkCategoryChecker" \
      org.opencontainers.image.description="Detects the framework used to build Android APKs" \
      org.opencontainers.image.source="https://github.com/GabMar/ApkCategoryChecker" \
      org.opencontainers.image.licenses="MIT"

COPY --from=build /src/build/libs/ApkCategoryChecker.jar /opt/apk-category-checker/ApkCategoryChecker.jar

# Mount the APKs here: docker run --rm -v "$PWD:/data" IMAGE app.apk
WORKDIR /data
USER 65534:65534
ENTRYPOINT ["java", "-jar", "/opt/apk-category-checker/ApkCategoryChecker.jar"]
CMD ["--help"]
