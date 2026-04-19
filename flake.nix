{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    devshell-tools.url = "github:eikek/devshell-tools";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
    devshell-tools,
  }:
    flake-utils.lib.eachDefaultSystem (system: let
      pkgs = nixpkgs.legacyPackages.${system};
      devshellPkgs = with pkgs; [
        devshell-tools.packages.${system}.sbt17
        metals
        jekyll
      ];
    in {
      formatter = pkgs.alejandra;
      devShells = {
        default = pkgs.mkShellNoCC {
          buildInputs = devshellPkgs;

        };
      };
    });
}
