import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {

    private static final String SERVER_ADDRESS = "localhost"; // Use "localhost" for same machine, or server IP
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Socket socket = null;
        BufferedReader in = null;
        PrintWriter out = null;

        try {
            System.out.println("Connecting to chat server at " + SERVER_ADDRESS + ":" + SERVER_PORT + "...");
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            System.out.println("Connected to the chat server.");

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true); // true for auto-flush

            String line;
            String userName = "";

            // Thread to listen for messages from the server
            Thread readThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        if (serverMessage.startsWith("SUBMITNAME")) {
                            System.out.print("Enter your username: ");
                            userName = scanner.nextLine();
                            out.println(userName); // Send username to server
                        } else {
                            System.out.println(serverMessage); // Display server messages (including other users' messages)
                        }
                    }
                } catch (SocketException e) {
                    System.out.println("Disconnected from server.");
                } catch (IOException e) {
                    System.err.println("Error reading from server: " + e.getMessage());
                }
            });
            readThread.start(); // Start the thread to receive messages

            // Main thread to send messages to the server
            System.out.println("You can start typing messages. Type 'QUIT' to exit.");
            while (true) {
                String messageToSend = scanner.nextLine();
                if (messageToSend.equalsIgnoreCase("QUIT")) {
                    out.println("QUIT"); // Inform server about quitting
                    break;
                }
                out.println(messageToSend); // Send message to server
            }

        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + SERVER_ADDRESS);
        } catch (ConnectException e) {
            System.err.println("Connection refused. Is the server running and accessible at " + SERVER_ADDRESS + ":" + SERVER_PORT + "?");
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        } finally {
            System.out.println("Client disconnecting.");
            try {
                if (socket != null) socket.close();
                if (in != null) in.close();
                if (out != null) out.close();
                if (scanner != null) scanner.close();
            } catch (IOException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
}