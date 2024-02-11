import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler extends Thread {
    private Socket socket;
    private int clientNumber;
    private String username;
    private String password;
    private DataOutputStream output;
    private Serveur serveur; // possede une reference a la classe serveur auquel il appartient pour appeler de ses methodes
	private static boolean isConnected = true;

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

	public static Map<String, String> getDatabase() {
		return database;
	}

	public static boolean isConnected() {
	    return isConnected;
	}

	public static void setConnectedState(boolean disconnect) {
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
	        System.out.println("Erreur lors de la fermeture du socket : " + e.getMessage());
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
                    System.out.println("Format de ligne non valide dans le fichier : " + line);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Le fichier spécifié n'a pas été trouvé : " + filePath);
        } catch (IOException e) {
            System.out.println("Erreur de lecture du fichier : " + filePath);
        }
    }

    
    private static void writeToUserFile(String filePath, String username, String password) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(username.toLowerCase() + "," + password);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Erreur lors de l'écriture dans le fichier : " + e.getMessage());
        }
    }
    private static void writeToMessageFile(String newMessage) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/messages.txt", true))) {
            writer.write(newMessage);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Erreur lors de l'écriture dans le fichier : " + e.getMessage());
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
    
    private void processClientMessage(String message) {
        try {
            if (isValidMessage(message)) {
                if ("exit".equalsIgnoreCase(message.trim())) {
                	serveur.removeClientFromVector(clientNumber);
                    Serveur.setClientNumber(Serveur.getClientNumber() - 1);
                    String response = Serveur.ANSI_GRAY + "Déconnexion réussie." + Serveur.ANSI_WHITE + Serveur.time;
                    setConnectedState(false);
                    System.out.println(Serveur.ANSI_BLUE + username + Serveur.ANSI_GRAY + " s'est déconnecté à " + Serveur.ANSI_WHITE + Serveur.time);
                    output.writeUTF(response);
                    return;
                } else {
                	String formattedMessage = String.format("[%s - %s:%d - %s]: %s", Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE, socket.getInetAddress().getHostAddress(), socket.getPort(), Serveur.time, message);
                    System.out.println(formattedMessage);
                    serveur.addMessageQueue(formattedMessage); //appel de la fonction proteger de la classe serveur pour ajouter le message  dans la QUEUE
                    writeToMessageFile(formattedMessage);
                    serveur.writeToEveryClient(clientNumber ,formattedMessage);
                    String response = Serveur.ANSI_GREEN + "Message délivré " + Serveur.time  + Serveur.ANSI_WHITE;
                    output.writeUTF(response);
                }
            } else {
                String response = Serveur.ANSI_RED + "Veuillez respecter le nombre de caractère" + Serveur.ANSI_WHITE;
                output.writeUTF(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void connectClient(DataInputStream in) throws IOException {
        boolean credentialsValid = false;

        while (!credentialsValid) {
            username = in.readUTF();
            password = in.readUTF();

            if (usernameExist(username)) {
                if (validateUserCredentials(username, password)) {
                    String successMessage = Serveur.ANSI_GREEN + "Connexion réussie pour l'utilisateur " + Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE + Serveur.time;
                    for (String message : Serveur.getMessages()) { //ecriture des 15 derniers messages, pris d<un static data memeber de la classe par un GETTER
                    	successMessage += "\n" + message;
                    }
                    output.writeUTF(successMessage);
                    credentialsValid = true;
                } else {
                    String errorMessage = Serveur.ANSI_RED + "Mot de passe incorrect pour l'utilisateur " + Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE + Serveur.time;
                    output.writeUTF(errorMessage);
                }
            } else {
                createUser(username, password);
                String successMessage = Serveur.ANSI_GREEN + "Création du compte réussie pour l'utilisateur " + Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE + Serveur.time;
                for (String message : Serveur.getMessages()) { //ecriture des 15 derniers messages, pris d<un static data memeber de la classe par un GETTER
                	successMessage += message + "\n";
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
                processClientMessage(clientMessage);                
            }
        } catch (SocketException e) {
        	serveur.removeClientFromVector(clientNumber);
            System.out.println("Connexion interrompue avec le client " + Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE + Serveur.time);
        } catch (EOFException e) {
            System.out.println("Fin de flux atteinte pour le client " + Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE + Serveur.time);
        } catch (IOException e) {
            System.out.println("Erreur I/O avec le client " + Serveur.ANSI_BLUE + username + Serveur.ANSI_WHITE + Serveur.time + ": " + e.getMessage());
        } finally {
            closeSocket();
        }
    }


}
