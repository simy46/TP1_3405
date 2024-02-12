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
    private CopyOnWriteArrayList<ClientHandler> listClientHandler = new CopyOnWriteArrayList<>();
	private LinkedList<String> messages = new LinkedList<>();
	public static final String ANSI_WHITE = "\u001B[0m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_BLUE = "\u001B[38;5;189m";
	public static final String ANSI_GRAY = "\u001B[90m";
	
	
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
	
	private static void displayServer(String serverAddress, int serverPort) {
	    int boxWidth = 70;

	    String message = String.format("%sLe serveur fonctionne sur %s%s%s : %s%d%s",
	            Serveur.ANSI_GREEN,
	            Serveur.ANSI_BLUE, serverAddress, Serveur.ANSI_WHITE,
	            Serveur.ANSI_BLUE, serverPort, Serveur.ANSI_WHITE);


	    String border = "+" + "-".repeat(boxWidth - 2) + "+";

	    int paddingLength = boxWidth - message.replaceAll("\u001B\\[[;\\d]*m", "").length() - 4;
	    String padding = " ".repeat(Math.max(0, paddingLength));

	    System.out.println(border);
	    System.out.printf("| %s%s |\n", message, padding);
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
	    ServerSocket Listener = new ServerSocket();
	    Listener.setReuseAddress(true);
	    InetAddress serverIP = InetAddress.getByName(serverAddress);

	    Listener.bind(new InetSocketAddress(serverIP, serverPort));
	    
	    displayServer(serverAddress, serverPort);
	    
	    try {
	        while (true) {
	        	  ClientHandler newClient = new ClientHandler(Listener.accept(), this, clientNumber++);
	                listClientHandler.add(newClient);
	                newClient.start();
	                if (clientNumber == 0) {
	                    break;
	                }
	        }
	    } finally {
	        Listener.close();
	    }
	}
	
	protected void addMessageQueue(String newMessage) { //fonction protected, utlise par clientHandler pour ecrire le message du client dans la QUEUE
	    
		if (messages.size() == 15) {
		    messages.removeFirst(); // Supprime le message le plus ancien
		}
		messages.addLast(newMessage);
	}
	
	public LinkedList<String> getMessages() { //getters pour la Queue.
		return new LinkedList<String>(messages);
	}
	
	public void writeToEveryClient(int clientNumber ,String userInput) throws IOException {
		for (ClientHandler x : listClientHandler) {
			if (x.getClientNumber() != clientNumber) {
				if (!x.socket.isClosed() && x.socket.isConnected()) {
					x.getDataOutputStream().writeUTF(userInput);
				}
			}
		}
	}


	// Application Serveur
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
	}
}
