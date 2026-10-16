<# : batch launcher - the PowerShell script below does the work
@echo off
setlocal
set "CHR_ROOT=%~dp0"
set "CHR_ARGS=%*"
powershell -NoProfile -ExecutionPolicy Bypass -Command "iex ([IO.File]::ReadAllText('%~f0'))"
set "CHR_EXIT=%ERRORLEVEL%"
if not defined CHR_NO_PAUSE pause
exit /b %CHR_EXIT%
: end of batch launcher #>

# ------------------------------------------------------------------------------------------
# modrinth.bat - builds every supported Minecraft version and uploads the jars to Modrinth.
#
#   modrinth.bat                 build, show a summary, ask, then upload
#   modrinth.bat --dry-run       build and show what would be uploaded, upload nothing
#   modrinth.bat --skip-build    use the jars already in build\libs
#   modrinth.bat --type=beta     upload as beta (release, beta or alpha; default release)
#   modrinth.bat --yes           do not ask before uploading
#
# The Minecraft versions are the mcVersions list in settings.gradle. A version can cover more
# game versions with modrinth.game.versions in versions\<mc>\gradle.properties.
# The changelog is the "## Chronicle <mod_version>" section of CHANGELOG.md. The Modrinth
# token comes from the MODRINTH_TOKEN environment variable, a modrinth_token entry in
# %USERPROFILE%\.gradle\gradle.properties, or is asked for. It needs the "Create versions"
# scope. Versions that already exist on Modrinth are skipped, so a failed run can be repeated.
# Set CHR_NO_PAUSE=1 to skip the final "press any key".
# ------------------------------------------------------------------------------------------

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Add-Type -AssemblyName System.Net.Http

$api = 'https://api.modrinth.com/v2'
$userAgent = 'itzSeis/Chronicle (modrinth.bat)'
$dependencies = [ordered]@{
    'fabric-api' = 'required'
}

function Fail([string] $message) {
    Write-Host ''
    Write-Host "ERROR: $message" -ForegroundColor Red
    exit 1
}

function Read-Props([string] $path) {
    $props = @{}
    foreach ($line in [IO.File]::ReadAllLines($path)) {
        $t = $line.Trim()
        if ($t -eq '' -or $t.StartsWith('#')) { continue }
        $i = $t.IndexOf('=')
        if ($i -gt 0) { $props[$t.Substring(0, $i).Trim()] = $t.Substring($i + 1).Trim() }
    }
    return $props
}

$token = $null

# env var, then %USERPROFILE%\.gradle\gradle.properties, and only with -Ask a prompt
function Get-Token([switch] $Ask) {
    $t = $env:MODRINTH_TOKEN
    if (-not $t) {
        $gradleProps = Join-Path $env:USERPROFILE '.gradle\gradle.properties'
        if (Test-Path $gradleProps) { $t = (Read-Props $gradleProps)['modrinth_token'] }
    }
    if (-not $t -and $Ask) {
        $secure = Read-Host 'Modrinth token (needs the "Create versions" scope)' -AsSecureString
        $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        try { $t = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
    }
    return $t
}

# with a token, so draft projects (not approved yet) are visible too
function Api-Get([string] $path) {
    $headers = @{}
    if ($script:token) { $headers['Authorization'] = $script:token }
    return Invoke-RestMethod -Uri "$api$path" -UserAgent $userAgent -Headers $headers
}

function Is-NotFound($err) {
    $r = $err.Exception.Response
    return $r -and [int] $r.StatusCode -eq 404
}

function Java-Major([string] $javaHome) {
    if (-not $javaHome) { return 0 }
    $release = Join-Path $javaHome 'release'
    if (-not (Test-Path $release)) { return 0 }
    $m = [regex]::Match([IO.File]::ReadAllText($release), 'JAVA_VERSION="(\d+)')
    if ($m.Success) { return [int] $m.Groups[1].Value }
    return 0
}

# ---- options -----------------------------------------------------------------------------

$skipBuild = $false
$dryRun = $false
$assumeYes = $false
$versionType = 'release'
$flags = @()
if ($env:CHR_ARGS) { $flags = $env:CHR_ARGS.Split(' ', [StringSplitOptions]::RemoveEmptyEntries) }
foreach ($flag in $flags) {
    switch -Regex ($flag.ToLowerInvariant()) {
        '^--skip-build$' { $skipBuild = $true }
        '^--dry-run$'    { $dryRun = $true }
        '^--yes$'        { $assumeYes = $true }
        '^--type=(release|beta|alpha)$' { $versionType = $Matches[1] }
        default { Fail "Unknown option '$flag'. Options: --dry-run, --skip-build, --type=release|beta|alpha, --yes" }
    }
}

# ---- project data ------------------------------------------------------------------------

$root = $env:CHR_ROOT.TrimEnd('\')
Set-Location $root
$props = Read-Props "$root\gradle.properties"
$modVersion = $props['mod_version']
$projectSlug = $props['modrinth_project_id']
$baseName = $props['archives_base_name']
if (-not $modVersion -or -not $baseName) {
    Fail 'gradle.properties needs mod_version and archives_base_name.'
}
if (-not $projectSlug) {
    Fail 'Set modrinth_project_id in gradle.properties to the slug or ID of the Chronicle project on Modrinth.'
}

$settings = [IO.File]::ReadAllText("$root\settings.gradle")
$m = [regex]::Match($settings, 'mcVersions\s*=\s*\[([^\]]*)\]')
if (-not $m.Success) { Fail 'Could not find the mcVersions list in settings.gradle.' }
$mcVersions = [regex]::Matches($m.Groups[1].Value, "['""]([^'""]+)['""]") | ForEach-Object { $_.Groups[1].Value }
if (-not $mcVersions) { Fail 'The mcVersions list in settings.gradle is empty.' }

# ---- changelog ---------------------------------------------------------------------------

$lines = [IO.File]::ReadAllLines("$root\CHANGELOG.md", [Text.Encoding]::UTF8)
$heading = '^##\s+Chronicle\s+' + [regex]::Escape($modVersion) + '(\s|$)'
$start = -1
for ($i = 0; $i -lt $lines.Length; $i++) {
    if ($lines[$i] -match $heading) { $start = $i; break }
}
if ($start -lt 0) { Fail "CHANGELOG.md has no section '## Chronicle $modVersion'. Add it first." }
$body = New-Object System.Collections.Generic.List[string]
for ($i = $start + 1; $i -lt $lines.Length; $i++) {
    if ($lines[$i] -match '^##\s' -or $lines[$i].Trim() -eq '---') { break }
    $body.Add($lines[$i])
}
$changelog = ($body -join "`n").Trim()
if (-not $changelog) { Fail "The CHANGELOG.md section for $modVersion is empty." }

# ---- build -------------------------------------------------------------------------------

if (-not $skipBuild) {
    if ((Java-Major $env:JAVA_HOME) -lt 25) {
        $jdk = Get-ChildItem 'C:\Program Files\Java', 'C:\Program Files\Eclipse Adoptium' -Directory -ErrorAction SilentlyContinue |
            Where-Object { (Java-Major $_.FullName) -ge 25 } | Select-Object -First 1
        if (-not $jdk) { Fail 'The build needs Java 25 or newer. Set JAVA_HOME to a JDK 25.' }
        $env:JAVA_HOME = $jdk.FullName
    }
    Write-Host "Building Chronicle $modVersion for all versions with $env:JAVA_HOME ..." -ForegroundColor Cyan
    & "$root\gradlew.bat" build --console=plain
    if ($LASTEXITCODE -ne 0) { Fail 'The build failed, nothing was uploaded.' }
}

# ---- jars --------------------------------------------------------------------------------

$uploads = @()
foreach ($mc in $mcVersions) {
    $jar = "$root\build\libs\$baseName-$modVersion+mc$mc.jar"
    if (-not (Test-Path $jar)) { Fail "Missing $jar. Run without --skip-build." }
    $versionProps = Read-Props "$root\versions\$mc\gradle.properties"
    $games = $mc
    if ($versionProps['modrinth.game.versions']) { $games = $versionProps['modrinth.game.versions'] }
    $uploads += [pscustomobject]@{
        Mc     = $mc
        Jar    = $jar
        Number = "$modVersion+mc$mc"
        Games  = [string[]] ($games.Split(',') | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    }
}

# ---- Modrinth lookups --------------------------------------------------------------------

Write-Host ''
Write-Host 'Checking Modrinth ...' -ForegroundColor Cyan
$token = Get-Token
try {
    $project = Api-Get "/project/$projectSlug"
} catch {
    if (-not (Is-NotFound $_)) { Fail "Could not reach Modrinth: $($_.Exception.Message)" }
    if ($token) { Fail "Modrinth has no project '$projectSlug' that this token can see. Check modrinth_project_id and that the token belongs to the project owner." }
    # a draft is only visible with a token
    Write-Host "Project '$projectSlug' not found without login, it may still be a draft." -ForegroundColor Yellow
    $token = Get-Token -Ask
    if (-not $token) { Fail 'No Modrinth token given.' }
    try {
        $project = Api-Get "/project/$projectSlug"
    } catch {
        if (Is-NotFound $_) { Fail "Modrinth has no project '$projectSlug' that this token can see. Check modrinth_project_id and that the token belongs to the project owner." }
        Fail "Could not reach Modrinth: $($_.Exception.Message)"
    }
}
try {
    $dependencyIds = [ordered]@{}
    foreach ($slug in $dependencies.Keys) { $dependencyIds[$slug] = (Api-Get "/project/$slug").id }
    $existing = @((Api-Get "/project/$($project.id)/version") | ForEach-Object { $_.version_number })
} catch {
    Fail "Could not reach Modrinth: $($_.Exception.Message)"
}

# ---- summary -----------------------------------------------------------------------------

Write-Host ''
Write-Host "Project:  $($project.title) ($projectSlug)"
Write-Host "Version:  $modVersion ($versionType)"
Write-Host ''
$pending = @()
foreach ($u in $uploads) {
    $size = '{0:N0} KB' -f ((Get-Item $u.Jar).Length / 1KB)
    $line = '  {0,-26} {1,-24} {2}' -f $u.Number, ($u.Games -join ', '), $size
    if ($existing -contains $u.Number) {
        Write-Host "$line  (already on Modrinth, skipped)" -ForegroundColor DarkGray
    } else {
        Write-Host $line
        $pending += $u
    }
}
Write-Host ''
Write-Host '---- changelog ----' -ForegroundColor Cyan
Write-Host $changelog
Write-Host '-------------------' -ForegroundColor Cyan
Write-Host ''

function Version-Data($u) {
    $deps = @()
    foreach ($slug in $dependencies.Keys) {
        $deps += [ordered]@{ project_id = $dependencyIds[$slug]; dependency_type = $dependencies[$slug] }
    }
    return [ordered]@{
        project_id     = $project.id
        name           = "$($project.title) $($u.Number)"
        version_number = $u.Number
        changelog      = $changelog
        dependencies   = $deps
        game_versions  = $u.Games
        version_type   = $versionType
        loaders        = @('fabric')
        featured       = $false
        status         = 'listed'
        file_parts     = @('file')
        primary_file   = 'file'
    }
}

if ($pending.Count -eq 0) {
    Write-Host 'Every version is already on Modrinth. Nothing to upload.' -ForegroundColor Green
    exit 0
}
if ($dryRun) {
    Write-Host "Dry run: would upload $($pending.Count) version(s). Request data for the first one:" -ForegroundColor Yellow
    Write-Host ((Version-Data $pending[0]) | ConvertTo-Json -Depth 5)
    exit 0
}
if (-not $assumeYes) {
    $answer = Read-Host "Upload $($pending.Count) version(s) to Modrinth? (y/N)"
    if ($answer -notmatch '^(y|yes|j|ja)$') {
        Write-Host 'Cancelled, nothing was uploaded.'
        exit 0
    }
}

# ---- token -------------------------------------------------------------------------------

if (-not $token) { $token = Get-Token -Ask }
if (-not $token) { Fail 'No Modrinth token given.' }

# ---- upload ------------------------------------------------------------------------------

$client = [Net.Http.HttpClient]::new()
$client.Timeout = [TimeSpan]::FromMinutes(5)
[void] $client.DefaultRequestHeaders.TryAddWithoutValidation('Authorization', $token)
[void] $client.DefaultRequestHeaders.TryAddWithoutValidation('User-Agent', $userAgent)

$uploaded = 0
foreach ($u in $pending) {
    Write-Host "Uploading $($u.Number) ..." -NoNewline
    $json = (Version-Data $u) | ConvertTo-Json -Depth 5
    $form = [Net.Http.MultipartFormDataContent]::new()
    $form.Add([Net.Http.StringContent]::new($json, [Text.Encoding]::UTF8, 'application/json'), 'data')
    $file = [Net.Http.ByteArrayContent]::new([IO.File]::ReadAllBytes($u.Jar))
    $file.Headers.ContentType = [Net.Http.Headers.MediaTypeHeaderValue]::Parse('application/java-archive')
    $form.Add($file, 'file', [IO.Path]::GetFileName($u.Jar))
    try {
        $response = $client.PostAsync("$api/version", $form).GetAwaiter().GetResult()
        $text = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    } catch {
        Write-Host ''
        Fail "Upload of $($u.Number) failed: $($_.Exception.Message). $uploaded version(s) were uploaded; run again to continue."
    } finally {
        $form.Dispose()
    }
    if (-not $response.IsSuccessStatusCode) {
        Write-Host ''
        Fail "Modrinth rejected $($u.Number) ($([int] $response.StatusCode)): $text. $uploaded version(s) were uploaded; run again to continue."
    }
    $created = $text | ConvertFrom-Json
    Write-Host " done: https://modrinth.com/mod/$projectSlug/version/$($created.id)" -ForegroundColor Green
    $uploaded++
}

Write-Host ''
Write-Host "Uploaded $uploaded version(s) of Chronicle $modVersion to Modrinth." -ForegroundColor Green
exit 0
