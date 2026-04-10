package server;

public class ServerMain {

    public static void main(String[] args) {
        Server server = new Server();
        int port = server.run(8080);
        System.out.printf("240 Chess Server listening on %d%n", port);
    }
}