import java.awt.BorderLayout;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

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
	
	private ArrayList<String> names = new ArrayList<String>();
	
	private ArrayList<DataOutputStream> outs = new ArrayList<DataOutputStream>();
	
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
		
		try {
			// create socket with PORT_NO
			ServerSocket serverSocket = new ServerSocket(PORT_NO);
			jta.append("Server started at " + new Date() + "\n"); // display date when server starts
			while (true) {
				// accept all clients and give each their own thread to run
				Socket socket = serverSocket.accept();
				
				ThreadedClient tc = new ThreadedClient(socket);
				new Thread(tc).start();
			}
			
		} catch (IOException ioe) {
			ioe.printStackTrace();
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
				// when name is unique and not empty
				while (true) {
					out.writeUTF("[SUBMITNAME]");
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
				
				// infinite loop, get input and display message
				while (true) {
					String input = in.readUTF();
					if (input == null) // if user entered nothing, do nothing
						return;
					
					jta.append(name + ": " + input + "\n");  // for server logging
 					printToAll(name + ": " + input);
					
				}
				
			} catch (IOException ioe) {
				//ioe.printStackTrace();
			} finally {
				// display that the user has disconnected, remove name and output strea 
				// for that user and clean up
				jta.append(name + " has disconnected\n");
				printToAll(name + " has disconnceted");
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
		 * Broadcast a message to all the users
		 */
		private void printToAll(String msg) {
			try {
				for (DataOutputStream dos : outs) 
					dos.writeUTF(msg);
			} catch (IOException ioe){
				
			}
		}
		
		
		
	}
	
	public static void main(String[] args) {
		new Server();
	}

}
