param([string]$Repository = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path)
$ErrorActionPreference = 'Stop'

# Scheduled time follows the machine's DST rules. Require Berlin's Windows zone.
if ((Get-TimeZone).Id -ne 'W. Europe Standard Time') {
    throw 'Set the Windows time zone to Berlin before installing the 23:30 daily task.'
}
$repoPath = (Resolve-Path -LiteralPath $Repository).Path
$scriptPath = Join-Path $repoPath 'scripts\continuity\daily-status.mjs'
if (-not (Test-Path -LiteralPath $scriptPath)) { throw 'Continuity script missing.' }
$nodePath = (Get-Command node -ErrorAction Stop).Source
$taskName = 'Shanivar-Daily-GitHub-Status'
$action = New-ScheduledTaskAction -Execute $nodePath -Argument ('"{0}" --repo "{1}" --publish' -f $scriptPath, $repoPath) -WorkingDirectory $repoPath
$trigger = New-ScheduledTaskTrigger -Daily -At '23:30'
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -MultipleInstances IgnoreNew -ExecutionTimeLimit (New-TimeSpan -Minutes 10)
$principal = New-ScheduledTaskPrincipal -UserId ([System.Security.Principal.WindowsIdentity]::GetCurrent().Name) -LogonType Interactive -RunLevel Limited
# Never replace an existing job silently; installation is one-time and inspectable.
if (Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue) {
    throw "Task $taskName already exists. Inspect its action and trigger before changing it."
}
Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings -Principal $principal -Description 'Publish metadata and stale-work warnings to codex/daily-status; never stage code or change main.' | Out-Null
Get-ScheduledTask -TaskName $taskName | Select-Object TaskName, State, Actions, Triggers
