
import java.util.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.io.BufferedWriter;
import java.io.FileWriter;


public class Serveur {

	private static ServerSocket Listener;
	private static Map<String, String> DataBase;
	private static int port;
	private static String serverAdress;
	
	public static Map<String, String> getDataBase() {
		return DataBase;
	}
	
    public static boolean usernameExist(String username) {
        return DataBase.get(username) != null;
    }
    
    // Permet de LIRE le fichier texte //
    public static void loadUserDatabase(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String username = parts[0].trim();
                    String password = parts[1].trim();
                    DataBase.put(username, password);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
    // Permet d'ÉCRIRE dans le fichier texte //
    public static void writeToUserFile(String filePath, String username, String password) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(username + "," + password);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // si client EXISTE => Vérifier MDP // sinon on écrit ses infos //
    private static boolean validateUserCredentials(String receivedUsername, String receivedPassword) {
        if (DataBase.containsKey(receivedUsername)) {
            return DataBase.get(receivedUsername).equals(receivedPassword);
        } else {
            DataBase.put(receivedUsername, receivedPassword);
            writeToUserFile("dataBase.txt", receivedUsername, receivedPassword);
            return true;
        }
    }

	// Application Serveur
	public static void main(String[] args) throws Exception {
		
		// Compteur incrémenté à chaque connexion d'un client au serveur
		int clientNumber = 0;
		// Client.askingForIpAndPort(); À REVOIR CAR PRIVATE ATTRIBUT DE CLIENT DANS CETTE FONCTION //
		
		// Adresse et port du serveur
		String serverAddress = "10.200.12.99";
		int serverPort = 5030;
		
		// Création de la connexien pour communiquer avec les clients
		Listener = new ServerSocket();
		Listener.setReuseAddress(true);
		InetAddress serverIP = InetAddress.getByName(serverAddress);
		
		// Association de l'adresse et du port à la connexien
		Listener.bind(new InetSocketAddress(serverIP, serverPort));
		System.out.format("The server is running on %s : %d%n", serverAddress, serverPort);
		try {
			// À chaque fois qu'un nouveau client se, connecte, on exécute la fonstion
			// run() de l'objet ClientHandler
			
			while (true) {
				Socket clientSocket = Listener.accept(); // Attend qu'un client se connecte

		        // Création des canaux de communication avec le client
		        DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());

		        // Lecture du nom d'utilisateur et du mot de passe du client
		        String userName = dataInputStream.readUTF();
		        
		        

		        // Création d'une nouvelle instance de ClientHandler pour gérer le client
		        new ClientHandler(clientSocket, clientNumber++, userName).start();
			}
			
		} finally {
			// Fermeture de la connexion
			Listener.close();
		}
	}
}