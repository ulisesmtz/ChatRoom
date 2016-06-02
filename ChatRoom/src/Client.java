import java.awt.BorderLayout;
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
	private DataInputStream in;
	private DataOutputStream out;
	private final int PORT_NO = 8888;
	
	public Client() {
		// add gui elements
		String name = "";
		name = JOptionPane.showInputDialog("What is your name?");
		
		while (name.equals("Hi")) {
			name = JOptionPane.showInputDialog(name + " is already taken. Input another name");
		}
		
		p.setLayout(new BorderLayout());
		
		DefaultCaret caret = (DefaultCaret)jta.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); // automatically scroll to bottom
		
		p.add(new JLabel("Enter radius"), BorderLayout.WEST);
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
		
		// create socket and initialize output and input stream
		try {
			Socket socket = new Socket("localhost", PORT_NO);
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
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
		
	}
	
	
	/**
	 * @param a the action event
	 * Gets radius from text field, sends it to server and displays area received from server
	 */
	public void performAction(ActionEvent a) {
		try {
			double radius = Double.parseDouble(jtf.getText().trim());
			jtf.setText(""); // reset text field
			out.writeDouble(radius);
			out.flush();
			
			double area = in.readDouble();
			jta.append("Radius is " + radius + 
					"\nArea received from server is " + area + "\n");
			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (NumberFormatException nfe) { // in case input can't be parsed
			jta.append("Invalid entry of: " + jtf.getText().trim() + "\n");
			jtf.setText("");
		}
	}
	
	public static void main(String[] args) {
		new Client();
	}
 }
