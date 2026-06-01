[CmdletBinding()]
param(
    [string] $Slug = '',
    [string] $Impact = '',
    [string] $Target = '',
    [string] $Summary = '',
    [switch] $ArchiveEmptyIntegrationNote,
    [switch] $DryRun
)

$ErrorActionPreference = 'Stop'

function Resolve-RepositoryRoot {
    $scriptDirectory = Split-Path -Parent $PSCommandPath
    return (Resolve-Path -LiteralPath (Join-Path $scriptDirectory '..')).Path
}

function Read-ProjectVersion {
    param(
        [string] $RepositoryRoot
    )

    $buildGradlePath = Join-Path $RepositoryRoot 'build.gradle'
    $versionLine = Select-String -LiteralPath $buildGradlePath -Pattern "^\s*version\s*=\s*'([^']+)'\s*$" | Select-Object -First 1
    if ($null -eq $versionLine) {
        throw "Could not read project.version from $buildGradlePath"
    }

    return $versionLine.Matches[0].Groups[1].Value
}

function Assert-VersionIsOpen {
    param(
        [string] $RepositoryRoot,
        [string] $Version
    )

    $notesDirectory = Join-Path $RepositoryRoot "src/docs/asciidoc/updates/notes/$Version"
    if ((Test-Path -LiteralPath $notesDirectory) -and
        (Get-ChildItem -LiteralPath $notesDirectory -Filter '*.adoc' -File | Select-Object -First 1)) {
        throw "API docs for project.version=$Version are already archived. Bump project.version before finalizing docs."
    }
}

function Convert-ToSlug {
    param(
        [string] $Value
    )

    $slug = ($Value.ToLowerInvariant() -replace '[^a-z0-9._-]+', '-').Trim('-._')
    if ([string]::IsNullOrWhiteSpace($slug)) {
        return 'integration-note'
    }

    if ($slug.Length -gt 60) {
        return $slug.Substring(0, 60).Trim('-._')
    }

    return $slug
}

function Remove-AsciidocInlineMarkup {
    param(
        [string] $Value
    )

    return ($Value `
        -replace 'link:[^\[]+\[([^\]]+)\]', '$1' `
        -replace 'pass:\[([^\]]+)\]', '$1' `
        -replace '`([^`]+)`', '$1' `
        -replace '\s+', ' ').Trim()
}

function Get-LatestIntegrationNoteMetadata {
    param(
        [string] $RepositoryRoot
    )

    $latestPath = Join-Path $RepositoryRoot 'src/docs/asciidoc/updates/latest.adoc'
    if (-not (Test-Path -LiteralPath $latestPath)) {
        throw "Integration note latest file does not exist: $latestPath"
    }

    $latestLines = Get-Content -LiteralPath $latestPath -Encoding UTF8
    $firstEntry = $latestLines |
        Where-Object { $_ -match '^\s*([A-Za-z][A-Za-z0-9_-]*)::\s*(.+?)\s*$' } |
        Select-Object -First 1

    if ($null -eq $firstEntry) {
        return @{
            Impact = 'Changed'
            Target = '`updates/latest.adoc`'
            Summary = 'integration note'
            Slug = 'integration-note'
        }
    }

    $entryMatch = [regex]::Match($firstEntry, '^\s*([A-Za-z][A-Za-z0-9_-]*)::\s*(.+?)\s*$')
    $derivedImpact = $entryMatch.Groups[1].Value
    $derivedSummary = Remove-AsciidocInlineMarkup $entryMatch.Groups[2].Value

    $derivedTarget = '`updates/latest.adoc`'
    foreach ($line in $latestLines) {
        foreach ($targetMatch in [regex]::Matches($line, '`([^`]+)`')) {
            $candidate = $targetMatch.Groups[1].Value.Trim()
            if ($candidate -match '^(GET|POST|PUT|PATCH|DELETE|OPTIONS|HEAD)\s+/' -or
                $candidate -match '^/' -or
                $candidate -match '^[A-Za-z][A-Za-z0-9]*(Role|Service|Controller|Repository|Request|Response|Policy|Config)$') {
                $derivedTarget = "``$candidate``"
                break
            }
        }

        if ($derivedTarget -ne '`updates/latest.adoc`') {
            break
        }
    }

    return @{
        Impact = $derivedImpact
        Target = $derivedTarget
        Summary = $derivedSummary
        Slug = Convert-ToSlug $derivedSummary
    }
}

function Invoke-Gradle {
    param(
        [string] $RepositoryRoot,
        [string[]] $Arguments
    )

    $gradle = Join-Path $RepositoryRoot 'gradlew.bat'
    & $gradle @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle command failed: .\gradlew.bat $($Arguments -join ' ')"
    }
}

$repositoryRoot = Resolve-RepositoryRoot
$previousLocation = Get-Location

try {
    Set-Location -LiteralPath $repositoryRoot

    $version = Read-ProjectVersion -RepositoryRoot $repositoryRoot
    Assert-VersionIsOpen -RepositoryRoot $repositoryRoot -Version $version
    $metadata = Get-LatestIntegrationNoteMetadata -RepositoryRoot $repositoryRoot

    if ([string]::IsNullOrWhiteSpace($Slug)) {
        $Slug = $metadata.Slug
    }
    if ([string]::IsNullOrWhiteSpace($Impact)) {
        $Impact = $metadata.Impact
    }
    if ([string]::IsNullOrWhiteSpace($Target)) {
        $Target = $metadata.Target
    }
    if ([string]::IsNullOrWhiteSpace($Summary)) {
        $Summary = $metadata.Summary
    }

    Write-Host "Finalizing API docs for project.version=$version"
    Write-Host "Integration note metadata: slug=$Slug, impact=$Impact, target=$Target"
    Write-Host "Integration note summary: $Summary"
    if ($ArchiveEmptyIntegrationNote) {
        Write-Host 'ArchiveEmptyIntegrationNote is now the default behavior; continuing without extra Gradle properties.'
    }

    if ($DryRun) {
        Write-Host "Dry run requested; no Gradle tasks were executed."
        return
    }

    Invoke-Gradle -RepositoryRoot $repositoryRoot -Arguments @('copyApiDocs')

    $generatedIndexPath = Join-Path $repositoryRoot 'build/resources/main/static/docs/index.html'
    if (-not (Test-Path -LiteralPath $generatedIndexPath)) {
        throw "API docs index was not generated: $generatedIndexPath"
    }

    $archiveArguments = @(
        'archiveIntegrationNote',
        "-PintegrationNoteSlug=$Slug",
        "-PintegrationNoteImpact=$Impact",
        "-PintegrationNoteTarget=$Target"
    )

    $archiveArguments += "-PintegrationNoteSummary=$Summary"

    Invoke-Gradle -RepositoryRoot $repositoryRoot -Arguments $archiveArguments

    $notesDirectory = Join-Path $repositoryRoot "src/docs/asciidoc/updates/notes/$version"
    if (-not (Test-Path -LiteralPath $notesDirectory)) {
        throw "Integration note archive directory was not created: $notesDirectory"
    }

    Invoke-Gradle -RepositoryRoot $repositoryRoot -Arguments @('-PforceApiDocs=true', 'copyApiDocs')

    Write-Host "API docs generated and integration note archived for project.version=$version"
}
finally {
    Set-Location -LiteralPath $previousLocation
}
