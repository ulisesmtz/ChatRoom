import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;


/**
 * @author UlisesM
 */
public class Client extends JFrame{

	private JPanel p = new JPanel();           // panel to hold text field and text area
	private Socket socket;
	private DataInputStream in;
	private DataOutputStream out;
	private final int PORT_NO = 8888;
	private String name = "";                // name of client
	private Algorithm alg = new Algorithm(); // to encrypt/decrypt messages
	private final String key = "<6$b^*%2"; // random key for encryption/decryption (match server's key)
	private final int MAX_WIDTH = 200, MAX_HEIGHT = 200; // max size of image after resizing
	private JTabbedPane tabbedPane = new JTabbedPane();
	private JPanel tabbedPanePanel = new JPanel();
	private int currentTab = 0;
	private List<Chat> chats = new ArrayList<Chat>();
	private List<String> names = new ArrayList<String>();
	
	List<String> getNames() {
		return names;
	}
	
	List<Chat> getChats() {
		return chats;
	}
	
	
	
	private boolean isDone = false;
	
	boolean getDone() {
		return isDone;
	}
	
	void setDone(boolean choice) {
		isDone = choice;
	}
	
	DataOutputStream getDos() {
		return out;
	
	}
	
	JTabbedPane getTabbedPane() {
		return tabbedPane;
	}
	public Client() {
		
		tabbedPanePanel.setLayout(new GridLayout(1,1));
		tabbedPanePanel.add(tabbedPane);

		// add gui elements
		p.setLayout(new BorderLayout());
		
		setLayout(new BorderLayout());	
		add(tabbedPanePanel, BorderLayout.CENTER);

		setTitle("Client");
		setSize(500, 300);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);	

		
		// test algorithms
		new Algorithm().ECB("a", key, false);
		new Algorithm().ECB("as", key, true);
		
		tabbedPane.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				currentTab = tabbedPane.getSelectedIndex();
				System.out.println(currentTab);
			}
			
		});
		
	
		// try to instantiate socket and input/output stream
		try {
			socket = new Socket("localhost", PORT_NO);
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
		} catch (IOException ioe) {
			JOptionPane.showMessageDialog(this, "Can not connect to server.");
			System.exit(1);
		}
		
		//chats.add(new Chat(tabbedPane, "Global", out, this));
		chats.add(new Chat(this, "Global"));
		
		while (true) {
			try {
				String input = in.readUTF();
				//TODO: fix line under
				StyledDocument doc = chats.get(currentTab).getStyledDoc();
				if (input.equals("[NAME]")) {
					name = JOptionPane.showInputDialog(this, "Enter your name").trim();
					setTitle(name);
					out.writeUTF(name);
				} else if (input.equals("[PICTURE]")) { // image received
					/*
					 * Make array of bytes and get bytes from image.
					 * Convert array to actual image and display image
					 */
					int length = in.readInt();
					byte[] bytes = new byte[length];
					in.readFully(bytes, 0, length);
					String _name = in.readUTF(); // name of client that sent picture
					BufferedImage b = ImageIO.read(new ByteArrayInputStream(bytes));
					StyleConstants.setIcon(doc.getStyle("Picture"), new ImageIcon(b));
					
					doc.insertString(doc.getLength(), _name + ":\n\t", doc.getStyle("Regular"));
					doc.insertString(doc.getLength(), "ignored", doc.getStyle("Picture"));
					doc.insertString(doc.getLength(), "\n", doc.getStyle("Regular"));
					
				} else if (input.equals("[LIST]")) {
					int size = in.readInt();
					names.clear();
					
					for (int i = 0; i < size; i++) {
						names.add(in.readUTF());
					}
					names.remove(name); // remove own client's name from list
					isDone = true;
					
				} else { // normal text
					doc.insertString(doc.getLength(), decryptMessage(input) + "\n", doc.getStyle("Regular"));
				}
			} catch (BadLocationException ble) { // when using insertString
				ble.printStackTrace();
			} catch (IOException ioe) {
				JOptionPane.showMessageDialog(this, "Error with server. Try again later.");
				System.exit(1);
			} catch (NullPointerException npe) { 
				// close application if user did not enter name
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
	

		
	public static void main(String[] args) {
		new Client();
	}
 }

