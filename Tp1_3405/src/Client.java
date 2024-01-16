
import java.io.DataInputStream;
import java.net.Socket;
// Application client
public class Client {
	private static Socket socket;
	
	public boolean verificationPort (int port) {
		while (port <= 5000 || port >= 5050) {
			return false;
		}
		return true;
	}
	
	public void verificationipAdresse (char[] ipAddress) {
		
	}
	
	public static void main(String[] args) throws Exception {
		// Adresse et port du serveur
		String serverAddress = "10.200.12.99";
		int port = 5000;
		// Création d'une nouvelle connexion aves le serveur
		socket = new Socket(serverAddress, port);
		System.out.format("Serveur lancé sur [%s:%d]", serverAddress, port);
		// Céatien d'un canal entrant pour recevoir les messages envoyés, par le serveur
		DataInputStream in = new DataInputStream(socket.getInputStream());
		// Attente de la réception d'un message envoyé par le, server sur le canal
		String helloMessageFromServer = in.readUTF();
		System.out.println(helloMessageFromServer);
		// fermeture de La connexion avec le serveur
		socket.close();
	}
}
