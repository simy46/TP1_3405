import java.util.Scanner;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
	private static Socket socket;
	private static String username;
	private static String password;
	private static String serverIP;
	private static int serverPort;


	public static void askingForUsernameAndPassword(DataInputStream inClient, DataOutputStream outClient,Scanner scanner) {
		try {
			System.out.println("Entrez votre nom d'utilisateur : ");
			username = scanner.nextLine();

			System.out.println("Entrez votre mot de passe : ");
			password = scanner.nextLine();

			outClient.writeUTF(username);
			outClient.writeUTF(password);

			String responseFromServer = inClient.readUTF();
			System.out.println("Réponse du serveur : " + responseFromServer);

			boolean credentialsValid = responseFromServer.contains(Serveur.ANSI_GREEN);


			while (!credentialsValid) {
				System.out.println(Serveur.ANSI_RED + "Mot de passe incorrect.\n" + Serveur.ANSI_WHITE);

				System.out.println("Entrez votre mot de passe : ");
				password = scanner.nextLine();

				outClient.writeUTF(username);
				outClient.writeUTF(password);

				responseFromServer = inClient.readUTF();
				System.out.println("Réponse du serveur : " + responseFromServer);

				credentialsValid = responseFromServer.contains(Serveur.ANSI_GREEN);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}





	public static void connectToServer() throws UnknownHostException, IOException {
		Scanner scanner = new Scanner(System.in);
		try {
			socket = new Socket(serverIP, serverPort);
			System.out.format("Connecté au serveur sur [%s%s%s : %d] %n", Serveur.ANSI_BLUE, serverIP, Serveur.ANSI_WHITE, serverPort);

			DataOutputStream outClient = new DataOutputStream(socket.getOutputStream());
			DataInputStream inClient = new DataInputStream(socket.getInputStream());

			askingForUsernameAndPassword(inClient, outClient,scanner);

			while (!ClientHandler.DisconnectRequested()) {
				sendMessageToServer(inClient, outClient, scanner);
			}


		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	public static void sendMessageToServer(DataInputStream inClient, DataOutputStream outClient, Scanner scanner) throws IOException {
	    try {
	        System.out.println("Saisissez votre réponse (200 caractères maximum) ou tapez 'exit' pour quitter : ");

	        if (scanner.hasNextLine()) {
	            String userResponse = scanner.nextLine();
	            outClient.writeUTF(userResponse);

	            String responseFromServer = inClient.readUTF();
	            System.out.println("Réponse du serveur : " + responseFromServer);
	        }
	    } finally {}
	}




	public static void main(String[] args) {
		try {
			serverIP = Serveur.askForIP();
			serverPort = Serveur.askForPort();
			connectToServer();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (socket != null && !socket.isClosed()) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
