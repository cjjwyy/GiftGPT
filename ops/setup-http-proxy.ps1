# Run as administrator on the GiftGPT Windows server, not a development machine.
$ErrorActionPreference = 'Stop'

$health = Invoke-RestMethod 'http://127.0.0.1:3000/api/v1/health' -TimeoutSec 15
if ($health.code -ne 200 -or $health.data.service -ne 'giftgpt-server' -or $health.data.status -ne 'ok') {
    throw 'GiftGPT upstream is not healthy; no proxy changes made.'
}

$proxyKey = 'HKLM:\SYSTEM\CurrentControlSet\Services\PortProxy\v4tov4\tcp'
$existing = Get-ItemPropertyValue -Path $proxyKey -Name '0.0.0.0/80' -ErrorAction SilentlyContinue
if ($existing -and $existing -ne '127.0.0.1/3000') {
    throw "Port 80 already has a different proxy target: $existing"
}
$listeners = @(Get-NetTCPConnection -State Listen -ErrorAction Stop | Where-Object LocalPort -eq 80)
if ($listeners.Count -gt 0 -and -not $existing) {
    $listeners | Format-Table LocalAddress, LocalPort, OwningProcess
    throw 'Port 80 is already in use; refusing to replace another service.'
}

Set-Service iphlpsvc -StartupType Automatic
Start-Service iphlpsvc
netsh interface portproxy add v4tov4 listenaddress=0.0.0.0 listenport=80 connectaddress=127.0.0.1 connectport=3000 protocol=tcp
if ($LASTEXITCODE -ne 0) { throw 'Failed to configure port proxy.' }

$ruleName = 'GiftGPT-HTTP-80'
$rule = Get-NetFirewallRule -Name $ruleName -ErrorAction SilentlyContinue
if ($rule) {
    $filter = $rule | Get-NetFirewallPortFilter
    if ($filter.Protocol -ne 'TCP' -or $filter.LocalPort -ne '80' -or $rule.Direction -ne 'Inbound') {
        throw 'Existing named firewall rule has unexpected settings; refusing to overwrite it.'
    }
    $rule | Set-NetFirewallRule -Enabled True -Action Allow -Profile Any
} else {
    New-NetFirewallRule -Name $ruleName -DisplayName 'GiftGPT HTTP port 80' -Direction Inbound -Protocol TCP -LocalPort 80 -Action Allow -Profile Any | Out-Null
}

$verified = $false
for ($attempt = 0; $attempt -lt 10; $attempt++) {
    try {
        $page = Invoke-WebRequest 'http://127.0.0.1/' -UseBasicParsing -TimeoutSec 5
        $health = Invoke-RestMethod 'http://127.0.0.1/api/v1/health' -TimeoutSec 5
        if ($page.StatusCode -eq 200 -and $health.code -eq 200 -and $health.data.status -eq 'ok' -and $health.data.service -eq 'giftgpt-server') {
            $verified = $true
            break
        }
    } catch { Write-Output "Waiting for proxy: $($_.Exception.Message)" }
    Start-Sleep -Seconds 2
}
if (-not $verified) { throw 'Port 80 verification failed. Port 3000 remains unchanged; inspect the proxy and firewall.' }
netsh interface portproxy show v4tov4
Write-Output 'GiftGPT HTTP proxy verified: port 80 -> 127.0.0.1:3000 (homepage and API).'
