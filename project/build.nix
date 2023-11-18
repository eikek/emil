with import <nixpkgs> { };
let
  initScript = writeScript "emil-build-init" ''
     export LD_LIBRARY_PATH=
     sbt "$@"
  '';
in
buildFHSUserEnv {
  name = "emil-sbt";
  targetPkgs = pkgs: with pkgs; [
    netcat jdk11 wget which zsh dpkg sbt git ncurses mc jekyll
  ];
  runScript = initScript;
}
