import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;

import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;


public class Serveur {

	private static ServerSocket Listener;
	private static String serverAddress;
	private static int serverPort;
	private static int clientNumber = 0;

	
	public static final String ANSI_WHITE = "\u001B[0m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_BLUE = "\u001B[38;5;189m";
	public static final String ANSI_GRAY = "\u001B[90m";

	
	public static int getClientNumber() {
        return clientNumber;
    }

    public static void setClientNumber(int number) {
        clientNumber = number;
    }
	
	public static ServerSocket getServerSocket() {
		return Listener;
	}


	public static boolean verificationPort(int port) {
		return port >= 5000 && port <= 5050;
	}

	public static boolean ipVerification(String ipAddress) {
		if (ipAddress.startsWith(".") || ipAddress.endsWith(".") || ipAddress.contains("..")) return false;
		if (ipAddress.isEmpty() || ipAddress.length() > 15) return false;

		String[] split = ipAddress.split("\\.");

		if (split.length != 4) return false;

		for (String i : split) {
			int number;
			try {
				number = Integer.parseInt(i);
			} catch (NumberFormatException error) {
				return false;
			}
			if (number > 255 || number < 0) return false;
		}
		return true;
	}

	public static String askForIP() {
		boolean correctIpFormat;
		Scanner scanner = new Scanner(System.in);
		String serverIP;

		do {
			System.out.println("Entrez l'adresse IPv4 du serveur : ");
			serverIP = scanner.nextLine();
			correctIpFormat = ipVerification(serverIP);

			if (!correctIpFormat) {
				System.out.println(ANSI_RED + "Format d'adresse IP incorrect, veuillez réessayer. \n" + ANSI_WHITE);
			}

		} while (!correctIpFormat);

		return serverIP;
	}

	public static int askForPort() {
		boolean correctPortFormat;
		Scanner scanner = new Scanner(System.in);
		int serverPort = 0;
		do {
			System.out.println("Entrez le port du serveur (entre 5000 et 5050) : ");
			String portString = scanner.nextLine();

			try {
				serverPort = Integer.parseInt(portString);
				correctPortFormat = verificationPort(serverPort);

				if (!correctPortFormat) {
					System.out.println(ANSI_RED + "Format de port incorrect, veuillez réessayer. \n" + ANSI_WHITE);
				}

			} catch (NumberFormatException e) {
				System.out.println(ANSI_RED + "Format de port incorrect, veuillez entrer un nombre entier. \n" + ANSI_WHITE);
				correctPortFormat = false;
			}

		} while (!correctPortFormat);

		return serverPort;
	}

	


	public static void connectClient() throws IOException {
		// Création de la connexion pour communiquer avec les clients
		Listener = new ServerSocket();
		Listener.setReuseAddress(true);
		InetAddress serverIP = InetAddress.getByName(serverAddress);

		// Association de l'adresse et du port à la connexion
		Listener.bind(new InetSocketAddress(serverIP, serverPort));
		System.out.format(ANSI_GREEN + "Le serveur fonctionne sur %s%s%s : %s%d%s %n", ANSI_BLUE, serverAddress, ANSI_WHITE, ANSI_BLUE, serverPort, ANSI_WHITE);

		try {
			while (true) {
				new ClientHandler(Listener.accept(), clientNumber++).start();
				if(clientNumber == 0) {
					break;
				}
			}
		} finally {
			Listener.close();
		}

	}



	// Application Serveur
	public static void main(String[] args) throws Exception {
		serverAddress = askForIP();
		serverPort = askForPort();
		connectClient();   
	}
}
