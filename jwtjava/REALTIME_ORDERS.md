# Seller order updates (STOMP WebSocket)

## Connection

- Production URL: `wss://javabackend-olfp.onrender.com/ws`
- Local URL: `ws://localhost:8080/ws`
- Authentication: send the access token in the STOMP `CONNECT` header.
- Seller destination: `/user/queue/orders`

Only access tokens with the `SELLER` or `ADMIN` role can connect. A seller receives
only the items and total belonging to that seller.

## Frontend example

Install:

```bash
npm install @stomp/stompjs
```

Connect:

```javascript
import { Client } from "@stomp/stompjs";

const client = new Client({
  brokerURL: "wss://javabackend-olfp.onrender.com/ws",
  connectHeaders: {
    Authorization: `Bearer ${accessToken}`,
  },
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
});

client.onConnect = () => {
  client.subscribe("/user/queue/orders", (message) => {
    const event = JSON.parse(message.body);
    // event.type:
    // ORDER_CREATED | ORDER_STATUS_CHANGED | FULFILLMENT_STATUS_CHANGED
    // event.order: seller-filtered OrderResponse
    refreshOrMergeOrder(event.order);
  });
};

client.activate();
```

Call `client.deactivate()` when the seller logs out. After reconnecting, call
`GET /api/v1/orders/seller/history` to resynchronize because realtime events are
not persisted for offline clients.

## Update fulfillment status

```http
PATCH /api/v1/orders/{orderId}/seller-status
Authorization: Bearer <seller-access-token>
Content-Type: application/json

{"status":"CONFIRMED"}
```

Allowed sequence:

`AWAITING_CONFIRMATION` → `CONFIRMED` → `PROCESSING` → `SHIPPED` → `DELIVERED`

The order must be `PAID`. The API updates only the items owned by the authenticated
seller. An admin may add `?sellerEmail=seller@example.com`.
