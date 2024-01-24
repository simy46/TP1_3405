import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    private static Socket socket;
    private static String username;
    private static String password;
    private static String serverIP;
    private static int serverPort;
    private static boolean disconnect = false;

    public static boolean verificationPort(int port) {
        return port > 5000 && port < 5050;
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

    public static void askingForIpAndPort() {
        boolean correctIpFormat;
        boolean correctPortFormat;
        Scanner scanner = new Scanner(System.in);
        do {
            System.out.println("Entrez l'adresse IPv4 du serveur : ");
            serverIP = scanner.nextLine();
            correctIpFormat = ipVerification(serverIP);

            if (!correctIpFormat) {
                System.out.println("Format d'adresse IP incorrect, veuillez réessayer. \n");
            }

        } while (!correctIpFormat);

        do {
            System.out.println("Entrez le port du serveur (entre 5000 et 5050) : ");
            String portString = scanner.nextLine();

            try {
                serverPort = Integer.parseInt(portString);
                correctPortFormat = verificationPort(serverPort);

                if (!correctPortFormat) {
                    System.out.println("Format de port incorrect, veuillez réessayer. \n");
                }

            } catch (NumberFormatException e) {
                System.out.println("Format de port incorrect, veuillez entrer un nombre entier. \n");
                correctPortFormat = false;
            }

        } while (!correctPortFormat);
    }
    
    private static void askingForUsernameAndPassword() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Entrez le nom d'utilisateur : ");
        username = scanner.nextLine();
        System.out.println("Entrez le mot de passe : ");
        password = scanner.nextLine();
    }

    public static void connectToServer() throws UnknownHostException, IOException {
        try {
            socket = new Socket(serverIP, serverPort);
            System.out.format("Connecté au serveur sur [%s : %d] %n", serverIP, serverPort);

            DataOutputStream outClient = new DataOutputStream(socket.getOutputStream());
            DataInputStream inClient = new DataInputStream(socket.getInputStream());

            askingForUsernameAndPassword();

            outClient.writeUTF(username);
            outClient.writeUTF(password);

            String responseFromServer = inClient.readUTF();
            System.out.println("Réponse du serveur : " + responseFromServer);

        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    public static void sendMessageToServer() throws IOException {
        try (
            Scanner scanner = new Scanner(System.in);
            DataOutputStream outClient = new DataOutputStream(socket.getOutputStream())) {

            System.out.println("Saisissez votre réponse (200 caractères maximum) : ");
            String userResponse = scanner.nextLine();

            if ("exit".equals(userResponse)) {
                disconnect = true;
                socket.close();
            }

            if (userResponse.length() > 200) {
                System.out.println("La réponse ne doit pas dépasser 200 caractères.");
                return;
            }

            outClient.writeUTF(userResponse);
        }
    }

    public static void main(String[] args) {
        try {
            askingForIpAndPort();
            connectToServer();

            while (!disconnect) {
                sendMessageToServer();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
