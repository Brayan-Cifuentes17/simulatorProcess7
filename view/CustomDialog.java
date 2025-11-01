package view;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class CustomDialog extends JDialog implements KeyListener {
    
    public static final int WARNING_TYPE = 1;
    public static final int INFO_TYPE = 2;
    public static final int CONFIRM_TYPE = 3;
    
    private JButton defaultButton; 
    
    public CustomDialog(ActionListener listener, String text, int type){
       
            int lines = quantityOfWords(text, "<br>") + 1;
            int baseWidth = Math.max(500, text.length() * 10);
            int baseHeight = 140 + (lines * 30);
        
        if(type == CONFIRM_TYPE) {
            baseHeight += 20; 
        }
        
        this.setSize(new Dimension(baseWidth, baseHeight));
        this.setLocationRelativeTo(null);
        this.setLayout(new GridBagLayout());
        this.setUndecorated(true);
        this.setAlwaysOnTop(true);
        this.setModal(true);
        
       
        this.addKeyListener(this);
        this.setFocusable(true);
        
        initComponents(listener, text, type);
        this.setVisible(true);
        
      
        this.requestFocus();
    }
    
    public static int quantityOfWords(String text, String word) {
        String textCopy = ""+text;
        String[] words = textCopy.split("\\s+");
        int counter = 0;
        for (String p : words) {
            if (p.equalsIgnoreCase(word)) {
                counter++;
            }
        }
        return counter;
    }
    
    public void initComponents(ActionListener listener, String text, int type){
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(this.getSize());
        panel.setBackground(new Color(44, 62, 80));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15); 
        
        
        JLabel label = new JLabel("<html><center><font size='5' color='white'>" + text + "</font></center></html>");
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        label.setForeground(Color.WHITE);
        label.setHorizontalAlignment(JLabel.CENTER);
        panel.add(label, gbc);
        gbc.gridy = 1;
        
        if(type == CONFIRM_TYPE) {
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            buttonPanel.setBackground(Color.lightGray);
            
            JButton yesButton = new JButton("Si");
            yesButton.addActionListener(listener);
            yesButton.setActionCommand(Constants.CONFIRM_YES);
            yesButton.addKeyListener(this);
            yesButton.setFont(new Font("Arial", Font.PLAIN, 14));
            yesButton.setBackground(Color.WHITE);
            yesButton.setForeground(new Color(44, 62, 80));


            JButton noButton = new JButton("No");
            noButton.addActionListener(listener);
            noButton.setActionCommand(Constants.CONFIRM_NO);
            noButton.addKeyListener(this);
            noButton.setFont(new Font("Arial", Font.PLAIN, 14));
            noButton.setBackground(Color.WHITE);
            noButton.setForeground(new Color(44, 62, 80));
            
            buttonPanel.add(yesButton);
            buttonPanel.add(noButton);
            panel.add(buttonPanel, gbc);
            
            
            defaultButton = yesButton;
            yesButton.requestFocusInWindow();
            
        } else {
            JButton button = new JButton("Aceptar");
            button.addActionListener(listener);
            button.addKeyListener(this);
            button.setFont(new Font("Arial", Font.PLAIN, 14));
            button.setBackground(Color.WHITE);
            button.setForeground(new Color(44, 62, 80));
            
            if(type == WARNING_TYPE) {
                button.setActionCommand(Constants.CLOSE_WARNING);
            } else {
                button.setActionCommand(Constants.CLOSE_INFO);
            }
            panel.add(button, gbc);
            
           
            defaultButton = button;
            button.requestFocusInWindow();
        }
        
        this.add(panel);
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            
            if (defaultButton != null) {
                defaultButton.doClick();
            }
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            
            this.dispose();
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
      
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
       
    }
}