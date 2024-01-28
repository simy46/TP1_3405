import java.util.Scanner;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    private static Socket socket;
    private static String username;
    private static String password;
    private static String serverIP;
    private static int serverPort;
    private static boolean disconnect = false;



    public static void askingForUsernameAndPassword(DataInputStream inClient, Scanner scanner) {
        try {
            // Envoyer le nom d'utilisateur et le mot de passe au serveur (vous avez déjà cela dans votre code)

            // Attendre la réponse du serveur
            String responseFromServer = inClient.readUTF();
            System.out.println("Réponse du serveur : " + responseFromServer);

            // Vérifier si la réponse du serveur indique que les informations d'identification sont valides
            boolean credentialsValid = responseFromServer.contains("Connexion réussie");

            while (!credentialsValid) {
                System.out.println("Veuillez réessayer.\n");

                // Relecture du nom d'utilisateur et du mot de passe
                System.out.println("Entrez votre nom d'utilisateur : ");
                username = scanner.nextLine();

                System.out.println("Entrez votre mot de passe : ");
                password = scanner.nextLine();

                // Envoyer les nouvelles informations au serveur (vous avez déjà cela dans votre code)

                // Attendre la nouvelle réponse du serveur
                responseFromServer = inClient.readUTF();
                System.out.println("Réponse du serveur : " + responseFromServer);

                // Vérifier à nouveau si les informations d'identification sont valides
                credentialsValid = responseFromServer.contains("Connexion réussie");
            }
        } catch (IOException e) {
            // Gérer l'exception, par exemple, imprimer l'erreur
            e.printStackTrace();
        }
    }





    public static void connectToServer() throws UnknownHostException, IOException {
        Scanner scanner = new Scanner(System.in);

        try {
            // Créez le socket avant d'essayer d'envoyer des données
            socket = new Socket(serverIP, serverPort);
            System.out.format("Connecté au serveur sur [%s : %d] %n", serverIP, serverPort);

            DataOutputStream outClient = new DataOutputStream(socket.getOutputStream());
            DataInputStream inClient = new DataInputStream(socket.getInputStream());

            // Utilisez la boucle while pour continuer tant que le nom d'utilisateur existe
            // Lecture du nom d'utilisateur et du mot de passe du client
            System.out.println("Entrez votre nom d'utilisateur : ");
            username = scanner.nextLine();
            System.out.println();

            while (true) {
                System.out.println("Entrez votre mot de passe : ");
                password = scanner.nextLine();
                System.out.println();

                outClient.writeUTF(username);
                outClient.writeUTF(password);

                String responseFromServer = inClient.readUTF();

                // Vérifiez ici si la réponse indique que les informations d'identification sont valides
                if (responseFromServer.contains("Connexion réussie") || responseFromServer.contains("Création du compte réussie")) {
                    break;  // Sortez de la boucle si les informations d'identification sont valides
                } else {
                    System.out.println("Veuillez réessayer.\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void sendMessageToServer() throws IOException {
        try (Scanner scanner = new Scanner(System.in);
             DataOutputStream outClient = new DataOutputStream(socket.getOutputStream())) {

            System.out.println("Saisissez votre réponse (200 caractères maximum) ou tapez 'exit' pour quitter : ");

            if (scanner.hasNextLine()) {
                String userResponse = scanner.nextLine();

                if ("exit".equals(userResponse)) {
                    disconnect = true;
                    // Ne fermez pas la socket ici
                } else if (userResponse.length() > 200) {
                    System.out.println("La réponse ne doit pas dépasser 200 caractères.");
                } else {
                    outClient.writeUTF(userResponse);
                }
            }
        }
    }

    
    public static void main(String[] args) {
        try {
            serverIP = Serveur.askForIP();
            serverPort = Serveur.askForPort();
            connectToServer();
            while (!disconnect) {
                sendMessageToServer();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Fermez le socket ici, après avoir terminé toutes les opérations nécessaires
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
