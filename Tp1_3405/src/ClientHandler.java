import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

public class ClientHandler extends Thread {
    Socket socket;
    private int clientNumber;
    private String username;
    private String password;
    private DataOutputStream output;
    private Serveur serveur; // possede une reference a la classe serveur auquel il appartient pour appeler de ses methodes
	private boolean isConnected = true;
	private static Map<String, String> database = new HashMap<>();

	public ClientHandler(Socket socket, Serveur serveur, int clientNumber) {
		this.serveur = serveur;
		this.socket = socket;
        this.setClientNumber(clientNumber);
        try {
			output = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        System.out.println(String.format("Nouvelle connexion : client #(%d) sur %s" + Serveur.time, clientNumber, socket));
    }
	public String getClientName() {
		return username;
	}
	public static Map<String, String> getDatabase() {
		return database;
	}

	public boolean isConnected() {
	    return isConnected;
	}

	public void setConnectedState(boolean disconnect) {
	    isConnected = disconnect;
	}
	
	public int getClientNumber() {
		return clientNumber;
	}

	public void setClientNumber(int clientNumber) {
		this.clientNumber = clientNumber;
	}
    
	private void closeSocket() {
	    try {
	        if (socket != null && !socket.isClosed()) {
	            socket.close();
	        }
	    } catch (IOException e) {
	        System.out.println("Erreur ClientHandler: Erreur lors de la fermeture du socket : " + e.getMessage());
	    }
	}
    private boolean isValidMessage(String message) {
        return message != null && message.length() <= 200;
    }
    
    public static void loadUserDatabase(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String username = parts[0].trim();
                    String password = parts[1].trim();
                    database.put(username, password);
                } else {
                    System.out.println("Erreur ClientHandler: Format de ligne non valide dans le fichier : " + line);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Le fichier spécifié n'a pas été trouvé : " + filePath);
        } catch (IOException e) {
            System.out.println("Erreur ClientHandler: Erreur de lecture du fichier : " + filePath);
        }
    }

    
    private static void writeToUserFile(String filePath, String username, String password) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(username.toLowerCase() + "," + password);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Erreur ClientHandler: Erreur lors de l'écriture dans le fichier : " + e.getMessage());
        }
    }
    private static void writeToMessageFile(String newMessage) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/messages.txt", true))) {
            writer.write(newMessage);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Erreur ClientHandler: Erreur lors de l'écriture dans le fichier : " + e.getMessage());
        }
    }


    public static boolean usernameExist(String username) {
        return database.containsKey(username.toLowerCase());
    }

    private static boolean validateUserCredentials(String receivedUsername, String receivedPassword) {
        String storedPassword = database.get(receivedUsername.toLowerCase());
        return storedPassword != null && storedPassword.equals(receivedPassword);
    }

    private static void createUser(String username, String password) {
        database.put(username.toLowerCase(), password);
        writeToUserFile("src/user.txt", username, password);
    }
    
    private void processClientMessage(String message, DataInputStream in) {
        try {
            if (isValidMessage(message)) {
                if ("exit".equalsIgnoreCase(message.trim())) {
                	serveur.removeClientFromVector(clientNumber);
                    String response = Serveur.ANSI_GRAY + "Déconnexion réussie." + Serveur.ANSI_WHITE + Serveur.time;
                    setConnectedState(false);
                    System.out.println(Serveur.ANSI_BLUE + username + Serveur.ANSI_GRAY + " s'est déconnecté à " + Serveur.ANSI_WHITE + Serveur.time);
                    output.writeUTF(response);
                    return;
                } else if("/image".equals(message.trim())) {                	
                	receiveAndProcessImage(in);
                	return;
                } else {
                	String formattedMessage = String.format("[%s - %s:%d - %s]: %s", Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE, socket.getInetAddress().getHostAddress(), socket.getPort(), Serveur.time, message);
                    System.out.println(formattedMessage);
                    serveur.addMessageQueue(formattedMessage);
                    writeToMessageFile(formattedMessage);
                    serveur.writeToEveryClient(clientNumber, username ,formattedMessage);
                    String response = Serveur.ANSI_GREEN + "(Délivré " + Serveur.time  + Serveur.ANSI_WHITE + ")";
                    output.writeUTF(response);
                }
            } else {
                String response = Serveur.ANSI_RED + "Veuillez respecter le nombre de caractère" + Serveur.ANSI_WHITE;
                output.writeUTF(response);
            }
        } catch (IOException e) {
        	System.out.println("Exception ClientHandler: exception attrape dans ProcessClientMessage");
            e.printStackTrace();
        }
    }
    
    private void receiveAndProcessImage(DataInputStream input) {
        try {
            int length = input.readInt();  // Lire la taille de l'image
            byte[] imageData = new byte[length];
            input.readFully(imageData);  // Lire les données de l'image

            // Conversion des données binaires en BufferedImage
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image == null) {
                throw new IOException("Le fichier reçu n'est pas une image valide ou n'a pas pu être décodé.");
            }

            // Appliquer le filtre de Sobel
            BufferedImage processedImage = Sobel.process(image);

            // Logique pour gérer l'image après traitement
            // Par exemple, sauvegarder ou renvoyer l'image, selon les besoins
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(processedImage, "jpg", baos);
            byte[] processedImageData = baos.toByteArray();

            // Exemple d'envoi de l'image traitée en retour au client (si nécessaire)
            output.writeUTF("/imageProcessed");
            output.writeInt(processedImageData.length);
            output.write(processedImageData);
        } catch (IOException e) {
            System.out.println("Erreur lors de la réception ou du traitement de l'image: " + e.getMessage());
        }
    }


    
    private void connectClient(DataInputStream in) throws IOException {
        boolean credentialsValid = false;
        String rulesMessage = "\nInteragissez avec " + Serveur.ANSI_GREEN + "respect" + Serveur.ANSI_WHITE
                + ", envoyez des messages " + Serveur.ANSI_BLUE + "(200 caractères maximum) " + Serveur.ANSI_WHITE
                + ", tapez " + Serveur.ANSI_RED + "'exit'" + Serveur.ANSI_WHITE
                + " pour quitter ou tapez " + Serveur.ANSI_PURPLE + "'/image'" + Serveur.ANSI_WHITE + " pour formater une image : ";

        while (!credentialsValid) {
            username = in.readUTF();
            password = in.readUTF();

            if (usernameExist(username)) {
                if (validateUserCredentials(username, password)) {
                    String successMessage = Serveur.ANSI_GREEN + "Connexion réussie pour " + Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE + Serveur.time + rulesMessage;
                    for (String message : serveur.getMessages()) { //ecriture des 15 derniers messages, pris d<un static data memeber de la classe par un GETTER
                    	successMessage += "\n" + message;
                    }
                    output.writeUTF(successMessage);
                    credentialsValid = true;
                } else {
                    String errorMessage = Serveur.ANSI_RED + "Mot de passe incorrect pour " + Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE + Serveur.time;
                    output.writeUTF(errorMessage);
                }
            } else {
                createUser(username, password);
                String successMessage = Serveur.ANSI_GREEN + "Création du compte réussie pour " + Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE + Serveur.time + rulesMessage;
                for (String message : serveur.getMessages()) { //ecriture des 15 derniers messages, pris d<un static data memeber de la classe par un GETTER
                	successMessage += "\n" + message;
                }
                output.writeUTF(successMessage);                
                credentialsValid = true;
            }
        }
    }

    public DataOutputStream getDataOutputStream() {
    	return output;
    }

    public void run() {
        try (
            DataInputStream in = new DataInputStream(socket.getInputStream());
        	//DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        ) {
            loadUserDatabase("src/user.txt");
            connectClient(in);
            
            while (isConnected()) {
                String clientMessage = in.readUTF();
                processClientMessage(clientMessage, in);                
            }
        } catch (SocketException e) {
        	serveur.removeClientFromVector(clientNumber);
            System.out.println("Exception ClientHandler: Connexion interrompue avec le client " + Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE + Serveur.time);
        } catch (EOFException e) {
            System.out.println("Exception ClientHandler: Fin de flux atteinte pour le client " + Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE + Serveur.time);
        } catch (IOException e) {
            System.out.println("Exception ClientHandler: Erreur I/O avec le client " + Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE + Serveur.time + ": " + e.getMessage());
        } finally {
            closeSocket();
        }
    }


}
