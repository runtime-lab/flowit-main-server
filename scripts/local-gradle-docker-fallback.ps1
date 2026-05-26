param(
    [Parameter(Mandatory = $true)]
    [string] $Task
)

$ErrorActionPreference = 'Stop'

$AppHome = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$LocalAppImage = 'flowit-main-server:local'
$LocalAppHealthUrl = 'http://127.0.0.1:8081/actuator/health'
$LocalInfrastructureServices = @('mysql', 'redis', 'prometheus', 'grafana')

function Write-Info {
    param([string] $Message)
    Write-Host $Message
}

function Invoke-Checked {
    param([string[]] $Command)

    $executable = $Command[0]
    $arguments = @()
    if ($Command.Length -gt 1) {
        $arguments = $Command[1..($Command.Length - 1)]
    }

    & $executable @arguments
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
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
        throw "ERROR: refusing to run '.\gradlew.bat $Task'. Local Docker tasks are for client/API local development only and are blocked in production or CI environments. Signal(s): $($signals -join ', ')"
    }
    if ((Test-LinuxHost) -and -not (Test-Truthy $env:FLOWIT_ALLOW_LOCAL_DOCKER)) {
        throw "ERROR: refusing to run '.\gradlew.bat $Task' on Linux without FLOWIT_ALLOW_LOCAL_DOCKER=true. This keeps local Docker tasks out of server environments."
    }
}

function Require-Docker {
    if (-not (Test-Command 'docker')) {
        throw "ERROR: local Java is unavailable, and Docker is required for '.\gradlew.bat $Task'. Install Java or start Docker Desktop and make the 'docker' command available."
    }

    & docker compose version *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "ERROR: Docker Compose is unavailable. Install Docker Desktop or a Docker CLI with the Compose plugin."
    }
}

function Invoke-DockerCompose {
    param([string[]] $ComposeArgs)

    $command = @('compose', '-f', (Join-Path $AppHome 'compose.yaml')) + $ComposeArgs
    Invoke-Checked -Command (@('docker') + $command)
}

function Invoke-DockerBuild {
    Invoke-Checked -Command @(
        'docker',
        'build',
        '--progress=plain',
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
    & docker image inspect $LocalAppImage *> $null
    if ($LASTEXITCODE -eq 0) {
        Write-Info "Using existing Docker image: $LocalAppImage"
        Write-Info 'Run .\gradlew.bat localBuildImage when you need to rebuild the application image from source.'
    }
    else {
        Write-Info "Docker image not found: $LocalAppImage"
        Write-Info "Building Docker image: $LocalAppImage"
        Invoke-DockerBuild
    }

    Invoke-DockerCompose -ComposeArgs @('up', '-d')

    Write-Info 'Flowit local Docker Compose stack start requested.'
    Write-Info 'Application: http://127.0.0.1:8080'
    Wait-AppHealth
    Write-Info 'Logs: docker compose logs -f app'
}

Assert-LocalDockerAllowed
Require-Docker
Write-Info "Local Java is unavailable; running '.\gradlew.bat $Task' through Docker commands."

switch ($Task) {
    'localStart' {
        Start-Local
    }
    'localBuildImage' {
        Write-Info "Building Docker image: $LocalAppImage"
        Invoke-DockerBuild
    }
    'localInfraStart' {
        $composeArgs = @('compose', '-f', (Join-Path $AppHome 'compose.yaml'), 'up', '-d', '--wait') + $LocalInfrastructureServices
        & docker @composeArgs
        if ($LASTEXITCODE -ne 0) {
            Write-Info 'Docker Compose did not accept or complete --wait. Falling back to: docker compose up -d'
            Invoke-DockerCompose -ComposeArgs (@('up', '-d') + $LocalInfrastructureServices)
        }
    }
    'localInfraStop' {
        Invoke-DockerCompose -ComposeArgs (@('stop') + $LocalInfrastructureServices)
    }
    'localStop' {
        Invoke-DockerCompose -ComposeArgs @('down')
    }
    'localStatus' {
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
    default {
        throw "ERROR: unsupported Docker fallback task: $Task"
    }
}
