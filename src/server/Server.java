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

import utils.Code;


public class Server {


/**
* Añade un usuario a la base de datos si 'username' está disponible
* @param username Nombre de usuario
* @param pwd Contraseña de acceso
* @param stmt Statement utilizado para conectarse a la base de datos.
* @return 0 si añade el usuario correctamente, -41 si el usuario ya existía.
*/
private static int addUser(String username, String pwd, Statement stmt) 
	throws SQLException {
		//Comprobar si el usuario ya existe
		ResultSet rs = stmt.executeQuery(
			"SELECT userId FROM User WHERE username = '" + username + "'");
		if(rs.next()) 
			return -41; 

		String insUser = String.format(
			"INSERT INTO User (username, password) VALUES ('%s', '%s')", username, pwd);
		stmt.executeUpdate(insUser);
		return 0;
}

/**
* Método que envuelve a addUser y comprueba que el número de parametros es 4 antes
* de ejecutar el método addUser principal.
* @param fields Array de Strings que contiene el mensaje recibido del cliente separado
* por espacios.
* @param stmt Statement utilizado para conectarse a la base de datos.
* @return Si fields contiene 4 elementos, devolverá el resultado del método addUser 
* principal (0 o -41), sino, devuelve -42 (consultar utils.Code)
*/
private static int addUser(String[] fields, Statement stmt) throws SQLException {
	if(fields.length == 4) {
		return addUser(fields[2], fields[3], stmt);
	}
	return -42;
}


private static int login(String username, String pwd, Statement stmt) throws SQLException {
	String queryUserId = String.format(
		"SELECT userId FROM User WHERE username = '%s' AND password = '%s'", username, pwd);
	ResultSet userIdRS = stmt.executeQuery(queryUserId);
	if(userIdRS.next()) { 
		return 0;	
	}
	return -44; //auth fail
}

private static int login(String[] fields, Statement stmt) throws SQLException {
	if(fields.length != 4) return -42;
	if(Integer.parseInt(fields[0]) != 0) return -43; //User already logged in
	return login(fields[2], fields[3], stmt);
}

private static int getUserId(String username, Statement stmt) throws SQLException {
	String queryUserId = String.format(
		"SELECT userId FROM User WHERE username = '%s'", username);
	ResultSet userIdRS = stmt.executeQuery(queryUserId);
	if(userIdRS.next()){ 
		return userIdRS.getInt(1);
	}
	return -45;
}


public static void main(String[] args) throws IOException, InterruptedException,
	SQLException {

		Connection con = DriverManager.getConnection("jdbc:sqlite:Contactus.db"); 
		Statement stmt = con.createStatement();

	    ServerSocket serverSocket = new ServerSocket(8000);
	    System.out.println("Waiting for client conexions...");
	    Socket clientSocket = serverSocket.accept();
	    System.out.println("Conection established!!!");
	    
	    BufferedReader in = new BufferedReader(
	        new InputStreamReader(clientSocket.getInputStream()));
	    PrintWriter out = new PrintWriter(
	        clientSocket.getOutputStream(), true);
	    
	    boolean execute = true;
	    while(execute){
	    	//Mensaje recibido del cliente (lo dividimos por palabras con split):
	        String msgReceived = in.readLine();
	        String[] fields = msgReceived.split(" ");

	        //if(msgReceived.equals("close")) break; //temporal

	        //Implementar lógica aquí
	        int res = -1; //resultado del comando
	        String info1 = ""; //Informacion adcional para contestar al cliente
	        String info2 = "";
	        if(fields.length > 1){
	        	switch(fields[1]) {
	        		case "addUser": 
	        			res = addUser(fields, stmt);
	        			break;
	        		case "login": 
	        			res = login(fields, stmt);
	        			if(res == 0) {
	        				info1 = Integer.toString(getUserId(fields[2], stmt)); 
	        				info2 = fields[2]; //devolvemos tambien el username
	        			}
	        			break;
	        		case "exit": 
	        			res = -3;
	        			execute = false;
	        			break;

	        		default: break;
	        	}
	        }
	        
	        //Envíamos al cliente el resultado de su petición
	        String command = (res == -1) ? "unrecognized" : fields[1];
	        out.println(command + " " + res + " " + info1 + " " + info2); 	        
	    }
	    
	    con.close();
	    clientSocket.close();
	    serverSocket.close();
}

}