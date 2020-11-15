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

/**
* Guarda el username actual para utilizarlo en el prompt.
*/
static private String currentUsername = "";

/**
* Comprueba si el comando login ha sido exitoso. En caso de exito, actualiza
* el currentUserId y el currentUsername, abriendo así una sesión de usuario.
* @param fields Contestación del servidor separada por espacios a la llamada
* de login.
*/
private static void processLoginResult(String[] fields){
	if(Integer.parseInt(fields[1]) == 0 && fields.length == 4){
		currentUserId = Integer.valueOf(fields[2]);
		currentUsername = fields[3];
	}
}

private static void processListUsersResult(String[] fields){
	if(Integer.parseInt(fields[1]) == 0){
		for(int i = 2; i < fields.length; i++)
			System.out.print(fields[i] + " ");
		System.out.println();
	}
}



public static void main(String[] args) throws IOException {
	Scanner keyboard = new Scanner(System.in);
	Socket clientSocket = new Socket("localhost", 8000);

	PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
	BufferedReader in = new BufferedReader(
		new InputStreamReader(clientSocket.getInputStream()));

	boolean execute = true;
	while(execute){
		System.out.print(currentUsername + "> ");
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
	    //Formato de mensaje recibido: "comando_ejecutado resultado info1 info2 ...."
	    String[] fields = res.split(" ");
	    if(fields.length > 0){
	    	switch(fields[0]){
	    		case "login": 
	    			processLoginResult(fields);
	    			break;
	    		case "listUsers": 
	    			processListUsersResult(fields);
	    			break;
	    		case "exit":
	    			execute = false;
	    	}
	    }
	}
	    
	clientSocket.close();
}

}