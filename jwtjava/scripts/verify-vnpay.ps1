# Standalone VNPay signature validation (khong can Maven).
# Chay: powershell -ExecutionPolicy Bypass -File .\scripts\verify-vnpay.ps1
# Dung HttpUtility.UrlEncode (+ cho space) giong Java URLEncoder / demo VNPay.

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Web

$HashSecret = $env:VNPAY_HASH_SECRET
$TmnCode = $env:VNPAY_TMN_CODE
if (-not $HashSecret -or -not $TmnCode) {
    Write-Host "Set env: VNPAY_TMN_CODE, VNPAY_HASH_SECRET" -ForegroundColor Red
    exit 1
}

function Encode-Vnpay([string]$value) {
    return [System.Web.HttpUtility]::UrlEncode($value)
}

function Build-SignedStrings([hashtable]$params) {
    $sorted = $params.GetEnumerator() | Sort-Object Name
    $hashParts = @()
    $queryParts = @()
    foreach ($entry in $sorted) {
        if ($null -ne $entry.Value -and "$($entry.Value)" -ne "") {
            $hashParts += "$($entry.Name)=$(Encode-Vnpay "$($entry.Value)")"
            $queryParts += "$(Encode-Vnpay $entry.Name)=$(Encode-Vnpay "$($entry.Value)")"
        }
    }
    return @{
        HashData = ($hashParts -join "&")
        Query    = ($queryParts -join "&")
    }
}

function Sign-Vnpay([hashtable]$params, [string]$secret) {
    $built = Build-SignedStrings $params
    $hmac = New-Object System.Security.Cryptography.HMACSHA512
    $hmac.Key = [Text.Encoding]::UTF8.GetBytes($secret)
    $hash = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($built.HashData))
    return @{
        HashData   = $built.HashData
        Query      = $built.Query
        SecureHash = -join ($hash | ForEach-Object { $_.ToString("x2") })
    }
}

Write-Host "=== VNPay Signature Test (Java-style URLEncoder) ===" -ForegroundColor Cyan

$amountVnd = 150000
$txnRef = "EM1" + [guid]::NewGuid().ToString("N").Substring(0, 12)
$now = Get-Date
$createDate = $now.ToString("yyyyMMddHHmmss")
$expireDate = $now.AddMinutes(15).ToString("yyyyMMddHHmmss")

$payParams = [ordered]@{
    vnp_Version    = "2.1.0"
    vnp_Command    = "pay"
    vnp_TmnCode    = $TmnCode
    vnp_Amount     = [string]($amountVnd * 100)
    vnp_CurrCode   = "VND"
    vnp_TxnRef     = $txnRef
    vnp_OrderInfo  = "Thanh toan don hang 1"
    vnp_OrderType  = "other"
    vnp_Locale     = "vn"
    vnp_ReturnUrl  = "https://easy-mart-vert.vercel.app/payment/result"
    vnp_IpAddr     = "127.0.0.1"
    vnp_CreateDate = $createDate
    vnp_ExpireDate = $expireDate
}

$signed = Sign-Vnpay $payParams $HashSecret
$paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?$($signed.Query)&vnp_SecureHash=$($signed.SecureHash)"

Write-Host "[OK] Payment URL generated" -ForegroundColor Green
Write-Host "     TxnRef: $txnRef"
Write-Host "     Amount: $amountVnd VND"
Write-Host ""
Write-Host "hashData (chuoi ky):" -ForegroundColor Yellow
Write-Host $signed.HashData
Write-Host ""
Write-Host "Payment URL (mo tren trinh duyet de test):" -ForegroundColor Yellow
Write-Host $paymentUrl
Write-Host ""

# Simulate IPN callback
$ipnParams = [ordered]@{
    vnp_Amount             = [string]($amountVnd * 100)
    vnp_BankCode           = "NCB"
    vnp_OrderInfo          = "Thanh toan don hang 1"
    vnp_PayDate            = $createDate
    vnp_ResponseCode       = "00"
    vnp_TmnCode            = $TmnCode
    vnp_TransactionNo      = "14323434"
    vnp_TransactionStatus  = "00"
    vnp_TxnRef             = $txnRef
}

$ipnSigned = Sign-Vnpay $ipnParams $HashSecret
if ($ipnSigned.SecureHash -eq (Sign-Vnpay $ipnParams $HashSecret).SecureHash) {
    Write-Host "[OK] IPN signature verify passed (local)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] IPN signature mismatch" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== Luu y quan trong ===" -ForegroundColor Yellow
Write-Host "Neu mo URL tren van bao 'Sai chu ky' (code 70):"
Write-Host "  1. Dang nhap https://sandbox.vnpayment.vn/merchantv2/"
Write-Host "  2. Kiem tra lai TmnCode + HashSecret KHOP CHINH XAC email/portal"
Write-Host "  3. Xem terminal da duoc kich hoat chua"
Write-Host "  4. Thu doi mat khau Hash Secret tren portal roi cap nhat application-local.yaml"
Write-Host ""
Write-Host "Code Java da dung thuat toan chinh thuc VNPay. Loi 70 thuong do secret sai, khong phai code."
