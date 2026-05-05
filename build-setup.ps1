# ── Build Setup Script ──────────────────────────────────────────────────────
# Chạy script này sau khi đã tải xong:
#   - JDK 17 zip  → $env:TEMP\jdk17.zip
#   - cmdtools.zip → $env:TEMP\cmdtools.zip

$SDK_DIR   = "C:\AndroidSDK"
$JDK_DIR   = "C:\JDK17"
$GRADLE    = "C:\Users\Admin\.gradle\wrapper\dists\gradle-8.10.2-all\7iv73wktx1xtkvlq19urqw1wm\gradle-8.10.2\bin\gradle.bat"

# ── 1. Extract JDK ───────────────────────────────────────────────────────────
if (-not (Test-Path "$JDK_DIR\bin\java.exe")) {
    Write-Host "[1/5] Extracting JDK..."
    New-Item -ItemType Directory -Force $JDK_DIR | Out-Null
    Expand-Archive -Path "$env:TEMP\jdk17.zip" -DestinationPath "$env:TEMP\jdk_tmp" -Force
    $inner = Get-ChildItem "$env:TEMP\jdk_tmp" -Directory | Select-Object -First 1
    Move-Item "$($inner.FullName)\*" $JDK_DIR -Force
    Remove-Item "$env:TEMP\jdk_tmp" -Recurse -Force
} else {
    Write-Host "[1/5] JDK already extracted"
}

$env:JAVA_HOME = $JDK_DIR
$env:PATH = "$JDK_DIR\bin;$env:PATH"
Write-Host "Java: $(& "$JDK_DIR\bin\java.exe" -version 2>&1 | Select-Object -First 1)"

# ── 2. Extract Android cmdline-tools ────────────────────────────────────────
$cmdToolsDir = "$SDK_DIR\cmdline-tools\latest"
if (-not (Test-Path "$cmdToolsDir\bin\sdkmanager.bat")) {
    Write-Host "[2/5] Extracting Android cmdline-tools..."
    New-Item -ItemType Directory -Force $cmdToolsDir | Out-Null
    Expand-Archive -Path "$env:TEMP\cmdtools.zip" -DestinationPath "$env:TEMP\cmdtools_tmp" -Force
    $inner = Get-ChildItem "$env:TEMP\cmdtools_tmp" -Directory | Select-Object -First 1
    Move-Item "$($inner.FullName)\*" $cmdToolsDir -Force
    Remove-Item "$env:TEMP\cmdtools_tmp" -Recurse -Force
} else {
    Write-Host "[2/5] cmdline-tools already extracted"
}

$env:ANDROID_HOME = $SDK_DIR
$env:PATH = "$cmdToolsDir\bin;$env:PATH"

# ── 3. Accept licenses + install build-tools + platform ─────────────────────
Write-Host "[3/5] Installing Android SDK components (may take a few minutes)..."
$sdkmanager = "$cmdToolsDir\bin\sdkmanager.bat"

# Accept licenses non-interactively
for ($i = 0; $i -lt 20; $i++) { "y" } | & $sdkmanager --licenses 2>&1 | Out-Null

# Install required components
& $sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools" 2>&1

Write-Host "[4/5] SDK components installed"

# ── 4. Create local.properties ───────────────────────────────────────────────
$sdkPath = $SDK_DIR.Replace("\","/")
@"
sdk.dir=$sdkPath
"@ | Set-Content "C:\Users\Admin\VPBankController\local.properties" -Encoding UTF8
Write-Host "[4/5] local.properties written"

# ── 5. Build APK ────────────────────────────────────────────────────────────
Write-Host "[5/5] Building APK..."
Set-Location "C:\Users\Admin\VPBankController"
$env:GRADLE_USER_HOME = "C:\gh2"
& ".\gradlew.bat" assembleDebug

$apk = "C:\Users\Admin\VPBankController\app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apk) {
    Write-Host ""
    Write-Host "✅ BUILD THÀNH CÔNG!"
    Write-Host "APK: $apk"
    Write-Host ""
    Write-Host "Cài lên điện thoại:"
    Write-Host "  adb install `"$apk`""
} else {
    Write-Host "❌ Build thất bại. Xem log phía trên."
}
