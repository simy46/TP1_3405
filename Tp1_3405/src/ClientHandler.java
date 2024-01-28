import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket socket;
    private int clientNumber;
    private String username;
    
    private static volatile boolean DisconnectRequested = false;
    
    public static boolean DisconnectRequested() {
		return DisconnectRequested;
	}

	public static void setDisconnectRequested(boolean isDisconnectRequested) {
		ClientHandler.DisconnectRequested = isDisconnectRequested;
	}

    public ClientHandler(Socket socket, int clientNumber, String username) {
        this.socket = socket;
        this.clientNumber = clientNumber;
        this.username = username;

        System.out.println(String.format("Nouvelle connexion avec %s (%d) sur %s", username, clientNumber, socket));
    }
    
    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Impossible de fermer le socket, que se passe-t-il ?");
        }
    }
    
    private boolean isValidMessage(String message) {
        return message != null && message.length() <= 200;
    }
    
    private void processClientMessage(String message, DataOutputStream out) {
        System.out.println("Message reçu de " + username + ": " + message);

        // Exemple de réponse au client
        String response = "Message reçu avec succès.";
        try {
            out.writeUTF(response);
        } catch (IOException e) {
            // Gérez l'exception en fonction de votre logique
            e.printStackTrace();
        }
    }

    public void run() {
        try (
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
            // Informer le client de la connexion réussie
            out.writeUTF("Connexion réussie. Bienvenue, " + username + "!");

            while (!DisconnectRequested()) {
                String clientMessage = in.readUTF();

                // Traitez le message ici selon vos besoins
                System.out.println("Message reçu de " + username + ": " + clientMessage);

                // Vérifiez si le message est "exit"
                if (isValidMessage(clientMessage)) {
                    // Traitez le message ici selon vos besoins
                    processClientMessage(clientMessage, out);
                } else {
                    // En cas de message invalide, vous pouvez choisir d'envoyer un message d'erreur au client, ignorer, etc.
                    System.out.println("Message invalide reçu de " + username);
                }

                // Exemple de réponse au client
                String response = "Message reçu avec succès.";
                out.writeUTF(response);
            }
        } catch (IOException e) {
            // Gérer l'exception (par exemple, déconnexion du client)
            System.out.println("Le client " + username + " s'est déconnecté.");
        } finally {
            // Fermez le socket ici si nécessaire
            closeSocket();
            System.out.println(String.format("Connexion avec %s (%d) fermée", new String(username), clientNumber));
        }
    }

	

}
