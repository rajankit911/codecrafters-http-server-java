import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Main {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        try (ServerSocket serverSocket = new ServerSocket(4221)) {

            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
                System.out.println("accepted new connection");
                executorService.submit(new SocketProcessor(clientSocket));
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}

class SocketProcessor implements Runnable {
    private Socket socket;
    public SocketProcessor(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        String response = "";
        try {
            InputStream is = this.socket.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            byte[] buffer = new byte[1024];
            int len = bis.read(buffer);
            String receivedData = new String(buffer, 0, len);
            System.out.println("Received: " + receivedData);
            String[] parts = receivedData.split("\r\n");
            String[] statusLine = parts[0].split(" ");
            System.out.println("URL: " + statusLine[1]);
            if (statusLine[1].equals("/")) {
                System.out.println("Inside /");
                response = "HTTP/1.1 200 OK\r\n\r\n";
            } else if (statusLine[1].startsWith("/echo/")) {
                System.out.println("Inside /echo");
                String content = statusLine[1].substring(6);
                int sz = content.length();
                response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: " + sz + "\r\n\r\n" + content;
            } else if (statusLine[1].startsWith("/user-agent")) {
                System.out.println("Inside /user-agent");
                for (String part : parts) {
                    if (part.toLowerCase().startsWith("user-agent: ")) {
                        String content = part.substring(12);
                        int sz = content.length();
                        response = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/plain\r\n" +
                                "Content-Length: " + sz + "\r\n\r\n" + content;
                    }
                }
            } else {
                System.out.println("Inside Not found");
                response = "HTTP/1.1 404 Not Found\r\n\r\n";
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            this.socket.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("connection closed!");
    }
}
