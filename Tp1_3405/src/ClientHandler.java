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
	
	public int getClientNumber() {
		return clientNumber;
	}

	public void setClientNumber(int clientNumber) {
		this.clientNumber = clientNumber;
	}

    public ClientHandler(Socket socket, int clientNumber, String username) {
        this.socket = socket;
        this.setClientNumber(clientNumber);
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
        if (isValidMessage(message)) {
            System.out.println("Message reçu de " + Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE + ": " + message);

            String response;
            if ("exit".equals(message)) {
                setDisconnectRequested(true);
                setClientNumber(getClientNumber() - 1);
                response = "Déconnexion demandée. Fermeture du serveur.";
            } else {
                response = "Message délivré.";
            }

            try {
                out.writeUTF(response);
                if ("exit".equals(message)) {
                    closeSocket();
                }
            } catch (IOException e) {
                System.out.println("Erreur lors de l'écriture de la réponse au client " + username);
                e.printStackTrace();
            }
        } else {
            System.out.println(Serveur.ANSI_RED + "Veuillez respecter le nombre de caractère %s" + Serveur.ANSI_WHITE);
        }
    }


    public void run() {
        try (
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
            // Informer le client de la connexion réussie
            out.writeUTF("Connexion réussie. Bienvenue, " + Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE + "!");

            while (!DisconnectRequested()) {
                String clientMessage = in.readUTF();
                processClientMessage(clientMessage, out);
                String response = "Message reçu avec succès.";
                out.writeUTF(response);
                
            }
        } catch (IOException e) {
            System.out.println("Le client " + username + " s'est déconnecté.");

            // Fermeture du socket côté serveur après la déconnexion du client
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

}
