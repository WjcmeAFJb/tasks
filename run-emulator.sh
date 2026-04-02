#!/usr/bin/env bash
# Convenience wrapper — delegates to the Nix flake app.
# Prefer:  nix run .#run-emulator
# Or inside nix develop:  run-emulator
set -euo pipefail

if command -v run-emulator &>/dev/null; then
  exec run-emulator "$@"
fi

echo "Not inside 'nix develop'. Launching via flake..."
exec nix run .#run-emulator -- "$@"
