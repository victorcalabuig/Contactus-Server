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
 * Client o de los threads secundarios de los clientes. Cada vez que recibe una
 * conexion, inicia un nuevo thread para gestionar esa conexion y sigue escuchando
 * para nuevas conexiones.
 */
public class Server {
	
	private static int port = 8000;


	public static void main(String[] args) throws IOException, InterruptedException,
			SQLException {
		ServerSocket serverSocket = new ServerSocket(port);
		Connection con = DriverManager.getConnection("jdbc:sqlite:Contactus.db");

		//Creación y lanzamiento del thread que comprueba cada cierto tiempo los contactos
		//cercanos de forma automática
		CloseContactChecker ccChecker = new CloseContactChecker(con);
		(new Thread(ccChecker)).start();

		boolean listen = true;
		while(listen){
			System.out.println("Waiting for client conexions from main server...");
			Socket clientSocket = serverSocket.accept();
			ServerInstance serverInstance = new ServerInstance(clientSocket, con);
			(new Thread(serverInstance)).start(); //Nuevo thread
		}
		serverSocket.close();
	}

}