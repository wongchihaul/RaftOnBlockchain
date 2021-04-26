package server;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.time.Instant;

public class Leader implements Runnable {
    private final String host;
    private final int port;
    private boolean isRunning;

    private final static long heartbeatInterval = 2000;
    private final static int MAX_RETRY = 5;

    private long lastSend;
    private int retryTimes;

    public Leader(String host, int port) {
        this.host = host;
        this.port = port;
        isRunning = true;
        retryTimes = 0;
    }

    public Leader(String peer) {
        String[] splits = peer.split(":");
        host = splits[0];
        port = Integer.parseInt(splits[1]);
        isRunning = true;
        retryTimes = 0;
    }

    private void sendHeartbeat(OutputStream output) {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
        try {
            writer.write("message: i'm alive from " + host + " : " + port);
            writer.newLine();
            writer.flush();
            System.out.println("leader send msg to " + host + " : " + port);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        isRunning = false;
    }

    private Socket getSocket(String host, int port) {
        Socket socket = null;
        try {
            socket = new Socket(host, port);
        } catch (ConnectException e) {
            retryTimes++;
            System.out.println(host + " : " + port + " 第" + retryTimes + "次超时重试");
            sleep(3000);
            if (retryTimes < MAX_RETRY) {
                socket = getSocket(host, port);
            } else {
                System.exit(-1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (socket != null && !socket.isConnected()) {
            retryTimes++;
            socket = getSocket(host, port);
        }
        retryTimes = 0;
        return socket;
    }

    private void sleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
        }
    }

    @Override
    public void run() {
        Socket socket = getSocket(host, port);
        System.out.println(socket.isConnected() + " " + socket.getInetAddress());
        OutputStream output = null;
        try {
            output = socket.getOutputStream();
        } catch (ConnectException e) {
            System.out.println("mark it");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (output != null) {
            while (isRunning) {
                long now = Instant.now().toEpochMilli();
                if (now - lastSend >= heartbeatInterval) {
                    lastSend = now;
                    sendHeartbeat(output);
                }
            }
        }
    }
}
