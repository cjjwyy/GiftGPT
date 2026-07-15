$target = "C:\giftgpt"

Get-Process java -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like '*giftgpt*' } | Stop-Process -Force
Get-Process caddy -ErrorAction SilentlyContinue | Stop-Process -Force

Start-Process -WindowStyle Hidden -FilePath java -ArgumentList "-Djava.net.preferIPv4Stack=true -jar $target\giftgpt-server-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod"
Start-Process -WindowStyle Hidden -FilePath "$target\caddy.exe" -ArgumentList "run --config $target\Caddyfile"

netsh advfirewall firewall add rule name="GiftGPT-API" dir=in action=allow protocol=TCP localport=8080 2>$null
netsh advfirewall firewall add rule name="GiftGPT-HTTPS" dir=in action=allow protocol=TCP localport=443 2>$null
netsh advfirewall firewall add rule name="GiftGPT-HTTP" dir=in action=allow protocol=TCP localport=80 2>$null

Write-Output "Deploy done"
