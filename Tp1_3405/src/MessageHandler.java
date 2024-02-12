import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageHandler extends Thread {
	private Socket socket;
	public BlockingQueue<String> messages = new LinkedBlockingQueue<>();
	private DataInputStream inClient;
	boolean isConnected = true;

	public MessageHandler(Socket socket, DataInputStream inClient) {
		this.socket = socket;
		this.inClient = inClient;
		System.out.println("MessageHandler Setup Completed");
	}

	public void processMessages(String message) {
		try {
			messages.put(message);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			System.out.println("MessageHanlder: Thread was interrupted, Failed to add message to the queue");
		}
	}

	public void handleMessage(String message) {
		if (message.startsWith("[") && message.contains("]: ")) {
			System.out.println(message);
		} else {
			processMessages(message);
		}
	}

	public String takeMessage() throws InterruptedException {
		return messages.take();
	}

	public void run() {
		while(isConnected) {
			try {
				if( socket.isConnected()) {
					String msg = inClient.readUTF();
					handleMessage(msg);
				}
			} catch (EOFException e) {
				System.out.println("Message Handler: Connection was terminated unexpectedly.");
				break;
			} catch (SocketException e) {
				System.out.println("Messaage Handler: Socket error, stopping handler.");
				break;
			} catch (IOException e) {
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

