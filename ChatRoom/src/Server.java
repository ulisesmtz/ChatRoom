import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;


/**
 * @author UlisesM
 */
public class Server extends JFrame{
	
	private JTextArea jta = new JTextArea();
	private final int PORT_NO = 8888;
	
	// to hold all of the names of users online
	private ArrayList<String> names = new ArrayList<String>();
	
	// hold every dataoutputstream for broadcasting
	private ArrayList<DataOutputStream> outs = new ArrayList<DataOutputStream>();
	
	private ServerSocket serverSocket = null;
	
	private Algorithm alg = new Algorithm(); // to decrypt/encrypt messages
	private final String key = "<6$b^*%2"; // random key for encryption/decryption (match client's key)
	
	public Server() {
		// set up gui components
		setLayout(new BorderLayout());
		
		DefaultCaret caret = (DefaultCaret)jta.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); // automatically scroll to bottom
		 
		add(new JScrollPane(jta), BorderLayout.CENTER);
		jta.setEditable(false); // user can't edit info
		setTitle("Server");
		setSize(500, 300);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		
		// test algorithm
		new Algorithm().ECB("HI", key, false);
		new Algorithm().ECB("BYE", key, true);

		
		try {
			// create socket with PORT_NO
			serverSocket = new ServerSocket(PORT_NO);
			jta.append("Server started at " + new Date() + "\n"); // display date when server starts
			while (true) {
				// accept all clients and give each their own thread to run
				Socket socket = serverSocket.accept();
				
				ThreadedClient tc = new ThreadedClient(socket);
				new Thread(tc).start();
			}
			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			try {
				serverSocket.close();
			} catch (IOException e) {
				// nothing we can do here
			}
		}
	}
	
	/**
	 * @author UlisesM
	 * Gives each client an own thread to operate.
	 */
	class ThreadedClient extends Thread {
		private Socket socket;
		private String name;
		
		public ThreadedClient(Socket socket) {
			this.socket = socket;
		}
		
		@Override
		public void run() {
			DataInputStream in = null;
			DataOutputStream out = null;
			
			try {
				// instantiate input and output stream
				in = new DataInputStream(socket.getInputStream());
				out = new DataOutputStream(socket.getOutputStream());
				
				// keep looping, trying to get a name from user and break from loop
				// when name is unique and not empty (not encrypted)
				while (true) {
					out.writeUTF("[NAME]");
					name = in.readUTF();
					if (name != null && !name.isEmpty()) {
						synchronized (names) {  // thread safe
							if (!names.contains(name)) {
								names.add(name);
								break;
							}
						}
					}
				}
				
				jta.append(name + " has connected at " + new Date() + "\n");
				outs.add(out);
				printToAll(name + " has connected at " + new Date());
				
				// infinite loop, get input, decrypt, and display message 
				while (true) {
					String input = in.readUTF();
					if (input == null) // if user entered nothing, do nothing
						return;
					
					if (input.equals("[PICTURE]")) {
						int length = in.readInt();
						byte[] bytes = new byte[length];
						in.readFully(bytes, 0, length);
//						for (byte b : bytes) {
//							System.out.print(b + " ");
//						}
						jta.append("image received from " + name + "\n");
						for (DataOutputStream d : outs) {
							d.writeUTF("[PICTURE]");
							d.writeInt(length);
							d.write(bytes, 0, length);
						}
					} else {
						input = decryptMessage(input);
						
						jta.append(name + ": " + input + "\n");  // for server logging
	 					printToAll(name + ": " + input);
					}
					
					
				}
				
			} catch (IOException ioe) {
				//ioe.printStackTrace();
			} finally {
				// display that the user has disconnected, remove name and output stream 
				// for that user and clean up
				if (name != null) { // in case user does not enter name and exits JOptionPane
					jta.append(name + " has disconnected\n");
					printToAll(name + " has disconnceted");
				}
				names.remove(name);
				outs.remove(out);
				try {
					out.close();
					socket.close();
				} catch (IOException ioe) {
					// nothing we can do here
				}
			}
				
			
			
		}
		
		/**
		 * @param msg the message to display to all users
		 * Broadcast encrypted message to all the users
		 */
		private void printToAll(String msg) {
			try {
				for (DataOutputStream dos : outs) 
					dos.writeUTF(encryptMessage(msg));
			} catch (IOException ioe){
				
			}
		}
		
		/**
		 * @param m the message to be decrypted
		 * @return decrypted String using ECB
		 */
		private String decryptMessage(String m) {
			int c[] = alg.ECB(m, key, true);
			return alg.convertToString(c);
		}
		
		/**
		 * @param m the message to be encrypted
		 * @return encrypted String using ECB
		 */
		private String encryptMessage(String m) {
			int c[] = alg.ECB(m, key, false);
			return alg.convertToString(c);
		}
		
		
		
	}
	
	public static void main(String[] args) {
		new Server();
	}

}
