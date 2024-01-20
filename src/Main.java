package src.src;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] argv) throws IOException {
        try {
            TCPMultithreadedChatServer tcpMultithreadedChatServer = new TCPMultithreadedChatServer(9922);
            tcpMultithreadedChatServer.run();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }
}

class TCPMultithreadedChatServer {
    private final ServerSocket serverSocket;
    private final Set<Socket> connectedClients;
    private final ExecutorService executorService;
    private static final String welcomeMessage = "Welcome to RUNI Computer Networks 2024 chat server! There are %d users connected \n";
    private static final int THREAD_POOL_SIZE = 10;

    public TCPMultithreadedChatServer(int serverPortNumber) throws IOException {
        this.serverSocket = new ServerSocket(serverPortNumber);
        this.connectedClients = Collections.synchronizedSet(new HashSet<Socket>());
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public String getWelcomeMessage() {
        return String.format(welcomeMessage,this.connectedClients.size()-1);
    }

    public void run() throws IOException {
        while (true) {
            Socket clientSessionSocket = this.serverSocket.accept();
            this.connectedClients.add(clientSessionSocket);
            this.sendBroadcastMessage(clientSessionSocket.getInetAddress().getHostAddress() + " joined", clientSessionSocket);
            ClientHandler clientHandler = new ClientHandler(clientSessionSocket, this);
            this.executorService.execute(clientHandler);
        }
    }

    public void sendBroadcastMessage(String broadcastMessage, Socket clientSocketToExclude) {
        synchronized (connectedClients) {
            for (Socket socket : connectedClients) {
                if (socket != clientSocketToExclude) {
                    try {
                        DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
                        outToClient.writeBytes(broadcastMessage + "\r\n");
                    } catch (IOException e) {
                        System.err.println("Error broadcasting message: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void disconnectClient(Socket clientSessionSocket) {
        this.connectedClients.remove(clientSessionSocket);
    }
}

class ClientHandler implements Runnable {

    private final Socket clientSessionSocket;
    private final TCPMultithreadedChatServer tcpMultithreadedChatServer;
    private final String clientIPAddress;
    private final int clientPortNumber;

    public ClientHandler(Socket clientSessionSocket, TCPMultithreadedChatServer tcpMultithreadedChatServer) {
        this.clientSessionSocket = clientSessionSocket;
        this.tcpMultithreadedChatServer = tcpMultithreadedChatServer;
        this.clientIPAddress = clientSessionSocket.getInetAddress().getHostAddress();
        this.clientPortNumber = clientSessionSocket.getPort();
    }

    @Override
    public void run() {
        String clientInput;
        String broadcastMessage;
        try (BufferedReader inFromClient = new BufferedReader(new InputStreamReader(this.clientSessionSocket.getInputStream()));
             DataOutputStream outToClient = new DataOutputStream(this.clientSessionSocket.getOutputStream())) {
            outToClient.writeBytes(this.tcpMultithreadedChatServer.getWelcomeMessage());
            while ((clientInput = inFromClient.readLine()) != null) {
                broadcastMessage = String.format("(%s:%d): %s", this.clientIPAddress, this.clientPortNumber, clientInput);
                this.tcpMultithreadedChatServer.sendBroadcastMessage(broadcastMessage, this.clientSessionSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                this.clientSessionSocket.close();
                this.tcpMultithreadedChatServer.disconnectClient(clientSessionSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
