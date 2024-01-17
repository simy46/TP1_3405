import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
	private static Socket socket;
	private static String username;
	private static String password;

	public static boolean verificationPort(int port) {
		return port > 5000 && port < 5050;
	}

	public static boolean ipVerification(String ipAdress){
		// Vérifier s'il y a des points consécutifs ou des points au début ou à la fin
		if (ipAdress.startsWith(".") || ipAdress.endsWith(".") || ipAdress.contains("..")) return false;
		//traitement des cas ou la string est vide ou quelle a plus de caractere qu e permis.
		if (ipAdress.isEmpty() || ipAdress.length() > 15) return false;

		//split du string que l'utilisateur à input
		String[] split = ipAdress.split("\\.");

		//verification que la structure de IpV4 est respectee. xxx.xxx.xxx.xxx (4 points max)
		if (split.length != 4) return false;

		//Cast les les string du tableau en int pour verifier qu'il n'y a pas de lettres
		for ( String i: split) {
			int number;
			try {
				number = Integer.parseInt(i);
			} catch (NumberFormatException error) {
				return false;
			}
			if (number > 255 || number < 0) return false;
		}
		return true; // si tous les etpaes de verifications sont passees.
	}
	
	


	public static void main(String[] args) {
		try { 
			// Adresse et port du serveur
			String serverAddress; //"10.200.12.99"
			int port = 5030;

			boolean correctFormat;
			Scanner scanner = new Scanner(System.in);
			do {
				
				System.out.println("Entrez l'adresse ipv4 du serveur : ");
				serverAddress = scanner.nextLine();
				correctFormat = ipVerification(serverAddress);
				if (!correctFormat) System.out.println("mauvais format d'adresse ip, veuillez recommencer. \n");

			} while (!correctFormat);

			System.out.println("Enter username");
			username = scanner.nextLine();
			System.out.println("Enter password");
			password = scanner.nextLine();
			
			scanner.close();


			// Création d'une nouvelle connexion avec le serveur
			socket = new Socket(serverAddress, port);
			System.out.format("Connecté au serveur sur [%s : %d]%n", serverAddress, port);

			// Création des canaux de communication avec le serveur
			DataOutputStream outClient = new DataOutputStream(socket.getOutputStream());
			DataInputStream inClient = new DataInputStream(socket.getInputStream());

			// Envoi du nom d'utilisateur et du mot de passe au serveur
			outClient.writeUTF(username);
			outClient.writeUTF(password);

			// Attente de la réception d'un message envoyé par le serveur sur le canal
			String helloMessageFromServer = inClient.readUTF();
			System.out.println(helloMessageFromServer);

			// Fermeture de la connexion avec le serveur
			socket.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
