import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;


/**
 * @author UlisesM
 */
public class Server extends JFrame{
	
	private JTextPane textPane = new JTextPane();
	private final int PORT_NO = 8888;
	
	// to hold all of the names of users online
	private List<String> names = new ArrayList<String>();
	
	// hold every dataoutputstream for broadcasting
	private ArrayList<DataOutputStream> outs = new ArrayList<DataOutputStream>();
	
	private ServerSocket serverSocket = null;
	
	private Algorithm alg = new Algorithm(); // to decrypt/encrypt messages
	private final String key = "<6$b^*%2"; // random key for encryption/decryption (match client's key)
	
	private StyledDocument doc = (StyledDocument) textPane.getDocument();

		
	public Server() {
		// set up gui components
		setLayout(new BorderLayout());
		
		DefaultCaret caret = (DefaultCaret)textPane.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); // automatically scroll to bottom
		 
		add(new JScrollPane(textPane), BorderLayout.CENTER);
		textPane.setEditable(false); // user can't edit info
		setTitle("Server");
		setSize(500, 300);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		
		doc.addStyle("Regular", null);	
		doc.addStyle("Picture", null);
		
		// test algorithm
		new Algorithm().ECB("HI", key, false);
		new Algorithm().ECB("BYE", key, true);

		
		try {
			// create socket with PORT_NO
			serverSocket = new ServerSocket(PORT_NO);
			
			doc.insertString(doc.getLength(), "Server started at " + new Date() + "\n", doc.getStyle("Regular"));
			
			while (true) {
				// accept all clients and give each their own thread to run
				Socket socket = serverSocket.accept();
				
				ThreadedClient tc = new ThreadedClient(socket);
				new Thread(tc).start();
			}
			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (BadLocationException ble) {
			ble.printStackTrace();
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
		private DataInputStream in;
		private DataOutputStream out;
		public ThreadedClient(Socket socket) {
			this.socket = socket;
		}
		
		@Override
		public void run() {			
			try {
				// instantiate input and output stream
				in = new DataInputStream(socket.getInputStream());
				out = new DataOutputStream(socket.getOutputStream());
				
				// keep looping, trying to get a name from user and break from loop
				// when name is unique and not empty (not encrypted)
				while (true) {
					out.writeUTF("[NAME]");
					name = in.readUTF();
					if (name != null && !name.isEmpty() && !name.equals("Global")) {
						synchronized (names) {  // thread safe
							if (!names.contains(name)) {
								names.add(name);
								out.writeUTF("[ACCEPTED]");
								break;
							}
						}
					}
				}
				
				doc.insertString(doc.getLength(), name + " has connected at " + new Date( )+ "\n", doc.getStyle("Regular"));
				outs.add(out);
				
				// infinite loop, get input, decrypt, and display message 
				while (true) {
					String toWho = in.readUTF(); // recipient
					String input = in.readUTF(); // what command (eg [PICTURE], [LIST])
					
					if (input == null) // if user entered nothing, do nothing
						return;
					
					if (input.equals("[PICTURE]")) {
						// read bytes and store in array
						int length = in.readInt();
						byte[] bytes = new byte[length];
						in.readFully(bytes, 0, length);

						// display image in server
						BufferedImage b = ImageIO.read(new ByteArrayInputStream(bytes));
						StyleConstants.setIcon(doc.getStyle("Picture"), new ImageIcon(b));
						
						doc.insertString(doc.getLength(), name + ":\n\t", doc.getStyle("Regular"));
						doc.insertString(doc.getLength(), "ignored", doc.getStyle("Picture"));
						doc.insertString(doc.getLength(), "\n", doc.getStyle("Regular"));
						
						if (toWho.equals("Global")) { 
							// send to everyone
							for (DataOutputStream d : outs) 
								sendPicture(d, length, bytes, toWho);
							
						} else {
							// send to sender and recipient
							sendPicture(out, length, bytes, toWho);
							
							// get datatoutputstream of recipient
							int index = names.indexOf(toWho);
							DataOutputStream d = outs.get(index);
							
							sendPicture(d, length, bytes, toWho);
							
						}
					} else if (input.equals("[LIST]")) {
						// write all names in list of names
						out.writeUTF("[LIST]");
						out.writeInt(names.size());
						for (String n : names) 
							out.writeUTF(n);
						
					} else {
						// normal text
						
						input = decryptMessage(input);
						
						doc.insertString(doc.getLength(), name + ": " + input + "\n", doc.getStyle("Regular"));
					
						if (toWho.equals("Global")) {
							for (DataOutputStream d : outs) 
								sendText(d, input, toWho);
							
						} else {
							// send to sender and recipient
							sendText(out, input, toWho);
							
							// get dataoutputstream of recipient
							int index = names.indexOf(toWho);
							DataOutputStream d = outs.get(index);
							
							sendText(d, input, toWho);
						}
					}
					
					
				}
				
			} catch (IOException ioe) {
				//ioe.printStackTrace();
			} catch (BadLocationException ble) {
				ble.printStackTrace();
			} finally {
				// display that the user has disconnected, remove name and output stream 
				// for that user and clean up
				if (name != null) { // in case user does not enter name and exits JOptionPane
					try {
						doc.insertString(doc.getLength(), name + " has disconnected at " + new Date() + "\n", doc.getStyle("Regular"));
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
					names.remove(name);
					outs.remove(out);
				}
				
				try {
					out.close();
					socket.close();
				} catch (IOException ioe) {
					// nothing we can do here
				}
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
		
		/**
		 * Sends image to client
		 * @param d dataoutputstream to write picture to
		 * @param length length of array bytes
		 * @param bytes array that holds the picture in byte format
		 * @param toWho recipient of image
		 */
		private void sendPicture(DataOutputStream d, int length, byte[] bytes, String toWho) {
			try {
				d.writeUTF("[PICTURE]");
				d.writeInt(length);
				d.write(bytes, 0, length);
				d.writeUTF(name);
				d.writeUTF(toWho);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		/**
		 * Sends text to client
		 * @param d dataoutputstream to write text to
		 * @param input the message
		 * @param toWho recipient of text
		 */
		private void sendText(DataOutputStream d, String input, String toWho) {
			try {
				d.writeUTF(encryptMessage(input));
				d.writeUTF(name);
				d.writeUTF(toWho);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	
	}
	
	public static void main(String[] args) {
		new Server();
	}

}