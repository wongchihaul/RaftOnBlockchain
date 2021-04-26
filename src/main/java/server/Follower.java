package server;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Timer;

public class Follower {
    public final static long HEARTBEAT_TIMEOUT = 5000;

    private String host;
    private final int port;
    private Timer timer;
    private ServerSocket server;

    private boolean isRunning;


    public Follower(int port) {
        this("localhost", port);
    }

    public Follower(String host, int port) {
        this.host = host;
        this.port = port;
        timer = new Timer();
        isRunning = true;
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        System.out.println("Follower Server start, port = " + port);
        while (isRunning) {
            try {
                Socket socket = server.accept();
                if (socket.isConnected()) {
                    Thread handler = new Handler(socket, this);
                    handler.start();
                    handler.join();
                }
            } catch (SocketException e) {
                System.out.println("server close");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Follower server stop");
    }

    public void stop() {
        isRunning = false;
        try {
            server.close();
        } catch (IOException e) {
            //
        }
    }

    public Timer getTimer() {
        return timer;
    }


    public static void main(String[] args) {
        Options options = new Options()
                .addOption("port", true, "server port");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e1) {
            System.exit(-1);
        }

        int port = 9000;
        if (cmd.hasOption("port")) {
            try {
                port = Integer.parseInt(cmd.getOptionValue("port"));
            } catch (NumberFormatException e) {
                System.out.println("-port requires a port number, parsed: " + cmd.getOptionValue("port"));
                System.exit(-1);
            }
        }

        Follower follower = new Follower(port);
        follower.run();
    }

}
