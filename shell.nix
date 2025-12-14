let
  flake = builtins.getFlake (toString ./);
  system = builtins.currentSystem;
in
if builtins.hasAttr system flake.devShells then
  flake.devShells.${system}.default
else
  throw "Unsupported system for this devShell. Supported systems: ${builtins.concatStringsSep ", " (builtins.attrNames flake.devShells)}"
