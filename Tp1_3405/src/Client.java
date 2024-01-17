import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static Socket socket;
    private static String username;
    private static String password;

    public boolean verificationPort(int port) {
        return port > 5000 && port < 5050;
    }

    public void verificationipAdresse(char[] ipAddress) {
    	
    }
    

    public static void main(String[] args) {
        try {
            // Adresse et port du serveur
            String serverAddress = "10.200.12.99";
            int port = 5030;

            Scanner usernameScanner = new Scanner(System.in);
			System.out.println("Enter username");
			username = usernameScanner.nextLine();
			
            Scanner passwordScanner = new Scanner(System.in);
			System.out.println("Enter password");
			password = passwordScanner.nextLine();
			
            
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
