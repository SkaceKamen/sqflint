[Setup]
AppName=SQFLint
AppPublisher=Kamen
AppPublisherURL=http://sqflint.zipek.cz
AppVersion=0.7.0
DefaultDirName={pf}\SQFLint
UninstallDisplayIcon={app}\sqflint.exe
OutputDir=dist\packages
OutputBaseFilename=sqflint-install
ChangesEnvironment=yes
SetupIconFile=dist-src\sqflint.ico

[CustomMessages]
AppAddPath=Add application directory to your environmental path

[Tasks]
Name: modifypath; Description:{cm:AppAddPath};

[Files]
Source: "dist\*"; DestDir: "{app}"; Flags: recursesubdirs

[Registry]
Root: HKLM; Subkey: "SYSTEM\CurrentControlSet\Control\Session Manager\Environment"; \
    ValueType: expandsz; ValueName: "Path"; ValueData: "{olddata};{app}"; \
    Check: NeedsAddPath('{app}'); Tasks: modifypath

[Code]

function NeedsAddPath(Param: string): boolean;
var
  OrigPath: string;
begin
  if not RegQueryStringValue(HKEY_LOCAL_MACHINE,
    'SYSTEM\CurrentControlSet\Control\Session Manager\Environment',
    'Path', OrigPath)
  then begin
    Result := True;
    exit;
  end;
  // look for the path with leading and trailing semicolon
  // Pos() returns 0 if not found
  Result := Pos(';' + Param + ';', ';' + OrigPath + ';') = 0;
end;