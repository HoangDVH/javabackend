# Test VNPay end-to-end qua API (backend phải đang chạy port 8080).
# Chạy: .\scripts\test-vnpay.ps1

$BaseUrl = "http://localhost:8080"
$Email = "admin@gmail.com"
$Password = "Admin@123456"

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [string]$Token = $null
    )
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    $params = @{
        Uri = "$BaseUrl$Path"
        Method = $Method
        Headers = $headers
    }
    if ($Body) { $params.Body = ($Body | ConvertTo-Json -Depth 5) }
    return Invoke-RestMethod @params
}

Write-Host "1. Login..." -ForegroundColor Cyan
$login = Invoke-Api -Method POST -Path "/api/v1/auth/login" -Body @{ email = $Email; password = $Password }
$token = $login.result.accessToken
Write-Host "   OK - token received" -ForegroundColor Green

Write-Host "2. Lay san pham dau tien..." -ForegroundColor Cyan
$products = Invoke-Api -Method GET -Path "/api/v1/products?page=0&size=1"
$productId = $products.result.items[0].id
Write-Host "   productId = $productId" -ForegroundColor Green

Write-Host "3. Tao don hang..." -ForegroundColor Cyan
$order = Invoke-Api -Method POST -Path "/api/v1/orders" -Token $token -Body @{
    items = @(@{ productId = $productId; quantity = 1 })
}
$orderId = $order.result.id
Write-Host "   orderId = $orderId, status = $($order.result.status)" -ForegroundColor Green

Write-Host "4. Khoi tao VNPay..." -ForegroundColor Cyan
$payment = Invoke-Api -Method POST -Path "/api/v1/payments/vnpay" -Token $token -Body @{ orderId = $orderId }
Write-Host "   status = $($payment.result.status)" -ForegroundColor Green
Write-Host "   paymentUrl:" -ForegroundColor Yellow
Write-Host "   $($payment.result.paymentUrl)"

Write-Host ""
Write-Host "5. Mo paymentUrl tren trinh duyet, thanh toan the test NCB:" -ForegroundColor Cyan
Write-Host "   So the: 9704198526191432198 | OTP: 123456"
Write-Host ""
Write-Host "6. Sau khi thanh toan, kiem tra:" -ForegroundColor Cyan
Write-Host "   Invoke-RestMethod -Uri '$BaseUrl/api/v1/orders/$orderId' -Headers @{ Authorization = 'Bearer $token' }"
Write-Host ""
Write-Host "Luu y: Can cau hinh ngrok + IPN URL tren portal VNPay de don tu dong chuyen PAID." -ForegroundColor Yellow
