# Blacksmith

Blacksmith is the Java agent counterpart of ModernFix and VintageFix - it applies fixes & improvements
to the earliest stages of modloading.

## How to use

1. Clone this repository.
2. `./gradlew build`
3. Add `-javaagent:<path to repo>/build/libs/blacksmith-1.0-SNAPSHOT.jar` to your JVM arguments in a Forge 1.12+ instance.
