import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler extends Thread {
    private Socket socket;
    private int clientNumber;
    private String username;
    private String password;
	private static Map<String, String> database = new HashMap<>();

	public ClientHandler(Socket socket, int clientNumber) {
        this.socket = socket;
        this.setClientNumber(clientNumber);
        
        System.out.println(String.format("Nouvelle connexion : client #(%d) sur %s", clientNumber, socket));
    }

	public static Map<String, String> getDatabase() {
		return database;
	}

	private static void skipLine() {
		System.out.println();
	}
    
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
    
    private static void writeToUserFile(String filePath, String username, String password) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
			writer.write(username + "," + password);
			writer.newLine();
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
	
    
	private void processClientMessage(String message, DataOutputStream out) {
	    String response = null;
	    try {
	        if (isValidMessage(message)) {
	            System.out.println("Message reçu de " + Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE + ": " + message);

	            if ("exit".equals(message)) {
	                setDisconnectRequested(true);
	                setClientNumber(getClientNumber() - 1);
	                response = "Déconnexion demandée. Fermeture du serveur.";
	                closeSocket(); // Assurez-vous de fermer le socket ici
	            } else {
	                response = "Message délivré.";
	            }

	            // Écrire la réponse sur le flux de sortie
	            out.writeUTF(response);
	        } else {
	            System.out.println(Serveur.ANSI_RED + "Veuillez respecter le nombre de caractère %s" + Serveur.ANSI_WHITE);
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}


    
    private void connectClient(DataInputStream in, DataOutputStream out) throws IOException {
		boolean credentialsValid = false;
		while (!credentialsValid) {
			username = in.readUTF();
			password = in.readUTF();

			if (usernameExist(username)) {
				if (validateUserCredentials(username, password)) {
					String successMessage = Serveur.ANSI_GREEN + "Connexion réussie pour l'utilisateur " + Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE;
					System.out.println(successMessage);
					skipLine();
					out.writeUTF(successMessage);
					credentialsValid = true;
				} else {
					String errorMessage = Serveur.ANSI_RED + "Mot de passe incorrect pour l'utilisateur " + Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE;
					System.out.println(errorMessage);
					skipLine();
					out.writeUTF(errorMessage);
				}
			} else {
				// L'utilisateur n'existe pas, création du compte
				createUser(username, password);
				String successMessage = Serveur.ANSI_GREEN + "Création du compte réussie pour l'utilisateur " + Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE;
				System.out.println(successMessage);
				skipLine();
				out.writeUTF(successMessage);
				credentialsValid = true;
			}
		}
    }


    public void run() {
        try (
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
    		loadUserDatabase("src/user.txt");
    		connectClient(in ,out);
			
            while (!DisconnectRequested()) {
                String clientMessage = in.readUTF();
                processClientMessage(clientMessage, out);                
            }
            System.out.println(in.readUTF());
            closeSocket();
        } catch (IOException e) {
            System.out.println("Le client " + username + " s'est déconnecté.");

        }
    }

}
