import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;


/**
 * @author UlisesM
 */
public class Client extends JFrame{
	
	private JPanel panel = new JPanel();           // panel to hold text field and text area
	private Socket socket;
	private DataInputStream in;
	private DataOutputStream out;
	private final int PORT_NO = 8888;
	private String name = "";                // name of client
	private Algorithm alg = new Algorithm(); // to encrypt/decrypt messages
	private final String key = "<6$b^*%2"; // random key for encryption/decryption (match server's key)
	private JTabbedPane tabbedPane = new JTabbedPane();
	private JPanel tabbedPanePanel = new JPanel();
	private List<Chat> chats = new ArrayList<Chat>();     // all chats open in the client
	private List<String> names = new ArrayList<String>(); // all users that are online
	private boolean isDone = false;  // used to check if server is done writing list of names to client

	
	public Client() {
		
		tabbedPanePanel.setLayout(new GridLayout(1,1));
		tabbedPanePanel.add(tabbedPane);

		// add gui elements
		panel.setLayout(new BorderLayout());
		
		setLayout(new BorderLayout());	
		add(tabbedPanePanel, BorderLayout.CENTER);

		setTitle("Client");
		setSize(500, 300);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);	

		
		// test algorithms
		new Algorithm().ECB("a", key, false);
		new Algorithm().ECB("as", key, true);
	
		// try to instantiate socket and input/output stream
		try {
			socket = new Socket("localhost", PORT_NO);
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
		} catch (IOException ioe) {
			JOptionPane.showMessageDialog(this, "Can not connect to server.");
			System.exit(1);
		}
		
		chats.add(new Chat(this, "Global"));
		
		// keep asking for user name until server accepts the unique name
		try {
			String s = in.readUTF();
			while (!s.equals("[ACCEPTED]")) {
				name = JOptionPane.showInputDialog(this, "Enter your name").trim();
				out.writeUTF(name);	
				s = in.readUTF();
			}
			
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Error with server. Try again later.");
			System.exit(1);
		} catch (NullPointerException npe) {
			// close application if user did not enter name
			System.exit(1);
		}
		

		setTitle(name);

		while (true) {
			StyledDocument doc; // holds the styleddocument to append text/image

			try {
				String input = in.readUTF();

				if (input.equals("[PICTURE]")) { // image received
					/*
					 * Make array of bytes and get bytes from image.
					 * Convert array to actual image and display image
					 */
					int length = in.readInt();
					byte[] bytes = new byte[length];
					in.readFully(bytes, 0, length);
					String fromWho = in.readUTF(); // name of client that sent picture
					String tab = in.readUTF();

					doc = getStyledDocument(fromWho, tab);
					
					
					BufferedImage b = ImageIO.read(new ByteArrayInputStream(bytes));
					StyleConstants.setIcon(doc.getStyle("Picture"), new ImageIcon(b));
					
					doc.insertString(doc.getLength(), fromWho + ":\n\t", doc.getStyle("Regular"));
					doc.insertString(doc.getLength(), "ignored", doc.getStyle("Picture"));
					doc.insertString(doc.getLength(), "\n", doc.getStyle("Regular"));
					
				} else if (input.equals("[LIST]")) {
					// get size of array, add all names in list and remove client's own name
					int size = in.readInt();
					names.clear();
					
					for (int i = 0; i < size; i++) 
						names.add(in.readUTF());
					
					names.remove(name); // remove own client's name from list
					isDone = true;
					
				} else { // normal text
					input = decryptMessage(input);
					String fromWho = in.readUTF();
					String tab = in.readUTF();
					
					doc = getStyledDocument(fromWho, tab);
					
					doc.insertString(doc.getLength(), fromWho + ": " + input + "\n", doc.getStyle("Regular"));
				}
			} catch (BadLocationException ble) { // when using insertString
				ble.printStackTrace();
			} catch (IOException ioe) {
				JOptionPane.showMessageDialog(this, "Error with server. Try again later.");
				System.exit(1);
			} 	
		}		
	}
	
	

	/**
	 * @param m the message to be encrypted
	 * @return encrypted String
	 */
	private String encryptMessage(String m) {
		int[] temp = alg.ECB(m, key, false);
		return alg.convertToString(temp);
	}
	
	/**
	 * @param m the message to be decrypted
	 * @return decrypted String
	 */
	private String decryptMessage(String m) {
		int[] temp = alg.ECB(m, key, true);
		return alg.convertToString(temp);
	}
	
	/**
	 * Retrieve the correct styleddocument of the client to append the message
	 * @param fromWho Who sent the message
	 * @param tab the tab name when sender sent the message
	 * @return styleddocument to append the message
	 */
	private StyledDocument getStyledDocument(String fromWho, String tab) {
		if (name.equals(fromWho)) {  // sender is receiving its own copy
			return chats.get(tabbedPane.getSelectedIndex()).getStyledDoc(); 
			
		} else if (tab.equals("Global")) { // get styleddocument from first tab
			return chats.get(0).getStyledDoc();
			
		} else {
			// check to see if tab is already open
			int index = -1;
			int size = tabbedPane.getTabCount();
			for (int i = 0; i < size; i++) {
				if (tabbedPane.getTitleAt(i).equals(fromWho)) {
					index = i;
				}
			}
			
			if (index != -1) { // tab is open
				return chats.get(index).getStyledDoc();
				
			} else { // tab is not open, create new chat and get that styleddocument
				Chat c = new Chat(this, fromWho);
				chats.add(c);
				return c.getStyledDoc();
			}
		}
	}
	
	// getters and setters methods...
	
	public List<String> getNames() {
		return names;
	}
	
	public List<Chat> getChats() {
		return chats;
	}
		
	public boolean getDone() {
		return isDone;
	}
	
	public void setDone(boolean choice) {
		isDone = choice;
	}
	
	public DataOutputStream getDos() {
		return out;
	
	}
	
	public JTabbedPane getTabbedPane() {
		return tabbedPane;
	}
	

		
	public static void main(String[] args) {
		new Client();
	}
 }
