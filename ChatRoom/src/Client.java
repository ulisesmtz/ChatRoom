import java.awt.BorderLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.DefaultCaret;


/**
 * @author UlisesM
 */
public class Client extends JFrame{

	private JPanel p = new JPanel();           // panel to hold text field and text area
	private JTextField jtf = new JTextField(); // to input radius
	private JTextArea jta = new JTextArea();  // display radius and area from server
	private JButton jb = new JButton("Send");
	private Socket socket;
	private DataInputStream in;
	private DataOutputStream out;
	private final int PORT_NO = 8888;
	
	private String name = "";
	
	public Client() {
		// add gui elements
		p.setLayout(new BorderLayout());
		
		DefaultCaret caret = (DefaultCaret)jta.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); // automatically scroll to bottom
		
		p.add(new JLabel("Enter text"), BorderLayout.WEST);
		p.add(jtf, BorderLayout.CENTER);
		p.add(jb, BorderLayout.EAST);
		
		setLayout(new BorderLayout());
		add(p, BorderLayout.NORTH);
		add(new JScrollPane(jta), BorderLayout.CENTER);
		
		jta.setEditable(false); // client can't edit text area
		
		setTitle("Client");
		setSize(500, 300);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);	
		
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
		
		// try to instantiate socket and input/output stream
		try {
			socket = new Socket("localhost", PORT_NO);
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
		} catch (IOException ioe) {
			JOptionPane.showMessageDialog(this, "Can not connct to server.");
			System.exit(1);
		}
		
		while (true) {
			try {
				String input = "";
				input = in.readUTF();
				if (input.equals("[SUBMITNAME]")) {
					name = JOptionPane.showInputDialog(this, "Enter your name").trim();
					setTitle(name);
					out.writeUTF(name);
				} else {
					jta.append(input + "\n");
				}
			} catch (IOException ioe) {
				//ioe.printStackTrace();
			} catch (NullPointerException he) { 
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
			out.writeUTF(jtf.getText().trim());
			jtf.setText(""); // reset text field
			out.flush();			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	
	public String getName() {
		return name;
	}
	
	public static void main(String[] args) {
		new Client();
	}
 }
