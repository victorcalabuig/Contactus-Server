import javax.swing.*;

public class main {
  public static void main (String[] args) {
      login frame = new login();
      frame.setTitle("Login Form");
      frame.setVisible(true);
      frame.setBounds(10, 10, 370, 600);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setResizable(false);        
  }
  
}
