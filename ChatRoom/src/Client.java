import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
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
	private JTextField jtf = new JTextField(); // to input message
	private JTextPane jta = new JTextPane();  
	private JButton jb = new JButton("Send");
	private JButton picButton = new JButton("Send a pic!");
	private Socket socket;
	private DataInputStream in;
	private DataOutputStream out;
	private final int PORT_NO = 8888;
	private String name = "";                // name of client
	private Algorithm alg = new Algorithm(); // to encrypt/decrypt messages
	private final String key = "<6$b^*%2"; // random key for encryption/decryption (match server's key)
	
	public Client() {
		// add gui elements
		p.setLayout(new BorderLayout());
		DefaultCaret caret = (DefaultCaret)jta.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); // automatically scroll to bottom
		
		p.add(new JLabel("Enter text"), BorderLayout.WEST);
		p.add(jtf, BorderLayout.CENTER);
		p.add(jb, BorderLayout.EAST);
		p.add(picButton, BorderLayout.AFTER_LAST_LINE);
		
		setLayout(new BorderLayout());
		add(p, BorderLayout.SOUTH);
		add(new JScrollPane(jta), BorderLayout.CENTER);
		
		jta.setEditable(false); // client can't edit textpane
		

		setTitle("Client");
		setSize(500, 300);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);	
		
		
		//TODO: TEST using styledcdoc
		final StyledDocument doc = (StyledDocument) jta.getDocument();
		final Style style = doc.addStyle("Name", null);	
		final Style style2 = doc.addStyle("Style", null);
		
		
		
		// test algorithms
		new Algorithm().ECB("a", key, false);
		new Algorithm().ECB("as", key, true);
		
		// add action listener to text field and button and call same method
		jtf.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				performAction(arg0);
			}
			
		});
		
		jb.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				performAction(arg0);
			}
			
		});
		
		picButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser jfc = new JFileChooser();
				int result = jfc.showOpenDialog(p);
				if (result == JFileChooser.APPROVE_OPTION) {
					File file = jfc.getSelectedFile();
					try {
						/*
						 * Get bufferedimage from file, convert it into array
						 * of bytes and send array to server
						 */
						BufferedImage bimg = ImageIO.read(file);
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ImageIO.write(bimg, "png", baos);
						byte[] bytes = baos.toByteArray();
						baos.close();
						out.writeUTF("[PICTURE]");
						out.writeInt(bytes.length);
						out.write(bytes);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
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
		
		while (true) {
			try {
				String input = in.readUTF();
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
					String n = in.readUTF();
					BufferedImage b = ImageIO.read(new ByteArrayInputStream(bytes));
					
					ImageIcon pic = new ImageIcon(b);
					Image im = pic.getImage();
					im = im.getScaledInstance(100, 120, Image.SCALE_SMOOTH);
					pic = new ImageIcon(im);
					StyleConstants.setIcon(style, pic);
					try {
						doc.insertString(doc.getLength(), n + "\n\t", style2);
						doc.insertString(doc.getLength(), "ignored", style);
						doc.insertString(doc.getLength(), "\n", style2);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				} else { // normal text
					try {
						doc.insertString(doc.getLength(), decryptMessage(input) + "\n", style2);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
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
	 * @param a the action event
	 */
	public void performAction(ActionEvent a) {
		try {
			String msg = jtf.getText().trim();
			
			if (!msg.isEmpty()) {
				out.writeUTF(encryptMessage(msg));
				out.flush();	
			}	
			
			jtf.setText(""); // reset text field

		} catch (IOException ioe) {
			ioe.printStackTrace();
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
