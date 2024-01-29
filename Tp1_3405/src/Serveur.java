import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;

import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;


public class Serveur {

    private static ServerSocket Listener;
    private static String serverAddress;
    private static int serverPort;
    private static Map<String, String> database = new HashMap<>();
    
    public static final String ANSI_WHITE = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_BLUE = "\u001B[38;5;189m";

    
    public static Map<String, String> getDatabase() {
        return database;
    }
    
    private static void skipLine() {
    	System.out.println();
    }
    
    
    public static boolean verificationPort(int port) {
        return port >= 5000 && port <= 5050;
    }

    public static boolean ipVerification(String ipAddress) {
        if (ipAddress.startsWith(".") || ipAddress.endsWith(".") || ipAddress.contains("..")) return false;
        if (ipAddress.isEmpty() || ipAddress.length() > 15) return false;

        String[] split = ipAddress.split("\\.");

        if (split.length != 4) return false;

        for (String i : split) {
            int number;
            try {
                number = Integer.parseInt(i);
            } catch (NumberFormatException error) {
                return false;
            }
            if (number > 255 || number < 0) return false;
        }
        return true;
    }

    public static String askForIP() {
        boolean correctIpFormat;
        Scanner scanner = new Scanner(System.in);
        String serverIP;

        do {
            System.out.println("Entrez l'adresse IPv4 du serveur : ");
            serverIP = scanner.nextLine();
            correctIpFormat = ipVerification(serverIP);

            if (!correctIpFormat) {
            	skipLine();
                System.out.println(ANSI_RED + "Format d'adresse IP incorrect, veuillez réessayer. \n" + ANSI_WHITE);
            }

        } while (!correctIpFormat);
        
        return serverIP;
    }

    public static int askForPort() {
        boolean correctPortFormat;
        Scanner scanner = new Scanner(System.in);
        int serverPort = 0;
        do {
            System.out.println("Entrez le port du serveur (entre 5000 et 5050) : ");
            String portString = scanner.nextLine();

            try {
                serverPort = Integer.parseInt(portString);
                correctPortFormat = verificationPort(serverPort);

                if (!correctPortFormat) {
                	skipLine();
                    System.out.println(ANSI_RED + "Format de port incorrect, veuillez réessayer. \n" + ANSI_WHITE);
                }

            } catch (NumberFormatException e) {
            	skipLine();
                System.out.println(ANSI_RED + "Format de port incorrect, veuillez entrer un nombre entier. \n" + ANSI_WHITE);
                correctPortFormat = false;
            }

        } while (!correctPortFormat);

        return serverPort;
    }

    public static void loadUserDatabase(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String username = parts[0].trim();
                    String password = parts[1].trim();
                    database.put(username, password);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static boolean usernameExist(String username) {
        return database.containsKey(username);
    }

    private static boolean validateUserCredentials(String receivedUsername, String receivedPassword) {
        String storedPassword = database.get(receivedUsername);
        return storedPassword != null && storedPassword.equals(receivedPassword);
    }

    private static void createUser(String username, String password) {
        database.put(username, password);
        writeToUserFile("src/user.txt", username, password);
    }

    private static void writeToUserFile(String filePath, String username, String password) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(username + "," + password);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void connectClient() throws IOException {
        int clientNumber = 0;

        // Création de la connexion pour communiquer avec les clients
        Listener = new ServerSocket();
        Listener.setReuseAddress(true);
        InetAddress serverIP = InetAddress.getByName(serverAddress);

        // Association de l'adresse et du port à la connexion
        Listener.bind(new InetSocketAddress(serverIP, serverPort));
        System.out.format(ANSI_GREEN + "Le serveur fonctionne sur %s%s%s : %s%d%s %n", ANSI_BLUE, serverAddress, ANSI_WHITE, ANSI_BLUE, serverPort, ANSI_WHITE);
        skipLine();

        try {
            while (true) {
                Socket clientSocket = Listener.accept(); // Attend qu'un client se connecte

                // Création des canaux de communication avec le client
                DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

                String userName = null;
                boolean credentialsValid = false;

                while (!credentialsValid) {
                    // Lecture du nom d'utilisateur et du mot de passe du client
                    userName = dataInputStream.readUTF();
                    String password = dataInputStream.readUTF();

                    // Vérification des informations d'identification
                    if (usernameExist(userName)) {
                        if (validateUserCredentials(userName, password)) {
                            String successMessage = ANSI_GREEN + "Connexion réussie pour l'utilisateur " + ANSI_BLUE + userName + ANSI_WHITE;
                            System.out.println(successMessage);
                            skipLine();
                            dataOutputStream.writeUTF(successMessage);
                            credentialsValid = true;
                        } else {
                            String errorMessage = ANSI_RED + "Mot de passe incorrect pour l'utilisateur " + ANSI_BLUE + userName + ANSI_WHITE;
                            System.out.println(errorMessage);
                            skipLine();
                            dataOutputStream.writeUTF(errorMessage);
                        }
                    } else {
                        // L'utilisateur n'existe pas, création du compte
                        createUser(userName, password);
                        String successMessage = ANSI_GREEN + "Création du compte réussie pour l'utilisateur " + ANSI_BLUE + userName + ANSI_WHITE;
                        System.out.println(successMessage);
                        skipLine();
                        System.out.println();
                        dataOutputStream.writeUTF(successMessage);
                        credentialsValid = true;
                    }
                }

                new ClientHandler(clientSocket, clientNumber++, userName).start();
            }
        } finally {
            Listener.close();
        }

    }



    // Application Serveur
    public static void main(String[] args) throws Exception {
        loadUserDatabase("src/user.txt");
        serverAddress = askForIP();
        serverPort = askForPort();
        connectClient();   
        
    }
}
