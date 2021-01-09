import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class login extends JFrame implements ActionListener {

  Container container = getContentPane();
  JLabel userLabel = new JLabel("USERNAME");
  JLabel passwordLabel = new JLabel("PASSWORD");
  JTextField userTextField = new JTextField();
  JPasswordField passwordField = new JPasswordField();
  JButton loginButton = new JButton("LOGIN");
  JButton resetButton = new JButton("RESET");
  JCheckBox showPassword = new JCheckBox("Show Password");


  login() {
    setLayoutManager();
    setLocationAndSize();
    addComponentsToContainer();
    addActionEvent();

  }

  /**
   * Estos métodos que verás a continuación son los de diseño.
   */
  public void setLayoutManager() {
    container.setLayout(null);
  }

  public void setLocationAndSize() {
    userLabel.setBounds(50, 150, 100, 30);
    passwordLabel.setBounds(50, 220, 100, 30);
    userTextField.setBounds(150, 150, 150, 30);
    passwordField.setBounds(150, 220, 150, 30);
    showPassword.setBounds(150, 250, 150, 30);
    loginButton.setBounds(50, 300, 100, 30);
    resetButton.setBounds(200, 300, 100, 30);


  }

  /**
   * La clase addComponentsToContainer inserta los elementos a partir del panel creado inicialmente
   */
  public void addComponentsToContainer() {
    container.add(userLabel);
    container.add(passwordLabel);
    container.add(userTextField);
    container.add(passwordField);
    container.add(showPassword);
    container.add(loginButton);
    container.add(resetButton);
  }

  /**
   * Se produce la acción del evento al que corresponde en este caso los dos botones y el checkBox
   */
  public void addActionEvent() {
    loginButton.addActionListener(this);
    resetButton.addActionListener(this);
    showPassword.addActionListener(this);
  }

  /**
   * El metodo mas interesante lo que realiza es la acción, aquí deberiamos de vereficar que el login se ha realizado correctamente,
   * el problema es que desconozco la forma en la que se puede hacer todavía.
   * @param e
   */

  @Override
  public void actionPerformed(ActionEvent e) {
    //Coding Part of LOGIN button
    if (e.getSource() == loginButton) {
      String userText;
      String pwdText;
      userText = userTextField.getText();
      pwdText = passwordField.getText();
      if (userText.equalsIgnoreCase("mehtab") && pwdText.equalsIgnoreCase("12345")) {
        JOptionPane.showMessageDialog(this, "Login Successful");
      } else {
        JOptionPane.showMessageDialog(this, "Invalid Username or Password");
      }

    }
    //Coding Part of RESET button
    if (e.getSource() == resetButton) {
      userTextField.setText("");
      passwordField.setText("");
    }
    //Coding Part of showPassword JCheckBox
    if (e.getSource() == showPassword) {
      if (showPassword.isSelected()) {
        passwordField.setEchoChar((char) 0);
      } else {
        passwordField.setEchoChar('*');
      }
      
    }
  }

}

