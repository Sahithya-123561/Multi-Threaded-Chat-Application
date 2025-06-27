import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private static final int PORT = 12345; // Port for the server to listen on
    private static Set<PrintWriter> clientWriters = Collections.synchronizedSet(new HashSet<>()); // Thread-safe set for output streams

    public static void main(String[] args) {
        System.out.println("Chat Server starting on port " + PORT + "...");
        ExecutorService pool = Executors.newFixedThreadPool(50); // Thread pool to handle client connections

        try (ServerSocket listener = new ServerSocket(PORT)) {
            System.out.println("Server is listening for incoming connections...");
            while (true) {
                // Accept a new client connection
                Socket clientSocket = listener.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
                // Submit the client handler to the thread pool
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            pool.shutdown(); // Shut down the thread pool gracefully
        }
    }

    /**
     * Inner class to handle individual client connections.
     * Each client connection runs in its own thread.
     */
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // Initialize input and output streams for this client
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true); // true for auto-flush

                // Prompt client for their name
                out.println("SUBMITNAME"); // Special command for the client to send their name
                clientName = in.readLine();
                if (clientName == null || clientName.trim().isEmpty()) {
                    System.out.println("Client disconnected without submitting a name.");
                    return; // Disconnect if no name is provided
                }
                System.out.println(clientName + " has joined the chat.");
                broadcastMessage("SERVER: " + clientName + " has joined the chat.");

                clientWriters.add(out); // Add this client's writer to the set for broadcasting

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equals("QUIT")) {
                        break; // Client wants to quit
                    }
                    System.out.println(clientName + ": " + message);
                    broadcastMessage(clientName + ": " + message);
                }
            } catch (IOException e) {
                System.err.println("Error handling client " + clientName + ": " + e.getMessage());
            } finally {
                if (clientName != null) {
                    System.out.println(clientName + " has left the chat.");
                    broadcastMessage("SERVER: " + clientName + " has left the chat.");
                }
                if (out != null) {
                    clientWriters.remove(out); // Remove this client's writer
                }
                try {
                    socket.close(); // Close the client socket
                } catch (IOException e) {
                    System.err.println("Error closing client socket for " + clientName + ": " + e.getMessage());
                }
            }
        }

        /**
         * Broadcasts a message to all connected clients.
         *
         * @param message The message to broadcast.
         */
        private void broadcastMessage(String message) {
            System.out.println("Broadcasting: " + message);
            // Iterate through a copy of the set to avoid ConcurrentModificationException
            // if a client disconnects during iteration
            for (PrintWriter writer : new HashSet<>(clientWriters)) {
                writer.println(message);
            }
        }
    }
}