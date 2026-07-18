# Order updates (STOMP WebSocket)

## Connection

- Production URL: `wss://javabackend-olfp.onrender.com/ws`
- Local URL: `ws://localhost:8080/ws`
- Authentication: send the access token in the STOMP `CONNECT` header
- Destination for both buyer and seller: `/user/queue/orders`

Access tokens with role `USER`, `SELLER`, or `ADMIN` can connect.

| Role | Payload |
|------|---------|
| Seller | Only items and total belonging to that seller |
| Buyer | Full order with all items |

## Frontend example

Install:

```bash
npm install @stomp/stompjs
```

Connect (buyer or seller):

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
    // event.order: OrderResponse
    refreshOrMergeOrder(event.order);
  });
};

client.activate();
```

Call `client.deactivate()` on logout/unmount.

After reconnect:

- Seller: `GET /api/v1/orders/seller/history`
- Buyer: `GET /api/v1/orders`

Realtime events are not persisted for offline clients.

## Events pushed to buyer

Buyer receives events on:

- Order created
- Payment success (`PAID`)
- Order cancelled
- Seller fulfillment status changes (`CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`)

## Update fulfillment status (seller)

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

After a successful update, backend pushes:

- Filtered order to the seller queue
- Full order to the buyer queue
