import java.util.*;
import java.io.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;



public class Client {
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_WHITE = "\u001B[37m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	
	
	private static MessageHandler messageHandler;
	private static Socket socket;
	private static String username;
	private static String password;
	private static String serverIP;
	private static int serverPort;
	

	public static void askingForUsernameAndPassword(DataInputStream inClient, DataOutputStream outClient, Scanner scanner) {
	    try {
	        System.out.println("Entrez votre nom d'utilisateur : ");
	        username = scanner.nextLine();

	        System.out.println("Entrez votre mot de passe : ");
	        password = scanner.nextLine();

	        outClient.writeUTF(username);
	        outClient.writeUTF(password);

	        String responseFromServer = inClient.readUTF();
	        System.out.println(responseFromServer);

	        boolean credentialsValid = responseFromServer.contains(Serveur.ANSI_GREEN);

	        while (!credentialsValid) {
	            System.out.println("Entrez votre mot de passe : ");
	            password = scanner.nextLine();

	            outClient.writeUTF(username);
	            outClient.writeUTF(password);

	            responseFromServer = inClient.readUTF();
	            System.out.println(responseFromServer);

	            credentialsValid = responseFromServer.contains(Serveur.ANSI_GREEN);
	        }
	    } catch (EOFException e) {
	        System.out.println("Connexion interrompue : le serveur a fermé la connexion de manière inattendue.");
	    } catch (SocketException e) {
	        System.out.println("Erreur de socket : impossible de communiquer avec le serveur.");
	    } catch (IOException e) {
	        System.out.println("Erreur d'E/S : " + e.getMessage());
	    } catch (NoSuchElementException e) {
	        System.out.println("Flux d'entrée interrompu. Impossible de lire les données utilisateur.");
	    }
	}


	public static void connectToServer() {
	    Scanner scanner = new Scanner(System.in);
	    try {
	        socket = new Socket(serverIP, serverPort);
	        System.out.format("Connecté au serveur sur [%s%s%s : %s%d%s] %n", Serveur.ANSI_BLUE, serverIP, Serveur.ANSI_WHITE, Serveur.ANSI_BLUE, serverPort, Serveur.ANSI_WHITE);
	        System.out.println();
	        
	        DataOutputStream outClient = new DataOutputStream(socket.getOutputStream());
	        DataInputStream inClient = new DataInputStream(socket.getInputStream());
	        askingForUsernameAndPassword(inClient, outClient, scanner);
	        messageHandler = new MessageHandler(socket, inClient);
	        messageHandler.start();
	        while (messageHandler.isConnected) {
	            sendMessageToServer(inClient, outClient, scanner);
	        }
	        outClient.writeUTF(Serveur.ANSI_GRAY + "Déconnexion du client " + Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE);
	        return;
	        
	    } catch (UnknownHostException e) {
	        System.out.println("Impossible de se connecter au serveur : adresse IP non trouvée.");
	    } catch (IOException e) {
	        System.out.println("Client: Erreur lors de la connexion au serveur : " + e.getMessage());
	    } finally {
	        try {
	            if (socket != null && !socket.isClosed()) {
	                socket.close();
	            }
	        } catch (IOException e) {
	            System.out.println("Erreur lors de la fermeture du socket : " + e.getMessage());
	        }
	    }
	}
	
	public static void sendImageToServer(DataInputStream inClient, DataOutputStream outClient, Scanner scanner) {
			System.out.println("Voulez-vous envoyer une image? (oui/non)\n");
			String response = scanner.nextLine();
			
			if("oui".equalsIgnoreCase(response)) 
			{
				System.out.println("entrer le chemin de l'image : \n");
				String imagePath = scanner.nextLine();
				File imageFile = new File(imagePath); //cree un lien vers la location du fichier
				
				if (imageFile.exists() && !imageFile.isDirectory()) { 
				    // vérification si le fichier existe et que ce n'est pas un dossier
				    try {
				        FileInputStream fileInput = new FileInputStream(imageFile);
				        try {
				            byte[] buffer = new byte[fileInput.available()]; // Cette méthode peut ne pas être sûre pour de gros fichiers
				            int bytesRead = fileInput.read(buffer);
				            if (bytesRead == -1) {
				                throw new IOException("Aucune donnée lue du fichier, il peut être vide.");
				            }

				            outClient.writeUTF("/image");
				            outClient.writeInt(buffer.length);
				            outClient.write(buffer);
				            System.out.println("Image envoyée pour traitement.\n");
				            return;
				        } finally {
				            fileInput.close(); // Assure que le FileInputStream est fermé même en cas d'erreur
				        }
				    } catch (FileNotFoundException e) {
				        System.out.println("Le fichier n'a pas été trouvé : " + e.getMessage());
				    } catch (IOException e) {
				        System.out.println("Erreur lors de la lecture du fichier ou de la communication avec le serveur : " + e.getMessage());
				    }
				} else {
				    System.out.println("Le fichier n'existe pas ou est un dossier.");
				}

			} 
			else if("non".equalsIgnoreCase(response)) 
			{
				return;
			}
			else {
				System.out.println("Réponse invalide. Veuillez répondre par 'oui' ou 'non'.");
				sendImageToServer(inClient,outClient,scanner);
				return;
			}
	}


	public static void sendMessageToServer(DataInputStream inClient, DataOutputStream outClient, Scanner scanner) {
	    try {        
	        if (scanner.hasNextLine()) {
	            String userResponse = scanner.nextLine();
	            if ("exit".equals(userResponse)) {
	            	messageHandler.isConnected = false;
	            }
	            else if("/image".equals(userResponse)) { //devrait etre starts with. et ensuite on filtre pour prendre le chemin de l'image.
	            	sendImageToServer(inClient,outClient,scanner);
	            	return;
	            }

	            outClient.writeUTF(userResponse);
	            String responseFromServer = messageHandler.takeMessage();
	            System.out.println(responseFromServer);
	            return; //
	        }
	        
	    } catch (IOException e) {
	        System.out.println("Client: Erreur lors de la communication avec le serveur : " + e.getMessage());
            return;
	    } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



	public static void main(String[] args) {
		try {
			serverIP = Serveur.askForIP();
			serverPort = Serveur.askForPort();
			connectToServer();

		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
}
