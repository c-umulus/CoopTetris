package test;

import java.io.*;
import java.net.*;

/** NetworkManager 구체 구현 - 클라이언트 역할 (다형성) */
public class GameClient extends NetworkManager {

    private Socket             socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;

    @Override
    public void connect(String host, MessageHandler handler) throws Exception {
        this.handler = handler;
        socket = new Socket(host, PORT);
        out    = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        connected = true;

        Thread listenThread = new Thread(() -> {
            try {
                while (connected) {
                    GameMessage msg = (GameMessage) in.readObject();
                    if (handler != null) handler.onMessage(msg);
                }
            } catch (Exception e) {
                if (connected && handler != null) handler.onDisconnected();
            }
        }, "ClientListen");
        listenThread.setDaemon(true);
        listenThread.start();
    }

    @Override
    public synchronized void send(GameMessage msg) {
        if (out == null || !connected) return;
        try {
            out.writeObject(msg);
            out.flush();
            out.reset();
        } catch (Exception e) {
            connected = false;
        }
    }

    @Override
    public void disconnect() {
        connected = false;
        try { if (out    != null) out.close(); }    catch (Exception ignored) {}
        try { if (in     != null) in.close(); }     catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }
}
