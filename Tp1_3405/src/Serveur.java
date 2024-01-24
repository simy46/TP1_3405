
import java.util.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.DataInputStream;


public class Serveur {

	private static ServerSocket Listener;
	private static Map<String, String> DataBase;
	
	public static Map<String, String> getDataBase() {
		return DataBase;
	}

	public static void setDataBase(Map<String, String> dataBase) {
		DataBase = dataBase;
	}
	
    public static boolean usernameExist(String username) {
        return DataBase.get(username) != null;
    }
		

	// Application Serveur
	public static void main(String[] args) throws Exception {
		
		// Compteur incrémenté à chaque connexion d'un client au serveur
		int clientNumber = 0;
		Client.askingForIpAndPort();
		
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