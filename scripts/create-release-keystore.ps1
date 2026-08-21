param(
    [switch]$GenerateRandomPasswords,
    [string]$KeystorePath = ".release\whatsapp-scheduler-release.jks",
    [string]$Alias = "whatsapp-scheduler-release"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$KeystoreFullPath = Join-Path $Root $KeystorePath
$KeystoreDir = Split-Path -Parent $KeystoreFullPath
$PropertiesPath = Join-Path $Root "keystore.properties"
$BundledKeytool = Join-Path $Root ".toolchain\jdk-17\bin\keytool.exe"

function New-LocalPassword {
    $bytes = [byte[]]::new(36)
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    [Convert]::ToBase64String($bytes).TrimEnd("=").Replace("+", "-").Replace("/", "_")
}

function Read-PlainSecret([string]$Prompt) {
    if ($GenerateRandomPasswords) {
        return New-LocalPassword
    }

    $secure = Read-Host -Prompt $Prompt -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

if (Test-Path -LiteralPath $KeystoreFullPath) {
    throw "Keystore already exists: $KeystoreFullPath"
}

if (Test-Path -LiteralPath $PropertiesPath) {
    throw "keystore.properties already exists: $PropertiesPath"
}

$Keytool = if (Test-Path -LiteralPath $BundledKeytool) { $BundledKeytool } else { "keytool.exe" }
$StorePassword = Read-PlainSecret "Release keystore password"
$KeyPassword = $StorePassword

New-Item -ItemType Directory -Force -Path $KeystoreDir | Out-Null

& $Keytool `
    -genkeypair `
    -v `
    -keystore $KeystoreFullPath `
    -storetype PKCS12 `
    -alias $Alias `
    -keyalg RSA `
    -keysize 4096 `
    -validity 10000 `
    -storepass $StorePassword `
    -dname "CN=WhatsAppScheduler, OU=Private Release, O=Personal, L=Kyiv, S=Kyiv, C=UA"

if ($LASTEXITCODE -ne 0) {
    throw "keytool failed with exit code $LASTEXITCODE"
}

$StoreFileProperty = $KeystorePath.Replace("\", "/")
@"
WASCHEDULER_STORE_FILE=$StoreFileProperty
WASCHEDULER_STORE_PASSWORD=$StorePassword
WASCHEDULER_KEY_ALIAS=$Alias
WASCHEDULER_KEY_PASSWORD=$KeyPassword
"@ | Set-Content -LiteralPath $PropertiesPath -Encoding UTF8 -NoNewline

Write-Host "Created release keystore: $KeystoreFullPath"
Write-Host "Created local signing properties: $PropertiesPath"
Write-Host "Back up both files. Do not commit them to Git."
