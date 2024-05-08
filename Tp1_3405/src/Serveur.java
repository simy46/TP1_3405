import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class Serveur {

	private static ServerSocket Listener;
	private String serverAddress;
	private int serverPort;
	private static int clientNumber = 0;
	 private volatile boolean executionConnectClient = true;
	private CopyOnWriteArrayList<ClientHandler> listClientHandler = new CopyOnWriteArrayList<>();
	private LinkedList<String> messages = new LinkedList<>();
	public static final String ANSI_WHITE = "\u001B[0m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_BLUE = "\u001B[38;5;189m";
	public static final String ANSI_GRAY = "\u001B[90m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	private static Scanner scanner = new Scanner(System.in);


	public static String time = getCurrentTimeFormatted();

	private static String getCurrentTimeFormatted() {
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(" '('dd-MM-yyyy')@'HH:mm:ss");
		return now.format(formatter);
	}



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

	private static void displayServer(String serverAddress, int serverPort) {
		int boxWidth = 70;

		String message = String.format("%sLe serveur fonctionne sur %s%s%s : %s%d%s",
				Serveur.ANSI_GREEN,
				Serveur.ANSI_BLUE, serverAddress, Serveur.ANSI_WHITE,
				Serveur.ANSI_BLUE, serverPort, Serveur.ANSI_WHITE);

		String exitMessage = String.format("Pour quitter le serveur, tapez %s'exit'%s et appuyez sur Entrée.",
				Serveur.ANSI_RED, Serveur.ANSI_WHITE);

		String border = "+" + "-".repeat(boxWidth - 2) + "+";

	    // sfichage du message de serveur avec padding correct
	    int paddingLength1 = boxWidth - message.replaceAll("\u001B\\[[;\\d]*m", "").length() - 4;
	    String padding1 = " ".repeat(Math.max(0, paddingLength1));
	    System.out.println(border);
	    System.out.printf("| %s%s |\n", message, padding1);

	    int paddingLength2 = boxWidth - exitMessage.replaceAll("\u001B\\[[;\\d]*m", "").length() - 4;
	    String padding2 = " ".repeat(Math.max(0, paddingLength2));
	    System.out.printf("| %s%s |\n", exitMessage, padding2);

	    System.out.println(border);
	}

	protected void removeClientFromVector(int clientNumber) {
		for (ClientHandler x : listClientHandler) {
			if(x.getClientNumber()== clientNumber) {
				listClientHandler.remove(x);
			}
		}
	}

	public void connectClient() throws IOException {
		Listener = new ServerSocket();
		Listener.setReuseAddress(true);
		InetAddress serverIP = InetAddress.getByName(serverAddress);

		Listener.bind(new InetSocketAddress(serverIP, serverPort));

		displayServer(serverAddress, serverPort);

		new Thread(this::listenForExitCommand).start();
		try {
			while (executionConnectClient) {
//				if (scanner.hasNext()) { //bloque l'execution, FAIRE UN THREAD
//				    String entry = scanner.nextLine();
//				    if ("exit".equalsIgnoreCase(entry)) {
//				        closeServer();
//				    }
//				}
				ClientHandler newClient = new ClientHandler(Listener.accept(), this, clientNumber++);
				listClientHandler.add(newClient);
				newClient.start();
			}
		} finally {
			Listener.close();
		}
	}
	
	 private void listenForExitCommand() {
	        while (executionConnectClient) {
	            if (scanner.hasNext()) {
	                String entry = scanner.nextLine();
	                if ("exit".equalsIgnoreCase(entry)) {
	                    closeServer();
	                }
	            }
	        }
	    }

	protected void addMessageQueue(String newMessage) { 

		if (messages.size() == 15) {
			messages.removeFirst(); 
		}
		messages.addLast(newMessage);
	}

	public LinkedList<String> getMessages() { 
		return new LinkedList<String>(messages);
	}

	public void writeToEveryClient(int clientNumber, String clientName ,String userInput) throws IOException {
		for (ClientHandler x : listClientHandler){
			if ((x.getClientNumber() != clientNumber) && !(x.getClientName().equals(clientName))) {
				if (!x.socket.isClosed() && x.socket.isConnected()) {
					x.getDataOutputStream().writeUTF(userInput);
				}
			}
		}
	}

	private static void closeScanner() {
		scanner.close();
	}

	private void closeServer() {
		try {
			System.out.println("Arrêt du serveur en cours...");

			String shutdownMessage = Serveur.ANSI_RED + "Le serveur va se fermer. Déconnexion imminente." + Serveur.ANSI_WHITE;
			writeToEveryClient(-1, "", shutdownMessage);

			for (ClientHandler clientHandler : listClientHandler) {
				clientHandler.close();
			}
			Listener.close(); 
			System.out.println("Serveur arrêté avec succès.");
			System.exit(0);
		} catch (IOException e) {
			System.out.println("Erreur lors de la fermeture du serveur : " + e.getMessage());
		}
	}

	public static void main(String[] args) throws Exception {

		try {
			Serveur serveur = new Serveur();
			serveur.serverAddress = askForIP();
			serveur.serverPort = askForPort();
			serveur.connectClient();


		} catch (IOException e) {
			System.out.println("Serveur : exception attrape");
			e.printStackTrace();
		}
		finally{
			closeScanner();
		}
	}
}
