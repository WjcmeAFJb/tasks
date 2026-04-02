# Tasks — Development Notes

## Nix Dev Environment

Enter the dev shell:

```
nix develop
```

This provides: JDK 17, Gradle, Android SDK (platforms 34/35/36), emulator,
system images, and E2E helper scripts. It also auto-updates `local.properties`
with the correct `sdk.dir`.

### Building

```
./gradlew :app:assembleGenericDebug
```

The `generic` flavor builds without Google Play Services dependencies.

### Emulator

```
create-avd          # one-time: creates AVD "test_device"
run-emulator        # starts emulator and blocks until boot completes
```

Or via flake apps outside the dev shell:

```
nix run .#create-avd
nix run .#run-emulator
```

#### SELinux (critical)

On Fedora/RHEL with SELinux **Enforcing**, the Android emulator segfaults
~25 seconds after startup. The crash is in QEMU's memory-mapped rendering
path and affects both the Nix-packaged and Google-distributed emulator
binaries. **Set SELinux to Permissive before launching the emulator:**

```
sudo setenforce 0
```

This is the single most important finding from the initial setup. Without
this, every emulator launch silently crashes with SIGSEGV.

### E2E Helpers (available inside `nix develop`)

| Command | Usage |
|---|---|
| `e2e-screenshot [file]` | `e2e-screenshot home.png` |
| `e2e-tap X Y` | `e2e-tap 276 600` |
| `e2e-text "words"` | `e2e-text "Buy groceries"` |
| `e2e-dump-ui` | Dumps full UI tree (XML) |

These wrap `adb` and work against any running emulator on port 5554.
