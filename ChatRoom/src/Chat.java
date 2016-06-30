import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyledDocument;


/**
 * Chat class is responsible for the look of every chat and what to do
 * when a button is pressed. All chats will have the same functionality 
 * except for Global chat where it is not possible to close that tab and
 * every new private chat must be initiated from the Global chat tab.
 * @author UlisesM
 */
public class Chat extends ServerClient{
	
	private JButton privateChatButton = new JButton("New private chat");
	private JButton closeTabButton = new JButton("Close tab");
	private JButton sendPicButton = new JButton("Send a pic");
	private JButton sendButton = new JButton("Send");
	private JTextField textField = new JTextField(); 
	private JTextPane textPane = new JTextPane(); 
	private StyledDocument doc = (StyledDocument) textPane.getDocument();
	private DataOutputStream out;
	private String tabName;
	private JTabbedPane tabbedPane;
	private Client client;

	public Chat(Client client, String tabName) {
		this.client = client;
		this.tabName = tabName;
		out = client.getDos();
		tabbedPane = client.getTabbedPane();

		tabbedPane.addTab(this.tabName, createInnerPanel());
		
		DefaultCaret caret = (DefaultCaret)textPane.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); // automatically scroll to bottom
		
		textPane.setEditable(false);
		
		doc.addStyle("Regular", null);	
		doc.addStyle("Picture", null);
		
		textField.addActionListener(new ActionListener() {

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
		
		sendPicButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser jfc = new JFileChooser();
				FileNameExtensionFilter imageFilter = new FileNameExtensionFilter(
					    "Image files", ImageIO.getReaderFileSuffixes());
				jfc.setFileFilter(imageFilter);
				int result = jfc.showOpenDialog(tabbedPane);
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
						getDos().writeUTF(getTabName());
						getDos().writeUTF("[PICTURE]");
						getDos().writeInt(bytes.length);
						getDos().write(bytes);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
		});
		
		
		privateChatButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					getDos().writeUTF(getTabName());
					getDos().writeUTF("[LIST]");
				} catch (IOException e) {
					e.printStackTrace();
				}
				while (getClient().getDone() == false) {
					// wait until server finishes sending all names to client 
					// and client will set done to true
				}
				

				getClient().setDone(false);

				String choice = (String) JOptionPane.showInputDialog(
				                    tabbedPane,
				                    "With who would you like to start a private chat?",
				                    "Private chat starter",
				                    JOptionPane.PLAIN_MESSAGE,
				                    null,
				                    getClient().getNames().toArray(new String[getClient().getNames().size()]),
				                    "");
				
				if (choice == null) {
					return;
				}
				
				int tab = isTabOpen(choice);
				if (tab != -1) {   // tab exists, so switch to that tab
					tabbedPane.setSelectedIndex(tab);
				} else {  // create new chat and open that tab
					getClient().getChats().add(new Chat(getClient(), choice));
					tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
				}
				
			}
			
		});
		
		closeTabButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// remove chat and tab
				getClient().getChats().remove(tabbedPane.getSelectedIndex());
				tabbedPane.removeTabAt(tabbedPane.getSelectedIndex());
				
			}
			
		});
				
	}
	
	
	/**
	 * Create a new panel with all the buttons and fields for a chat
	 * @return new JPanel that holds the style for every chat
	 */
	private JPanel createInnerPanel() {
		JPanel mainPanel = new JPanel();
		JPanel buttonPanel = new JPanel();
		JPanel jtfPanel = new JPanel();
		
		jtfPanel.setLayout(new BorderLayout());
		mainPanel.setLayout(new BorderLayout());
		buttonPanel.setLayout(new FlowLayout());
		
		buttonPanel.add(privateChatButton);	
		buttonPanel.add(closeTabButton);
		buttonPanel.add(sendPicButton);
		
		// can only create private chat in global tab and can't close global chat tab
		if (tabName.equals("Global"))
			closeTabButton.setEnabled(false);
		else
			privateChatButton.setEnabled(false);
		
		jtfPanel.add(new Label("Enter text"), BorderLayout.WEST);
		jtfPanel.add(textField, BorderLayout.CENTER);
		jtfPanel.add(sendButton, BorderLayout.EAST);

		mainPanel.add(buttonPanel, BorderLayout.NORTH);
		mainPanel.add(jtfPanel, BorderLayout.SOUTH);
		mainPanel.add(new JScrollPane(textPane), BorderLayout.CENTER);	

		return mainPanel;
	}
	
	/**
	 * Sends text in text field to the server
	 * @param a the action event
	 */
	private void sendText(ActionEvent a) {
		try {
			String msg = textField.getText().trim();
			
			if (!msg.isEmpty()) {
				out.writeUTF(getTabName());
				out.writeUTF(encryptMessage(msg));
				out.flush();	
			}	
			
			textField.setText(""); // reset text field

		} catch (IOException ioe) {
			ioe.printStackTrace();
		} 
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
		Dimension d = getScaledDimension(200, 200, 
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
	
	/**
	 * Checks to see if a tab is open 
	 * @param s the title of the tab
	 * @return -1 if tab does not exist, else the index at where the tab exists
	 */
	private int isTabOpen(String s) {
		int size = tabbedPane.getTabCount();
		for (int i = 0; i < size; i++) {
			if (tabbedPane.getTitleAt(i).equals(s))
				return i;
		}
		return -1;
	}
	
	// getter methods...
	
	public String getTabName() {
		return tabName;
	}
	
	public StyledDocument getStyledDoc() {
		return doc;
	}
	
	
	private Client getClient() {
		return client;
	}
	
	private DataOutputStream getDos() {
		return out;
	}

}