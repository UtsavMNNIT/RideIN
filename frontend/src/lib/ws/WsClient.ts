/**
 * Reconnecting WebSocket client for the notification stream.
 *
 * Behaviour:
 *   - exponential backoff with jitter (1s → 30s cap, 10 attempts)
 *   - emits status transitions so the UI can render a connection indicator
 *   - parses every inbound frame as JSON; malformed frames are dropped
 *   - .close(1000) is treated as an intentional shutdown (no reconnect)
 *
 * Decoupling from React: the client is a plain class. {@link WsProvider} wraps
 * it in a React context so component code stays declarative.
 */
export type WsStatus = "IDLE" | "CONNECTING" | "OPEN" | "RECONNECTING" | "CLOSED";

export type WsMessage = {
  id:        string;
  userId:    string;
  role:      string;
  type:      string;
  rideId?:   string;
  payload:   Record<string, unknown>;
  createdAt: string;
};

type Handlers = {
  onMessage: (msg: WsMessage) => void;
  onStatus:  (status: WsStatus) => void;
};

const MAX_ATTEMPTS = 10;
const BASE_DELAY   = 1_000;
const MAX_DELAY    = 30_000;

export class WsClient {
  private socket?: WebSocket;
  private attempt = 0;
  private intentionalClose = false;
  private status: WsStatus = "IDLE";
  private reconnectTimer?: ReturnType<typeof setTimeout>;

  constructor(private url: string, private handlers: Handlers) {}

  connect() {
    this.intentionalClose = false;
    this.setStatus(this.attempt === 0 ? "CONNECTING" : "RECONNECTING");

    try {
      this.socket = new WebSocket(this.url);
    } catch (e) {
      // URL parse / scheme errors land here — surface as CLOSED, no reconnect.
      console.warn("[ws] connect threw", e);
      this.setStatus("CLOSED");
      return;
    }

    this.socket.onopen = () => {
      this.attempt = 0;
      this.setStatus("OPEN");
    };

    this.socket.onmessage = (ev) => {
      try {
        const parsed = JSON.parse(String(ev.data)) as WsMessage;
        if (parsed && typeof parsed.id === "string") {
          this.handlers.onMessage(parsed);
        }
      } catch {
        // Server-issued pongs ({"type":"PONG"}) deserialise as
        // {type: "PONG"}; ignored by the id-typed guard above.
      }
    };

    this.socket.onerror = () => {
      // Don't react here — onclose fires immediately after with a reason.
    };

    this.socket.onclose = (ev) => {
      if (this.intentionalClose || ev.code === 1000) {
        this.setStatus("CLOSED");
        return;
      }
      if (this.attempt >= MAX_ATTEMPTS) {
        console.warn("[ws] giving up after", this.attempt, "attempts");
        this.setStatus("CLOSED");
        return;
      }
      const delay = Math.min(MAX_DELAY, BASE_DELAY * 2 ** this.attempt);
      const jittered = delay * (0.7 + Math.random() * 0.6); // ±30% jitter
      this.attempt += 1;
      this.setStatus("RECONNECTING");
      this.reconnectTimer = setTimeout(() => this.connect(), jittered);
    };
  }

  close() {
    this.intentionalClose = true;
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    this.socket?.close(1000, "client closing");
  }

  getStatus(): WsStatus {
    return this.status;
  }

  private setStatus(s: WsStatus) {
    if (s === this.status) return;
    this.status = s;
    this.handlers.onStatus(s);
  }
}
