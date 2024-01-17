
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler extends Thread { // pour traiter la demande de chaque client sur un socket particulier
	private Socket socket;
	private int clientNumber;
	private String username;
	
	public ClientHandler(Socket socket, int clientNumber, String username) {
		
	    this.socket = socket;
	    this.clientNumber = clientNumber;
	    this.username = username;
	    
	    System.out.println(String.format("New connection with %s: (%d) at %s", username, clientNumber, socket));
	}


	public void run() {// Création de thread qui envoi un message à un client
		try {
			DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // création de canal d’envoi
			out.writeUTF("Hello from server - you are client #" + clientNumber); // envoi de message
		} catch (IOException e) {
			System.out.println("Error handling client # " + clientNumber + ": " + e);
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				System.out.println("Couldn't close a socket, what's going on?");
			}
			System.out.println(String.format("Connection with %s (%d) closed", new String(username), clientNumber));
		}
	}
}