$SDK_DIR   = "C:\AndroidSDK"
$JDK_DIR   = "C:\JDK17"

# ── 1. JDK (already extracted) ──────────────────────────────────────────────
if (Test-Path "$JDK_DIR\bin\java.exe") {
    Write-Host "[1/5] JDK found at $JDK_DIR"
} else {
    Write-Host "[1/5] Extracting JDK..."
    New-Item -ItemType Directory -Force $JDK_DIR | Out-Null
    Expand-Archive -Path "$env:TEMP\jdk17.zip" -DestinationPath "$env:TEMP\jdk_tmp" -Force
    $inner = Get-ChildItem "$env:TEMP\jdk_tmp" -Directory | Select-Object -First 1
    Move-Item "$($inner.FullName)\*" $JDK_DIR -Force
    Remove-Item "$env:TEMP\jdk_tmp" -Recurse -Force
}

$env:JAVA_HOME = $JDK_DIR
$env:PATH = "$JDK_DIR\bin;$env:PATH"
Write-Host "Java: $(& "$JDK_DIR\bin\java.exe" -version 2>&1 | Select-Object -First 1)"

# ── 2. Extract Android cmdline-tools ────────────────────────────────────────
$cmdToolsDir = "$SDK_DIR\cmdline-tools\latest"
if (Test-Path "$cmdToolsDir\bin\sdkmanager.bat") {
    Write-Host "[2/5] cmdline-tools already extracted"
} else {
    Write-Host "[2/5] Extracting Android cmdline-tools..."
    New-Item -ItemType Directory -Force $cmdToolsDir | Out-Null
    Expand-Archive -Path "$env:TEMP\cmdtools.zip" -DestinationPath "$env:TEMP\cmdtools_tmp" -Force
    $inner2 = Get-ChildItem "$env:TEMP\cmdtools_tmp" -Directory | Select-Object -First 1
    Move-Item "$($inner2.FullName)\*" $cmdToolsDir -Force
    Remove-Item "$env:TEMP\cmdtools_tmp" -Recurse -Force
}

$env:ANDROID_HOME = $SDK_DIR
$sdkmanager = "$cmdToolsDir\bin\sdkmanager.bat"
$env:PATH = "$cmdToolsDir\bin;$env:PATH"

# ── 3. Accept licenses + install SDK components ─────────────────────────────
Write-Host "[3/5] Accepting licenses..."
$yesInput = ("y`n" * 25)
$yesInput | & $sdkmanager --licenses 2>&1 | Out-Null

Write-Host "[3/5] Installing SDK components..."
& $sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools" 2>&1
Write-Host "[3/5] Done"

# ── 4. Write local.properties ────────────────────────────────────────────────
$sdkPath = $SDK_DIR.Replace("\", "/")
$localPropsContent = "sdk.dir=$sdkPath"
Set-Content -Path "C:\Users\Admin\VPBankController\local.properties" -Value $localPropsContent -Encoding UTF8
Write-Host "[4/5] local.properties written: $localPropsContent"

# ── 5. Build APK ─────────────────────────────────────────────────────────────
Write-Host "[5/5] Building APK..."
Set-Location "C:\Users\Admin\VPBankController"
$env:GRADLE_USER_HOME = "C:\gh2"
& ".\gradlew.bat" assembleDebug

$apk = "C:\Users\Admin\VPBankController\app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apk) {
    Write-Host ""
    Write-Host "BUILD THANH CONG!"
    Write-Host "APK: $apk"
    $adb = "C:\Users\Admin\AppData\Local\Microsoft\WinGet\Packages\Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe\platform-tools\adb.exe"
    Write-Host "Cai len dien thoai: & `"$adb`" install `"$apk`""
} else {
    Write-Host "BUILD THAT BAI. Xem log phia tren."
}
