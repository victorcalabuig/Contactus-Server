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
import java.sql.Timestamp;

import utils.Code;

/**
* Arranca el servidor y escucha en el puerto indicado conexiones de la clase
* Client o de la clase PositionSender. Cada vez que recibe una conexion, inicia
* un nuevo thread para gestionar esa conexion y sigue escuchando para nuevas 
* conexiones.
*/
public class Server {

private static int port = 8000;


public static void main(String[] args) throws IOException, InterruptedException,
	SQLException {

	    ServerSocket serverSocket = new ServerSocket(port);
	    
	    boolean listen = true;
	    while(listen){
	    	System.out.println("Waiting for client conexions...");
	    	Socket clientSocket = serverSocket.accept();
	    	ServerInstance serverInstance = new ServerInstance(clientSocket);
	    	(new Thread(serverInstance)).start(); //Nuevo thread
	    }

	    serverSocket.close();
}

}