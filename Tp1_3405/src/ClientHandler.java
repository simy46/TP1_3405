import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket socket;
    private int clientNumber;
    private String username;

    public ClientHandler(Socket socket, int clientNumber, String username) {
        this.socket = socket;
        this.clientNumber = clientNumber;
        this.username = username;

        System.out.println(String.format("New connection with %s: (%d) at %s", username, clientNumber, socket));
    }

    public void run() {
        try (
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            // Lecture du nom d'utilisateur et du mot de passe
            String receivedUsername = in.readUTF();
            String receivedPassword = in.readUTF();

            if (validateUserCredentials(receivedUsername, receivedPassword)) {
                // Envoyer un message de bienvenue au client
                out.writeUTF("Bienvenue, " + receivedUsername + "!");
            } else {
                // Envoyer un message d'erreur au client
                out.writeUTF("Erreur d'authentification. Veuillez vérifier vos informations.");
            }

        } catch (IOException e) {
            System.out.println("Erreur lors du traitement du client # " + clientNumber + ": " + e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Impossible de fermer le socket, que se passe-t-il ?");
            }
            System.out.println(String.format("Connexion avec %s (%d) fermée", new String(username), clientNumber));
        }
    }

    private boolean validateUserCredentials(String receivedUsername, String receivedPassword) {
        // Ajoutez ici votre logique pour valider les noms d'utilisateur et les mots de passe
        // Vous pouvez utiliser la méthode Serveur.usernameExist(username) pour vérifier si l'utilisateur existe
        // et vérifier le mot de passe correspondant dans la base de données.
        return Serveur.usernameExist(receivedUsername) && Serveur.getDataBase().get(receivedUsername).equals(receivedPassword);
    }
}
