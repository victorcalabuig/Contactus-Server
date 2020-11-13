package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.lang.Thread.sleep;
import java.net.ServerSocket;
import java.net.Socket;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;


public class Server {

/**
* Añade un usuario a la base de datos si 'username' está disponible
* @param username Nombre de usuario
* @param pwd Contraseña de acceso
* @param stmt Statement utilizado para conectarse a la base de datos.
* @return 0 si añade el usuario correctamente, 1 si el usuario ya existía.
*/
private static int addUser(String username, String pwd, Statement stmt) 
	throws SQLException {
		//Comprobar si el usuario ya existe
		ResultSet rs = stmt.executeQuery(
			"SELECT userId FROM User WHERE username = '" + username + "'");
		if(rs.next()) 
			return 1;

		String insUser = String.format(
			"INSERT INTO User (username, password) VALUES ('%s', '%s')", username, pwd);
		stmt.executeUpdate(insUser);
		return 0;
}

public static void main(String[] args) throws IOException, InterruptedException,
	SQLException {

		Connection con = DriverManager.getConnection("jdbc:sqlite:Contactus.db"); 
		Statement stmt = con.createStatement();

	    ServerSocket serverSocket = new ServerSocket(8000);
	    System.out.println("Waiting for client conexions...");
	    Socket clientSocket = serverSocket.accept();
	    
	    BufferedReader in = new BufferedReader(
	        new InputStreamReader(clientSocket.getInputStream()));
	    PrintWriter out = new PrintWriter(
	        clientSocket.getOutputStream(), true);
	    
	    while(true){
	        String msgReceived = in.readLine();
	        if(msgReceived.equals("close")) break;
	        //out.println(rec); //Reenvío del mensaje recibido al cliente
	        //Implementar lógica aquí
	    }
	    
	    con.close();
	    clientSocket.close();
	    serverSocket.close();
}

}