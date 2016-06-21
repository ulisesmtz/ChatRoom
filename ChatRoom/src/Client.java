import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
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
	private JTextField jtf = new JTextField(); // to input message
	private JTextPane jtp = new JTextPane();  
	private JButton sendButton = new JButton("Send");
	private JButton picButton = new JButton("Send a pic!");
	private Socket socket;
	private DataInputStream in;
	private DataOutputStream out;
	private final int PORT_NO = 8888;
	private String name = "";                // name of client
	private Algorithm alg = new Algorithm(); // to encrypt/decrypt messages
	private final String key = "<6$b^*%2"; // random key for encryption/decryption (match server's key)
	private final int MAX_WIDTH = 200, MAX_HEIGHT = 200; // max size of image after resizing
	private StyledDocument doc = (StyledDocument) jtp.getDocument();

	
	public Client() {
		// add gui elements
		p.setLayout(new BorderLayout());
		DefaultCaret caret = (DefaultCaret)jtp.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); // automatically scroll to bottom
		
		p.add(new JLabel("Enter text"), BorderLayout.WEST);
		p.add(jtf, BorderLayout.CENTER);
		p.add(sendButton, BorderLayout.EAST);
		p.add(picButton, BorderLayout.AFTER_LAST_LINE);
		
		setLayout(new BorderLayout());
		add(p, BorderLayout.SOUTH);
		add(new JScrollPane(jtp), BorderLayout.CENTER);
		
		jtp.setEditable(false); // client can't edit textpane
		

		setTitle("Client");
		setSize(500, 300);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);	
		
		
		//TODO: TEST using styledcdoc
		doc.addStyle("Regular", null);	
		doc.addStyle("Picture", null);
		
		// test algorithms
		new Algorithm().ECB("a", key, false);
		new Algorithm().ECB("as", key, true);
		
		// add action listener to text field and button and call same method
		jtf.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				sendText(arg0);
			}
			
		});
		
		sendButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				sendText(arg0);
			}
			
		});
		
		picButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser jfc = new JFileChooser();
				FileNameExtensionFilter imageFilter = new FileNameExtensionFilter(
					    "Image files", ImageIO.getReaderFileSuffixes());
				jfc.setFileFilter(imageFilter);
				int result = jfc.showOpenDialog(p);
				if (result == JFileChooser.APPROVE_OPTION) {
					File file = jfc.getSelectedFile();
					try {
						/*
						 * Get bufferedimage from file, convert it into array
						 * of bytes and send array to server
						 */
						BufferedImage bimg = ImageIO.read(file);
						bimg = resize(bimg);
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ImageIO.write(bimg, getFormat(file), baos);
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
					String _name = in.readUTF(); // name of client that sent picture
					BufferedImage b = ImageIO.read(new ByteArrayInputStream(bytes));
					StyleConstants.setIcon(doc.getStyle("Picture"), new ImageIcon(b));
					
					doc.insertString(doc.getLength(), _name + ":\n\t", doc.getStyle("Regular"));
					doc.insertString(doc.getLength(), "ignored", doc.getStyle("Picture"));
					doc.insertString(doc.getLength(), "\n", doc.getStyle("Regular"));
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
	 * @param a the action event
	 */
	public void sendText(ActionEvent a) {
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
	
	/**
	 * Gets the extension of a file
	 * @param file the (image) file to be checked for its extension
	 * @return extension of image (jpg, png, etc)
	 */
	private String getFormat(File file) {
		String name = file.getName().toLowerCase();
		if (name.endsWith(".png"))
			return "png";
		else if (name.endsWith(".jpg")) 
			return "jpg";
		else if (name.endsWith(".bmp"))
			return "bmp";
		else if (name.endsWith(".gif"))
			return "gif";
		else 
			return "Unknown";
	}
	
	/**
	 * Gets dimensions of soon to be resized image
	 * @param bWidth boundary width
	 * @param bHeight boundary height
	 * @param iWidth image width
	 * @param iHeight image height
	 * @return new width and height of image in form of dimension
	 */
	private Dimension getScaledDimension (int bWidth, int bHeight, int iWidth, int iHeight) {
		int newWidth = iWidth;
		int newHeight = iHeight;
		
		if (iWidth > bWidth) {
			newWidth = bWidth;
			newHeight = (newWidth * iHeight) / iWidth;
		}
		
		if (iHeight > bHeight) {
			newHeight = bHeight;
			newWidth = (newHeight * iWidth) / iHeight;
		}
		
		return new Dimension(newWidth, newHeight);
	}
	
	/**
	 * Resizes a bufferedimage while maintaining aspect ratio
	 * @param bimg the bufferedimage to be resized
	 * @return new resized bufferedimage 
	 */
	private BufferedImage resize(BufferedImage bimg) {
		// grab new dimensions
		Dimension d = getScaledDimension(MAX_WIDTH, MAX_HEIGHT, 
				bimg.getWidth(), bimg.getHeight());
		
		// rescale
		Image img = new ImageIcon(bimg).getImage().getScaledInstance( (int)d.getWidth(),
				(int)d.getHeight(), Image.SCALE_SMOOTH);
		
		// convert image to bufferedimage using graphics
		BufferedImage bufferedImage = new BufferedImage(img.getWidth(null), img.getHeight(null),
		        BufferedImage.TYPE_INT_RGB);

	    Graphics g = bufferedImage.createGraphics();
	    g.drawImage(img, 0, 0, null);
	    g.dispose();
	    return bufferedImage;
	}
	
	
	public static void main(String[] args) {
		new Client();
	}
 }
