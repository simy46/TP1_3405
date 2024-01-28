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

        System.out.println(String.format("Nouvelle connexion avec %s (%d) sur %s", username, clientNumber, socket));
    }

    public void run() {
        try (
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            // Informer le client de la connexion réussie
            out.writeUTF("Connexion réussie. Bienvenue, " + username + "!");

            // Boucle de traitement des messages du client
            while (true) {
                String clientMessage = in.readUTF();

                // Traitez le message ici selon vos besoins
                System.out.println("Message reçu de " + username + ": " + clientMessage);

                // Vous pouvez ajouter votre logique de traitement ici
                // Par exemple, vous pourriez diffuser le message à tous les autres clients

                // ...

                // Exemple de réponse au client
                String response = "Message reçu avec succès.";
                out.writeUTF(response);
            }

        } catch (IOException e) {
            // Gérer l'exception (par exemple, déconnexion du client)
            System.out.println("Le client " + username + " s'est déconnecté.");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Impossible de fermer le socket, que se passe-t-il ?");
            }
            System.out.println(String.format("Connexion avec %s (%d) fermée", new String(username), clientNumber));
        }
    }
}
