import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(4221)) {

            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);

            Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
            System.out.println("accepted new connection");
            String response = "";
            try {
                InputStream is = clientSocket.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                byte[] buffer = new byte[1024];
                int len = bis.read(buffer);
                String receivedData = new String(buffer, 0, len);
                System.out.println("Received: " + receivedData);
                String[] parts = receivedData.split("\r\n");
                String[] statusLine = parts[0].split(" ");
                System.out.println("URL: " + statusLine[1]);
                if (statusLine[1].equals("/")) {
                    response = "HTTP/1.1 200 OK\r\n\r\n";
                } else if (statusLine[1].startsWith("/echo/")) {
                    String content = statusLine[1].substring(6);
                    int sz = content.length();
                    response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Length: " + sz + "\r\n\r\n" + content;
                } else if (statusLine[1].startsWith("/user-agent")) {
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
                    response = "HTTP/1.1 404 Not Found\r\n\r\n";
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            clientSocket.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
            System.out.println("connection closed!");
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
