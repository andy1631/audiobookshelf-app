## Tasks for the Audiobookshelf mobile app

## List available tasks
default:
  @just --list

## Install Node deps, generate static web assets, and sync into the native shells
setup:
  npm install
  npm run generate
  npx cap sync

## Run Nuxt dev server on port 1337
dev:
  npm run dev

## Live-reload on device/emulator via Ionic Cap run
devlive:
  npm run devlive

## Generate static web app (nuxt generate)
generate:
  npm run generate

## Nuxt production build (SSR-style build)
build:
  npm run build

## Rebuild the web app and sync to native shells
sync:
  npm run sync

## Open Android project in Android Studio
studio:
  npx cap open android

## Create a Play Store AVD named 'abs' (uses JAVA8_HOME for JAXB compatibility)
avd-create:
  JAVA_HOME={{env_var("JAVA8_HOME")}} avdmanager create avd -n abs -k "system-images;android-35;google_apis_playstore;x86_64" -d pixel_7

## Launch the 'abs' emulator with host GPU and no snapshots
avd-start:
  emulator -avd abs -gpu host -no-snapshot-save
