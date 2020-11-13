package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

/**
* Guarda el Id del usuario actual, y es 0 hasta que se inicie sesión con el 
* comando Login. Sirve para indicarle al servidor qué usuario hay detrás del 
* cliente.
*/
static private int currentUserId = 0;

public static void main(String[] args) throws IOException {
	Scanner keyboard = new Scanner(System.in);
	Socket clientSocket = new Socket("localhost", 8000);

	PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
	BufferedReader in = new BufferedReader(
		new InputStreamReader(clientSocket.getInputStream()));

	boolean execute = true;
	while(execute){
		String keyboardInput = keyboard.nextLine(); 
		//Se añade como cabecera el Id del usuario actual
		String msgToServer = currentUserId + " " + keyboardInput; 

		//Envio de mensaje al servidor
	    out.println(msgToServer); //
	    out.flush();

	    //Mecanismo temporal para cerrar la conexión
	    if(keyboardInput.equals("close")) break; 

	    //Resultado que nos envía el servidor
	    String res = in.readLine();
	    System.out.println("server response: " + res); //Comprobación

	    //Implementar lógica tras contestación

	    if(Integer.parseInt(res) == 3) execute = false;
	}
	    
	clientSocket.close();
}

}