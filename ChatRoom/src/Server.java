import java.awt.BorderLayout;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.Date;

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
			int clientNumber = 0;  // keep track of how many clients there are
			jta.append("Server started at " + new Date() + "\n"); // display date when server starts
			while (true) {
				// accept all clients and give each their own thread to run
				Socket socket = serverSocket.accept();
				
				// display info about client
				jta.append("Starting thread for client " + ++clientNumber + " at " + new Date() + "\n");
				InetAddress ia = socket.getInetAddress();
				jta.append("Client " + clientNumber + " 's host name is " + ia.getHostName() + "\n");
				jta.append("Client " + clientNumber + " 's IP address is " + ia.getHostAddress() + "\n");
				
				ThreadedClient tc = new ThreadedClient(socket, clientNumber);
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
		private int clientNo;
		
		public ThreadedClient(Socket socket, int clientNo) {
			this.socket = socket;
			this.clientNo = clientNo;
		}
		
		@Override
		public void run() {
			DataInputStream in = null;
			DataOutputStream out = null;
			
			try {
				// instantiate input and output stream
				in = new DataInputStream(socket.getInputStream());
				out = new DataOutputStream(socket.getOutputStream());
				
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
				
				while (true) {  
					try {
						double radius = in.readDouble();
						double area = radius * radius * Math.PI;
						area = Double.parseDouble(new DecimalFormat("#0.00").format(area)); // round to 2 decimal places
						out.writeDouble(area);  // write to output stream for client to receive
					
						jta.append("Radius received from client " + clientNo + ": " + radius + 
								"\nArea found: " + area + "\n") ;
					} catch (IOException ioe) { // client has disconnected
						jta.append("Client " + clientNo + " has disconnected at " + new Date() + "\n");
						break;
					}
				}
			
		}
	}
	
	public static void main(String[] args) {
		new Server();
	}

}
