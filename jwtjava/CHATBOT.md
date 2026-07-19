# Chatbot tư vấn sản phẩm (Gemini)

## API

```http
POST https://javabackend-olfp.onrender.com/api/v1/chat/advise
Content-Type: application/json

{
  "message": "Tôi muốn mua áo khoác dưới 500 nghìn",
  "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "categoryId": null,
  "maxBudget": 500000
}
```

Không cần JWT. Rate limit khoảng 10 request/IP/phút.

### Response

```json
{
  "code": 1000,
  "message": "Chat advice generated",
  "result": {
    "reply": "Gợi ý vài mẫu áo khoác trong ngân sách của bạn...",
    "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "products": [
      {
        "id": 12,
        "name": "Áo khoác",
        "price": 450000,
        "discountPrice": 399000,
        "stock": 8,
        "categoryName": "Fashion",
        "rating": 4.5,
        "imageUrl": "https://..."
      }
    ],
    "disclaimer": "Gợi ý dựa trên catalog Easy Mart hiện tại. Giá và tồn kho lấy từ hệ thống."
  }
}
```

`products` luôn lấy từ database — dùng danh sách này để render card sản phẩm, không tin giá trong `reply`.

## Frontend

1. Tạo `sessionId = crypto.randomUUID()` một lần khi mở chat widget; lưu `sessionStorage`.
2. Gọi API mỗi tin nhắn user; append `reply` và `products` vào UI.
3. Sau reconnect/reload: dùng lại cùng `sessionId` để nhớ tối đa 3 lượt gần nhất (TTL 30 phút trên Redis).
4. Session mới = UUID mới.

```javascript
async function askChat(message, sessionId) {
  const res = await fetch("https://javabackend-olfp.onrender.com/api/v1/chat/advise", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ message, sessionId }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.message || "Chat failed");
  return data.result;
}
```

## Render env

Trên service backend:

```
GEMINI_ENABLED=true
GEMINI_API_KEY=<key từ Google AI Studio>
GEMINI_MODEL=gemini-2.5-flash
```

Key chỉ nằm trên server, không đưa vào Vercel frontend.
