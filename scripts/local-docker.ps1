param(
    [string] $Command = 'start',
    [string] $GradleFallbackTask = $env:FLOWIT_GRADLE_FALLBACK_TASK
)

$ErrorActionPreference = 'Stop'

$AppHome = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$LocalAppImage = 'flowit-main-server:local'
$LocalAppHealthUrl = 'http://127.0.0.1:8081/actuator/health'
$LocalInfrastructureServices = @('mysql', 'redis', 'prometheus', 'grafana')
$SourceHashLabel = 'dev.runtime-lab.flowit.source-hash'
$AllowedGitRemoteName = 'origin'
$AllowedGitRemoteUrls = @(
    'https://github.com/runtime-lab/flowit-main-server.git',
    'git@github.com:runtime-lab/flowit-main-server.git'
)
$AllowedGitBranch = 'main'
$SourceHashPaths = @(
    'src/main',
    'src/docs',
    'src/test/java/dev/runtime_lab/flowit/docs',
    'src/test/resources/org/springframework/restdocs',
    'build.gradle',
    'settings.gradle',
    'gradle',
    'Dockerfile',
    '.dockerignore'
)

function Write-Info {
    param([string] $Message)
    Write-Host $Message
}

function Write-Usage {
    Write-Info 'Usage:'
    Write-Info '  .\local.bat start'
    Write-Info '  .\local.bat build-image'
    Write-Info '  .\local.bat status'
    Write-Info '  .\local.bat logs'
    Write-Info '  .\local.bat stop'
    Write-Info '  .\local.bat infra-start'
    Write-Info '  .\local.bat infra-stop'
}

function Normalize-Command {
    param([AllowNull()][string] $Value)

    switch ($Value) {
        { [string]::IsNullOrWhiteSpace($_) } { return 'start' }
        'localStart' { return 'start' }
        'localBuildImage' { return 'build-image' }
        'localInfraStart' { return 'infra-start' }
        'localInfraStop' { return 'infra-stop' }
        'localStop' { return 'stop' }
        'localStatus' { return 'status' }
        'build' { return 'build-image' }
        default { return $Value }
    }
}

function Invoke-Native {
    param([string[]] $CommandLine)

    $executable = $CommandLine[0]
    $arguments = @()
    if ($CommandLine.Length -gt 1) {
        $arguments = $CommandLine[1..($CommandLine.Length - 1)]
    }

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & $executable @arguments 2>&1 | ForEach-Object {
            if ($_ -is [System.Management.Automation.ErrorRecord]) {
                Write-Host $_.Exception.Message
            }
            else {
                Write-Host $_
            }
        }
        $exitCode = $LASTEXITCODE
        return $exitCode
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
}

function Invoke-QuietNative {
    param([string[]] $CommandLine)

    $executable = $CommandLine[0]
    $arguments = @()
    if ($CommandLine.Length -gt 1) {
        $arguments = $CommandLine[1..($CommandLine.Length - 1)]
    }

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & $executable @arguments *> $null
        return $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
}

function Invoke-QuietNativeWithTimeout {
    param(
        [string[]] $CommandLine,
        [int] $TimeoutSeconds = 5
    )

    $job = Start-Job -ScriptBlock {
        param([string[]] $InnerCommandLine)

        $executable = $InnerCommandLine[0]
        $arguments = @()
        if ($InnerCommandLine.Length -gt 1) {
            $arguments = $InnerCommandLine[1..($InnerCommandLine.Length - 1)]
        }

        & $executable @arguments *> $null
        $LASTEXITCODE
    } -ArgumentList (, $CommandLine)

    try {
        if (Wait-Job -Job $job -Timeout $TimeoutSeconds) {
            $exitCode = Receive-Job -Job $job
            if ($null -eq $exitCode) {
                return 0
            }
            return [int] $exitCode
        }

        Stop-Job -Job $job
        return 124
    }
    finally {
        Remove-Job -Job $job -Force -ErrorAction SilentlyContinue
    }
}

function Invoke-Checked {
    param([string[]] $CommandLine)

    $exitCode = Invoke-Native -CommandLine $CommandLine
    if ($exitCode -ne 0) {
        exit $exitCode
    }
}

function Get-RelativeProjectPath {
    param([string] $Path)

    $basePath = $AppHome
    if (-not $basePath.EndsWith([System.IO.Path]::DirectorySeparatorChar)) {
        $basePath = "${basePath}$([System.IO.Path]::DirectorySeparatorChar)"
    }

    $Path.Substring($basePath.Length).Replace('\', '/')
}

function Get-SourceHash {
    $sourceFiles = foreach ($relativePath in $SourceHashPaths) {
        $path = Join-Path $AppHome $relativePath
        if (Test-Path -LiteralPath $path -PathType Container) {
            Get-ChildItem -LiteralPath $path -Recurse -File -Force
        }
        elseif (Test-Path -LiteralPath $path -PathType Leaf) {
            Get-Item -LiteralPath $path -Force
        }
    }

    $entries = $sourceFiles |
        Sort-Object { Get-RelativeProjectPath $_.FullName } |
        ForEach-Object {
            $relativePath = Get-RelativeProjectPath $_.FullName
            $fileHash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
            "${relativePath}`n${fileHash}"
        }

    $text = ($entries -join "`n")
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($text)
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    try {
        ($sha256.ComputeHash($bytes) | ForEach-Object { $_.ToString('x2') }) -join ''
    }
    finally {
        $sha256.Dispose()
    }
}

function Get-ImageSourceHash {
    $template = "{{ index .Config.Labels `"$SourceHashLabel`" }}"
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = & docker image inspect --format $template $LocalAppImage 2>$null
        if ($LASTEXITCODE -ne 0) {
            return $null
        }

        $firstLine = $output | Select-Object -First 1
        if ($null -eq $firstLine) {
            return $null
        }

        $value = $firstLine.Trim()
        if ([string]::IsNullOrWhiteSpace($value) -or $value -eq '<no value>') {
            return $null
        }

        $value
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
}

function Test-Command {
    param([string] $Name)
    $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Test-Truthy {
    param([AllowNull()][string] $Value)
    if ($null -eq $Value) {
        return $false
    }

    @('true', '1', 'yes', 'y', 'on') -contains $Value.Trim().ToLowerInvariant()
}

function Test-ProductionValue {
    param([AllowNull()][string] $Value)
    if ($null -eq $Value) {
        return $false
    }

    @('prod', 'production') -contains $Value.Trim().ToLowerInvariant()
}

function Test-ProductionProfile {
    param([AllowNull()][string] $Value)
    if ($null -eq $Value) {
        return $false
    }

    foreach ($profile in ($Value -split '[,\s;]+')) {
        if (Test-ProductionValue $profile) {
            return $true
        }
    }
    return $false
}

function Test-LinuxHost {
    $isLinuxVariable = Get-Variable -Name IsLinux -ErrorAction SilentlyContinue
    if ($null -ne $isLinuxVariable) {
        return [bool] $isLinuxVariable.Value
    }

    [System.IO.Path]::DirectorySeparatorChar -eq '/'
}

function Get-LocalDockerBlockSignals {
    $signals = @()
    if (Test-Truthy $env:CI) {
        $signals += 'CI'
    }
    if (Test-Truthy $env:GITHUB_ACTIONS) {
        $signals += 'GITHUB_ACTIONS'
    }
    if (Test-Truthy $env:GITLAB_CI) {
        $signals += 'GITLAB_CI'
    }
    if (-not [string]::IsNullOrWhiteSpace($env:JENKINS_URL)) {
        $signals += 'JENKINS_URL'
    }
    if (-not [string]::IsNullOrWhiteSpace($env:KUBERNETES_SERVICE_HOST)) {
        $signals += 'KUBERNETES_SERVICE_HOST'
    }
    if (Test-ProductionValue $env:FLOWIT_ENV) {
        $signals += "FLOWIT_ENV=${env:FLOWIT_ENV}"
    }
    if (Test-ProductionProfile $env:SPRING_PROFILES_ACTIVE) {
        $signals += "SPRING_PROFILES_ACTIVE=${env:SPRING_PROFILES_ACTIVE}"
    }
    if (Test-ProductionProfile $env:SPRING_PROFILES_INCLUDE) {
        $signals += "SPRING_PROFILES_INCLUDE=${env:SPRING_PROFILES_INCLUDE}"
    }

    $signals
}

function Assert-LocalDockerAllowed {
    $signals = @(Get-LocalDockerBlockSignals)
    if ($signals.Count -gt 0) {
        throw "ERROR: refusing to run '$InvocationLabel'. Local Docker commands are for client/API local development only and are blocked in production or CI environments. Signal(s): $($signals -join ', ')"
    }
    if ((Test-LinuxHost) -and -not (Test-Truthy $env:FLOWIT_ALLOW_LOCAL_DOCKER)) {
        throw "ERROR: refusing to run '$InvocationLabel' on Linux without FLOWIT_ALLOW_LOCAL_DOCKER=true. This keeps local Docker commands out of server environments."
    }
}

function Require-Docker {
    if (-not (Test-Command 'docker')) {
        throw "ERROR: Docker is required for '$InvocationLabel'. Install and start Docker Desktop, then make the 'docker' command available."
    }

    if ((Invoke-QuietNativeWithTimeout -CommandLine @('docker', 'info') -TimeoutSeconds 5) -ne 0) {
        throw "ERROR: Docker Desktop is not running or the Docker Engine is not ready. Start Docker Desktop or start the Docker Engine, wait until it is running, then run '$InvocationLabel' again."
    }

    if ((Invoke-QuietNative -CommandLine @('docker', 'compose', 'version')) -ne 0) {
        throw "ERROR: Docker Compose is unavailable. Install Docker Desktop or a Docker CLI with the Compose plugin."
    }
}

function Invoke-GitCapture {
    param([string[]] $Arguments)

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = & git -C $AppHome @Arguments 2>&1
        [PSCustomObject]@{
            ExitCode = $LASTEXITCODE
            Output = ($output -join "`n")
        }
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
}

function Invoke-GitNative {
    param([string[]] $Arguments)

    Invoke-Native -CommandLine (@('git', '-C', $AppHome) + $Arguments)
}

function Confirm-ContinueWithCurrentSource {
    param([string] $Reason)

    Write-Info $Reason
    if ([Console]::IsInputRedirected) {
        throw 'ERROR: automatic source update did not complete and no interactive terminal is available to confirm continuing with the current source.'
    }

    $answer = Read-Host 'Continue with the current local source? [y/N]'
    switch ($answer.Trim().ToLowerInvariant()) {
        'y' {
            Write-Info 'Continuing with current local source.'
            return
        }
        'yes' {
            Write-Info 'Continuing with current local source.'
            return
        }
        default {
            throw 'ERROR: stopped because local source was not updated.'
        }
    }
}

function Sync-LocalSourceIfNeeded {
    if ($Command -notin @('start', 'build-image')) {
        return
    }

    if (Test-Truthy $env:FLOWIT_SKIP_AUTO_UPDATE) {
        Write-Info 'Local source update skipped because FLOWIT_SKIP_AUTO_UPDATE is set.'
        return
    }

    if (-not (Test-Command 'git')) {
        Write-Info 'Git is unavailable; skipping local source update.'
        return
    }

    $gitTopLevelResult = Invoke-GitCapture -Arguments @('rev-parse', '--show-toplevel')
    if ($gitTopLevelResult.ExitCode -ne 0 -or [string]::IsNullOrWhiteSpace($gitTopLevelResult.Output)) {
        Confirm-ContinueWithCurrentSource 'Could not read Git repository metadata. Automatic source update cannot verify whether this source is current.'
        return
    }
    $gitTopLevel = (Resolve-Path -LiteralPath $gitTopLevelResult.Output.Trim()).Path
    if ($gitTopLevel -ne $AppHome) {
        Write-Info "Git repository root is not the expected project root; skipping automatic update. Expected $AppHome, got $gitTopLevel."
        return
    }

    $remoteUrlResult = Invoke-GitCapture -Arguments @('remote', 'get-url', $AllowedGitRemoteName)
    if ($remoteUrlResult.ExitCode -ne 0 -or [string]::IsNullOrWhiteSpace($remoteUrlResult.Output)) {
        Write-Info "Configured remote '$AllowedGitRemoteName' is unavailable; skipping automatic update."
        return
    }
    $remoteUrl = $remoteUrlResult.Output.Trim()
    if ($AllowedGitRemoteUrls -notcontains $remoteUrl) {
        return
    }

    $branchResult = Invoke-GitCapture -Arguments @('branch', '--show-current')
    if ($branchResult.ExitCode -ne 0 -or [string]::IsNullOrWhiteSpace($branchResult.Output)) {
        throw "ERROR: repository uses allowed remote '$AllowedGitRemoteName', but is not on a named branch. Switch to '$AllowedGitBranch' or set FLOWIT_SKIP_AUTO_UPDATE=true when intentionally running without source update."
    }
    $currentBranch = $branchResult.Output.Trim()
    if ($currentBranch -ne $AllowedGitBranch) {
        throw "ERROR: current branch is '$currentBranch', but '$AllowedGitRemoteName' points to the allowed source. Switch to '$AllowedGitBranch' before running $InvocationLabel."
    }

    $remoteBranch = "$AllowedGitRemoteName/$AllowedGitBranch"

    Write-Info "Checking for source updates from $remoteBranch..."
    $fetchResult = Invoke-GitCapture -Arguments @('fetch', '--prune', '--quiet', $AllowedGitRemoteName, $AllowedGitBranch)
    if ($fetchResult.ExitCode -ne 0) {
        Confirm-ContinueWithCurrentSource "Could not fetch source updates from $remoteBranch."
        return
    }

    $countResult = Invoke-GitCapture -Arguments @('rev-list', '--left-right', '--count', "HEAD...$remoteBranch")
    if ($countResult.ExitCode -ne 0) {
        Confirm-ContinueWithCurrentSource "Could not compare local source with $remoteBranch."
        return
    }

    $counts = $countResult.Output.Trim() -split '\s+'
    if ($counts.Count -lt 2) {
        Confirm-ContinueWithCurrentSource "Could not compare local source with $remoteBranch."
        return
    }

    $ahead = [int] $counts[0]
    $behind = [int] $counts[1]
    if ($behind -eq 0) {
        Write-Info "Local source is up to date with $remoteBranch."
        return
    }

    $statusResult = Invoke-GitCapture -Arguments @('status', '--porcelain')
    if ($statusResult.ExitCode -ne 0) {
        Confirm-ContinueWithCurrentSource "Could not inspect local source changes before updating from $remoteBranch."
        return
    }
    if (-not [string]::IsNullOrWhiteSpace($statusResult.Output)) {
        Confirm-ContinueWithCurrentSource "Upstream source has $behind newer commit(s), but local changes are present. Commit or stash local changes, then run: git fetch $AllowedGitRemoteName $AllowedGitBranch; git merge --ff-only $remoteBranch"
        return
    }

    if ($ahead -gt 0) {
        Confirm-ContinueWithCurrentSource "Local branch and $remoteBranch have diverged. Resolve the branch manually, then run: git fetch $AllowedGitRemoteName $AllowedGitBranch; git merge --ff-only $remoteBranch"
        return
    }

    Write-Info "Updating local source from $remoteBranch ($behind commit(s))..."
    $mergeExitCode = Invoke-GitNative -Arguments @('merge', '--ff-only', $remoteBranch)
    if ($mergeExitCode -eq 0) {
        Write-Info 'Local source updated.'
    }
    else {
        Confirm-ContinueWithCurrentSource "Automatic source update failed. Run manually when ready: git fetch $AllowedGitRemoteName $AllowedGitBranch; git merge --ff-only $remoteBranch"
    }
}

function Get-DockerComposeBaseArgs {
    $args = @('compose', '-f', (Join-Path $AppHome 'compose.yaml'))
    if (Test-LinuxHost) {
        $args += @('-f', (Join-Path $AppHome 'compose.linux.yaml'))
    }
    $args
}

function Invoke-DockerCompose {
    param([string[]] $ComposeArgs)

    Invoke-Checked -CommandLine (@('docker') + (Get-DockerComposeBaseArgs) + $ComposeArgs)
}

function Invoke-DockerBuild {
    param([string] $SourceHash = (Get-SourceHash))

    Invoke-Checked -CommandLine @(
        'docker',
        'build',
        '--progress=plain',
        '--label',
        "${SourceHashLabel}=${SourceHash}",
        '-t',
        $LocalAppImage,
        '-f',
        (Join-Path $AppHome 'Dockerfile'),
        $AppHome
    )
}

function Test-AppHealth {
    try {
        $response = Invoke-WebRequest -Uri $LocalAppHealthUrl -UseBasicParsing -TimeoutSec 1
        return $response.StatusCode -eq 200
    }
    catch {
        return $false
    }
}

function Wait-AppHealth {
    Write-Info 'Waiting up to 90 seconds for application health...'
    $deadline = (Get-Date).AddSeconds(90)
    while ((Get-Date) -lt $deadline) {
        if (Test-AppHealth) {
            Write-Info "Health: $LocalAppHealthUrl (HTTP 200)"
            return
        }
        Start-Sleep -Seconds 1
    }

    Write-Info "Health endpoint is not ready yet: $LocalAppHealthUrl"
}

function Start-Local {
    Write-Info 'Checking local source hash...'
    $sourceHash = Get-SourceHash
    $imageExists = (Invoke-QuietNative -CommandLine @('docker', 'image', 'inspect', $LocalAppImage)) -eq 0
    $imageSourceHash = if ($imageExists) { Get-ImageSourceHash } else { $null }

    if ($imageSourceHash -eq $sourceHash) {
        Write-Info 'Source hash unchanged; reusing image.'
        Write-Info "Using existing Docker image: $LocalAppImage"
        Write-Info "Run $BuildImageLabel when you need to rebuild the application image from source."
    }
    elseif ($imageExists) {
        if ($null -eq $imageSourceHash) {
            Write-Info "Docker image source hash is missing; rebuilding Docker image: $LocalAppImage"
        }
        else {
            Write-Info 'Source hash changed; rebuilding image.'
            Write-Info "Rebuilding Docker image: $LocalAppImage"
        }
        Invoke-DockerBuild -SourceHash $sourceHash
    }
    else {
        Write-Info "Docker image not found: $LocalAppImage"
        Write-Info "Building Docker image: $LocalAppImage"
        Invoke-DockerBuild -SourceHash $sourceHash
    }

    Invoke-DockerCompose -ComposeArgs @('up', '-d')

    Write-Info 'Flowit local Docker Compose stack start requested.'
    Write-Info 'Application: http://127.0.0.1:8080'
    Wait-AppHealth
    Write-Info 'Logs: docker compose logs -f app'
}

$Command = Normalize-Command $Command
$InvocationLabel = if ([string]::IsNullOrWhiteSpace($GradleFallbackTask)) {
    ".\local.bat $Command"
}
else {
    ".\gradlew.bat $GradleFallbackTask"
}
$BuildImageLabel = if ([string]::IsNullOrWhiteSpace($GradleFallbackTask)) {
    '.\local.bat build-image'
}
else {
    '.\gradlew.bat localBuildImage'
}

if ($Command -in @('help', '-h', '--help', '/?')) {
    Write-Usage
    exit 0
}

try {
    $supportedCommands = @('start', 'build-image', 'infra-start', 'infra-stop', 'stop', 'status', 'logs')
    if ($Command -notin $supportedCommands) {
        Write-Usage
        throw "ERROR: unsupported local Docker command: $Command"
    }

    Assert-LocalDockerAllowed
    Require-Docker
    Sync-LocalSourceIfNeeded
    if (-not [string]::IsNullOrWhiteSpace($GradleFallbackTask)) {
        Write-Info "Local Java is unavailable; running '.\gradlew.bat $GradleFallbackTask' through Docker commands."
    }

    switch ($Command) {
        'start' {
            Start-Local
        }
        'build-image' {
            $sourceHash = Get-SourceHash
            Write-Info "Building Docker image: $LocalAppImage"
            Invoke-DockerBuild -SourceHash $sourceHash
        }
        'infra-start' {
            $composeArgs = @('up', '-d', '--wait') + $LocalInfrastructureServices
            $exitCode = Invoke-Native -CommandLine (@('docker') + (Get-DockerComposeBaseArgs) + $composeArgs)
            if ($exitCode -ne 0) {
                Write-Info 'Docker Compose did not accept or complete --wait. Falling back to: docker compose up -d'
                Invoke-DockerCompose -ComposeArgs (@('up', '-d') + $LocalInfrastructureServices)
            }
        }
        'infra-stop' {
            Invoke-DockerCompose -ComposeArgs (@('stop') + $LocalInfrastructureServices)
        }
        'stop' {
            Invoke-DockerCompose -ComposeArgs @('down')
        }
        'status' {
            if (Test-AppHealth) {
                Write-Info 'Application health: HTTP 200'
            }
            else {
                Write-Info 'Application health: unavailable'
            }
            Write-Info ''
            Write-Info 'Docker Compose services:'
            Invoke-DockerCompose -ComposeArgs @('ps')
        }
        'logs' {
            Invoke-DockerCompose -ComposeArgs @('logs', '-f', 'app')
        }
    }
}
catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
}
