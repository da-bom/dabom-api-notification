# webpush-client

Standalone browser client for testing:

- `GET /push/vapidPublicKey`
- `POST /push/subscribe`
- `POST /push/send`

## Run

From repository root:

```bash
cd webpush-client
python3 -m http.server 3000
```

Open:

- `http://localhost:3000`

## Usage

1. Set backend URL (default: `http://localhost:8080`)
2. Click `Generate token`
3. Click `Register service worker`
4. Click `Request notification permission`
5. Click `Subscribe`
6. Enter message and click `Send push`

If backend JWT secret differs from default, change `JWT secret` field first.
