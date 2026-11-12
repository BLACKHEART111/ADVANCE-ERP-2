# User Interaction & Communication Preferences

## Language Requirement
- Whenever the user provides a prompt in English (or any language), always provide the explanations, descriptions, progress updates, and summaries in Bengali (বাংলা).

## Multi-Task Workflow & Step-by-Step Progress Tracking
- When executing multiple tasks or steps:
  1. Clearly announce in Bengali (বাংলা) which task is starting before beginning execution.
  2. When the task is completed, notify in Bengali that it is finished and briefly describe the outcome.
  3. Announce what task is starting next.
  4. Continue this step-by-step reporting pattern so the user can easily track how the work is progressing.

## Workspace & Android Build Guidelines (PocketDev)
- Active project workspace: `/workspace/clever-lovelace`. Create, edit, read, run, and build project files strictly inside this directory. Do not create project output under `~/.gemini/antigravity-cli/scratch` or any other scratch directory.
- For Android builds:
  - Never use plain `gradle build` or start a Gradle daemon.
  - Build only the debug APK. First check whether `app/build/outputs/apk/debug/` already contains an APK.
  - First build (no existing debug APK):
    `timeout 15m gradle -Dorg.gradle.jvmargs= --no-daemon --max-workers=2 --init-script /root/.gradle/init.d/pocketdev-android.gradle -Pandroid.aapt2FromMavenOverride=/root/android-sdk/build-tools/35.0.0/aapt2 assembleDebug --console=plain --stacktrace`
  - Cached build (with existing debug APK):
    `timeout 5m gradle -Dorg.gradle.jvmargs= --no-daemon --max-workers=2 --offline --init-script /root/.gradle/init.d/pocketdev-android.gradle -Pandroid.aapt2FromMavenOverride=/root/android-sdk/build-tools/35.0.0/aapt2 assembleDebug --console=plain --stacktrace`
  - If cached build finishes with clear missing-cached-dependency error, retry once online with `timeout 15m` (removing only `--offline`).
  - Do not use `nohup`, backgrounding, or detached processes. If moved to a managed task, poll and tail log every ~10s so the user receives progress. Never wait silently on a long foreground command.
