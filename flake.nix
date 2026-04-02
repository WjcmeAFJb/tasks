{
  description = "Tasks - Android task management app dev environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    android-nixpkgs = {
      url = "github:tadfisher/android-nixpkgs";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, android-nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs {
        inherit system;
        config.allowUnfree = true;
      };

      androidSdk = android-nixpkgs.sdk.${system} (sdkPkgs: with sdkPkgs; [
        cmdline-tools-latest
        build-tools-34-0-0
        build-tools-35-0-1
        build-tools-36-0-0
        platform-tools
        platforms-android-34
        platforms-android-35
        platforms-android-36
        emulator
        system-images-android-35-default-x86-64
      ]);

      androidHome = "${androidSdk}/share/android-sdk";

      # --- helper scripts exposed as flake apps ---

      create-avd = pkgs.writeShellScriptBin "create-avd" ''
        set -euo pipefail
        export ANDROID_HOME="${androidHome}"
        export ANDROID_SDK_ROOT="$ANDROID_HOME"
        export ANDROID_AVD_HOME="''${ANDROID_AVD_HOME:-$HOME/.android/avd}"

        AVD_NAME="''${1:-test_device}"
        DEVICE="''${2:-pixel_6}"
        IMAGE="system-images;android-35;default;x86_64"

        echo "Creating AVD '$AVD_NAME' (device=$DEVICE, image=$IMAGE)..."
        echo "no" | "$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" \
          create avd -n "$AVD_NAME" -k "$IMAGE" -d "$DEVICE" --force

        # Enable hardware keyboard
        sed -i 's/hw.keyboard=no/hw.keyboard=yes/' \
          "$ANDROID_AVD_HOME/$AVD_NAME.avd/config.ini"

        echo "AVD '$AVD_NAME' ready."
      '';

      run-emulator = pkgs.writeShellScriptBin "run-emulator" ''
        set -euo pipefail
        export ANDROID_HOME="${androidHome}"
        export ANDROID_SDK_ROOT="$ANDROID_HOME"
        export ANDROID_AVD_HOME="''${ANDROID_AVD_HOME:-$HOME/.android/avd}"
        export ANDROID_EMULATOR_HOME="''${ANDROID_EMULATOR_HOME:-$HOME/.android}"
        export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"

        AVD_NAME="''${1:-test_device}"
        PORT="''${2:-5554}"

        # --- preflight: SELinux ------------------------------------------------
        if command -v getenforce &>/dev/null && [ "$(getenforce 2>/dev/null)" = "Enforcing" ]; then
          echo "WARNING: SELinux is Enforcing. The Android emulator will segfault."
          echo "Run:  sudo setenforce 0"
          exit 1
        fi

        # --- preflight: AVD exists? --------------------------------------------
        if [ ! -d "$ANDROID_AVD_HOME/$AVD_NAME.avd" ]; then
          echo "AVD '$AVD_NAME' not found. Run:  nix run .#create-avd"
          exit 1
        fi

        echo "Starting emulator (avd=$AVD_NAME, port=$PORT)..."
        emulator -avd "$AVD_NAME" \
          -no-window -no-audio -gpu swiftshader_indirect \
          -no-boot-anim -port "$PORT" -no-metrics -no-snapshot &
        EMU_PID=$!
        echo "Emulator PID: $EMU_PID"

        echo "Waiting for device..."
        adb wait-for-device

        echo "Waiting for boot to complete..."
        for i in $(seq 1 120); do
          BOOT=$(adb -s "emulator-$PORT" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r\n')
          if [ "$BOOT" = "1" ]; then
            echo "Boot completed (~''${i}s)."
            break
          fi
          if ! kill -0 "$EMU_PID" 2>/dev/null; then
            echo "ERROR: Emulator process died." >&2
            exit 1
          fi
          sleep 1
        done

        adb devices
        echo "Emulator ready."
      '';

      e2e-screenshot = pkgs.writeShellScriptBin "e2e-screenshot" ''
        set -euo pipefail
        export PATH="${androidSdk}/share/android-sdk/platform-tools:$PATH"
        OUT="''${1:-screenshot.png}"
        adb exec-out screencap -p > "$OUT"
        echo "Saved $OUT ($(wc -c < "$OUT") bytes)"
      '';

      e2e-tap = pkgs.writeShellScriptBin "e2e-tap" ''
        set -euo pipefail
        export PATH="${androidSdk}/share/android-sdk/platform-tools:$PATH"
        adb shell input tap "$1" "$2"
      '';

      e2e-text = pkgs.writeShellScriptBin "e2e-text" ''
        set -euo pipefail
        export PATH="${androidSdk}/share/android-sdk/platform-tools:$PATH"
        # Replace spaces with %s for adb input text
        ESCAPED=$(echo "$1" | sed 's/ /%s/g')
        adb shell input text "$ESCAPED"
      '';

      e2e-dump-ui = pkgs.writeShellScriptBin "e2e-dump-ui" ''
        set -euo pipefail
        export PATH="${androidSdk}/share/android-sdk/platform-tools:$PATH"
        adb shell uiautomator dump /sdcard/ui.xml 2>/dev/null
        adb shell cat /sdcard/ui.xml
      '';

    in
    {
      devShells.${system}.default = pkgs.mkShell {
        buildInputs = [
          androidSdk
          pkgs.jdk17
          pkgs.gradle
          pkgs.scrcpy
          # E2E helper scripts available inside nix develop
          create-avd
          run-emulator
          e2e-screenshot
          e2e-tap
          e2e-text
          e2e-dump-ui
        ];

        ANDROID_HOME = androidHome;
        ANDROID_SDK_ROOT = androidHome;
        JAVA_HOME = "${pkgs.jdk17}";
        GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${androidHome}/build-tools/36.0.0/aapt2";

        shellHook = ''
          export PATH="${androidHome}/emulator:${androidHome}/platform-tools:$PATH"

          # Keep local.properties in sync with this Nix environment
          if [ -f local.properties ]; then
            CURRENT=$(grep -oP 'sdk.dir=\K.*' local.properties 2>/dev/null || true)
            if [ "$CURRENT" != "${androidHome}" ]; then
              sed -i "s|sdk.dir=.*|sdk.dir=${androidHome}|" local.properties
              echo "Updated local.properties sdk.dir"
            fi
          else
            echo "sdk.dir=${androidHome}" > local.properties
            echo "Created local.properties"
          fi

          echo "Tasks dev environment ready"
          echo "  ANDROID_HOME=$ANDROID_HOME"
          echo "  JAVA_HOME=$JAVA_HOME"
          echo ""
          echo "Quick start:"
          echo "  create-avd                  # create an emulator AVD"
          echo "  run-emulator                # launch emulator and wait for boot"
          echo "  ./gradlew :app:assembleGenericDebug  # build the app"
          echo "  adb install app/build/outputs/apk/generic/debug/app-generic-debug.apk"
          echo ""
          echo "E2E helpers:  e2e-screenshot  e2e-tap  e2e-text  e2e-dump-ui"
        '';
      };

      # Runnable scripts:  nix run .#create-avd  /  nix run .#run-emulator
      apps.${system} = {
        create-avd  = { type = "app"; program = "${create-avd}/bin/create-avd"; };
        run-emulator = { type = "app"; program = "${run-emulator}/bin/run-emulator"; };
      };
    };
}
