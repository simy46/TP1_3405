import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.imageio.ImageIO;

public class MessageHandler extends Thread {
	//private File outputfile;
	private Socket socket;
	public BlockingQueue<String> messages = new LinkedBlockingQueue<>();
	private DataInputStream inClient;
	boolean isConnected = true;

	public MessageHandler(Socket socket, DataInputStream inClient) {
		this.socket = socket;
		this.inClient = inClient;
	}

	public void processMessages(String message) {
		try {
			messages.put(message);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			System.out.println("MessageHanlder: Thread was interrupted, Failed to add message to the queue");
		}
	}

	public void handleMessage(DataInputStream in) throws InterruptedException {
		String message = takeMessage();
		if("/imageProcessed".equals(message)) {
			processImage(in);
		} else if (message.startsWith("[") && message.contains("]: ")) {
			System.out.println(message);
		} else if ("/terminate".equals(message)){
			isConnected = false;
		}else {
			processMessages(message); //sera depiler plus tard
		}
	}


	public void processImage(DataInputStream input) {
	    try {
	        int length = input.readInt();
	        byte[] imageData = new byte[length];
	        input.readFully(imageData);
	        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
	        if (image == null) {
	            throw new IOException("Le fichier reçu n'est pas une image valide ou n'a pas pu être décodé.");
	        }
	        File outputfile = new File("src/image_retour.jpg");
	        ImageIO.write(image, "jpg", outputfile);
	        System.out.println("Image sauvegardée avec succès à : " + outputfile.getAbsolutePath());
	        return;

	    } catch (IOException e) {
	        System.out.println("Erreur lors de la réception, conversion ou sauvegarde de l'image: " + e.getMessage());
	    }
	}


	public String takeMessage() throws InterruptedException {
		return messages.take();
	}

	public void run() {
		while(isConnected) {
			try {
				if( socket.isConnected()) {
					processMessages(inClient.readUTF());
					if (messages.size() > 0)
						handleMessage(inClient);
				}
			} catch (EOFException e) {
				System.out.println("Message Handler: Connection was terminated unexpectedly.");
				break;
			} catch (SocketException e) {
				System.out.println("Messaage Handler: erreur du socket, fermeture du handler.");
				break;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		/*try {
			if (inClient != null) inClient.close();
			if (socket != null && !socket.isClosed()) socket.close();
		} catch (IOException e) {
			System.out.println("Message Handler: Error closing socket or data streams");	
		}*/
	}
}

