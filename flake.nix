{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };
  outputs =
    {
      self,
      nixpkgs,
      ...
    }@inputs:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs {
        inherit system;
      };
    in
    with pkgs;
    {
      formatter.${system} = pkgs.nixfmt-rfc-style;
      devShell.${system} = pkgs.mkShell {
        packages = [
          jetbrains.idea-community-bin
          jdk21_headless
          maven
        ];
      };
    };
}
