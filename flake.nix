{
  description = "Nix flake dev shell for the Audiobookshelf mobile app";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
  }:
    flake-utils.lib.eachSystem ["x86_64-linux"] (system: let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            allowUnfree = true;
            android_sdk.accept_license = true;
          };
        };

      android = pkgs.androidenv.composeAndroidPackages {
        platformToolsVersion = "35.0.2";
        buildToolsVersions = ["35.0.0" "35.0.1"];
        platformVersions = ["35"];
        includeSources = true;
        includeEmulator = true;
        includeSystemImages = true;
        systemImageTypes = ["google_apis_playstore"];
        abiVersions = ["x86_64"];
        useGoogleAPIs = true;
        useGoogleTVAddOns = true;
        includeExtras = ["extras;google;gcm"];
        extraLicenses = [
          "android-sdk-preview-license"
          "android-googletv-license"
          "android-sdk-arm-dbt-license"
          "google-gdk-license"
          "intel-android-extra-license"
          "intel-android-sysimage-license"
          "mips-android-sysimage-license"
        ];
      };

        androidSdkRoot = "${android.androidsdk}/libexec/android-sdk";
        jdk = pkgs.jdk17;
        jdk8 = pkgs.jdk8;
      in {
        devShells.default = pkgs.mkShell {
          packages = [
            pkgs.git
            pkgs.gradle
            jdk
            jdk8
            pkgs.python3
            pkgs.pkg-config
            pkgs.nodejs_20
            pkgs.just
            android.androidsdk
            pkgs.android-studio
          ];

          shellHook = ''
            export ANDROID_HOME=${androidSdkRoot}
            export ANDROID_SDK_ROOT=${androidSdkRoot}
            export JAVA_HOME=${jdk}
            export JAVA8_HOME=${jdk8}
            export CAPACITOR_ANDROID_STUDIO_PATH=${pkgs.android-studio}/bin/android-studio

            if [ -d "${androidSdkRoot}/platform-tools" ]; then
              export PATH="${androidSdkRoot}/platform-tools:$PATH"
            fi
          if [ -d "${androidSdkRoot}/emulator" ]; then
            export PATH="${androidSdkRoot}/emulator:$PATH"
          fi
          if [ -d "${androidSdkRoot}/cmdline-tools/latest/bin" ]; then
            export PATH="${androidSdkRoot}/cmdline-tools/latest/bin:$PATH"
            elif [ -d "${androidSdkRoot}/tools/bin" ]; then
              export PATH="${androidSdkRoot}/tools/bin:$PATH"
            fi

            echo "Dev shell ready. Use 'just --list' to see available tasks. If avdmanager complains about JAXB, run it with JAVA_HOME=\"$JAVA8_HOME\" (or use 'just avd-create')."
          '';
        };
      });
}
