package server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;

public class Handler extends Thread {
    private Socket socket;
    private boolean isRunning;
    private Timer timer;
    private long lastReceive;
    private Follower follower;

    public Handler(Socket socket, Follower follower) {
        this.socket = socket;
        isRunning = true;
        this.follower = follower;
        this.timer = follower.getTimer();

    }

    @Override
    public void run() {
        lastReceive = Instant.now().toEpochMilli();
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream()) {
            receiveHeartbeat(input);
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException ioe) {
            }
            System.out.println("client disconnected.");
        }
    }

    private void receiveHeartbeat(InputStream input) throws IOException {
        checkTimeout();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        while (isRunning) {
            String s = reader.readLine();
            System.out.println("Receive " + s + "from " + socket.getRemoteSocketAddress());
            lastReceive = Instant.now().toEpochMilli();
        }
    }

    public void close() {
        isRunning = false;
        follower.stop();
    }

    private void checkTimeout() {
        long now = Instant.now().toEpochMilli();
        if (now - lastReceive > Follower.HEARTBEAT_TIMEOUT) {
            // timeout
            System.out.println("timeout");
            close();
            timer.cancel();
        } else {
            System.out.println("last receive " + lastReceive);
            setTimeout(Follower.HEARTBEAT_TIMEOUT);
        }
    }

    private void setTimeout(long delay) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkTimeout();
            }
        }, delay);
    }
}
