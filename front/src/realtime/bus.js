import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";

const WS_BASE = process.env.REACT_APP_WS_BASE || "/ws";


export function createStomp(onMessage, topics = ["/topic/cities"]) {
  const client = new Client({
    webSocketFactory: () => {
      console.log("%cWS", "color:#6366f1", "connecting to", WS_BASE);
      return new SockJS(WS_BASE);
    },
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: (msg) => console.log("%cSTOMP", "color:#22c55e", msg),
  });

  client.onConnect = (frame) => {
    console.log("%cSTOMP", "color:#22c55e", "connected", frame?.headers);

    topics.forEach((t) => {
      console.log("%cSUB", "color:#06b6d4", "subscribe →", t);
      client.subscribe(t, (message) => {
        try {
          let payload = message.body;
          try { payload = JSON.parse(message.body); } catch {}
          console.log("%cMSG", "color:#0ea5e9", t, payload);
          onMessage?.(message);
        } catch (e) {
          console.error("[STOMP] onMessage error:", e);
        }
      });
    });
  };

  client.onStompError = (frame) => {
    console.error(
      "[STOMP][BROKER ERROR]",
      frame?.headers?.message || "",
      frame?.body || ""
    );
  };

  client.onWebSocketError = (event) => {
    console.error("[WS][ERROR]", event);
  };

  client.onDisconnect = () => {
    console.warn("%cSTOMP", "color:#f59e0b", "disconnected");
  };

  return client;
}
