package server;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

public class LeaderServer {
    private String host;
    private int port;
    private List<String> nodes;

    public LeaderServer() {
        host = "localhost";
        port = 9002;
//        nodes = Arrays.asList("localhost:9000", "localhost:9001");
        nodes = Arrays.asList("localhost:9000");
    }

    public void start() {
        for (String peer : nodes) {
            Thread connectFollower = new Thread(new Leader(peer));
            connectFollower.start();
        }
    }


    public static void main(String[] args) {
        Socket socket = new Socket();
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LeaderServer leaderServer = new LeaderServer();
        leaderServer.start();
    }

}
