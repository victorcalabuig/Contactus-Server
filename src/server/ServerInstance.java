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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import utils.Code;


/**
 * Las intancias de esta clase se ejecutan en threads secundarios que
 * va lanzando la clase server cada vez que recibe una nueva conexion de un
 * cliente. Lo que se ejecuta está en el metodo run().
 */
public class ServerInstance implements Runnable {

	private static Socket clientSocket;


	public ServerInstance(Socket clientSocket){
		this.clientSocket = clientSocket;
	}

	/**
	 * Utilizado cuando el comando listPositions no suministra un parametro
	 * de tiempo.
	 */
	private static String defaultTimeFrame = "-1y"; //ultimo año


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

		insertUser(username, pwd, stmt);
		return 0;
	}

	private static void insertUser(String username, String pwd, Statement stmt) throws SQLException {
		stmt.executeUpdate(String.format(
				"INSERT INTO User (username, password) VALUES ('%s', '%s')", username, pwd));

		//Now update Healthy table as well (insert user in healthy)
		ResultSet newUserIdRS = stmt.executeQuery(String.format(
				"SELECT userId FROM User WHERE username = '%s'", username));
		newUserIdRS.next();
		int userId = newUserIdRS.getInt(1);
		stmt.executeUpdate(String.format("INSERT INTO Healthy VALUES (%d)", userId));
	}

	private static int removeUser (String username, String pwd, Statement stmt) throws SQLException{
		ResultSet rs = stmt.executeQuery("SELECT username FROM User WHERE username LIKE '" + username + "' AND password LIKE '" + pwd + "'");
		if(!rs.next())
			return 43;

		String remUser = String.format("DELETE FROM User WHERE (username LIKE '%s' AND password LIKE '%s')", username, pwd);
		int usuarioId = getUserId(username, stmt);
		String remLocation = String.format("DELETE FROM Location WHERE (userId = '%s')", usuarioId);

		stmt.executeUpdate(remUser);
		stmt.executeUpdate(remLocation);

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

	private static int removeUser(String[] fields, Statement stmt) throws SQLException {
		if (fields.length == 4) {
			return removeUser(fields[2], fields[3], stmt);
		}

		return 42;
	}


	/**
	 * Autentica una combinacion username-password.
	 */
	private static int login(String username, String pwd, Statement stmt) throws SQLException {
		String queryUserId = String.format(
				"SELECT userId FROM User WHERE username = '%s' AND password = '%s'", username, pwd);
		ResultSet userIdRS = stmt.executeQuery(queryUserId);
		if(userIdRS.next()) {
			return 0;
		}
		return -44; //auth fail
	}

	/**
	 * Método envoltorio del medoto login principal. Comprueba la validez de los
	 * argumentos y que el cliente no haya iniciado sesión previamente.
	 */
	private static int login(String[] fields, Statement stmt) throws SQLException {
		if(fields.length != 4) return -42;
		if(Integer.parseInt(fields[0]) != 0) return -43; //User already logged in
		return login(fields[2], fields[3], stmt);
	}

	/**
	 * Consulta a la base de datos y devuelve el userId de un usuario.
	 */
	private static int getUserId(String username, Statement stmt) throws SQLException {
		String queryUserId = String.format(
				"SELECT userId FROM User WHERE username = '%s'", username);
		ResultSet userIdRS = stmt.executeQuery(queryUserId);
		if(userIdRS.next()){
			return userIdRS.getInt(1);
		}
		return -45;
	}


	/**
	 * Añade una posición a la base de datos asociada al usuario que envía el comando.
	 */
	private static int addPosition(int userId, double latitude, double longitude, Statement stmt) throws SQLException {
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		long time = timestamp.getTime();
		String updatePos = String.format(
				"INSERT INTO Location VALUES ('%d', '%d', '%f', '%f')",
				userId, time, latitude, longitude);
		try{
			stmt.executeUpdate(updatePos);
		}catch (Exception e){
			System.out.println("patata");
		}
		return 0;
	}

	/**
	 * Método envoltorio que comprueba la validez de los parametros suministrados al
	 * comando addPosition (que sean 2 y que sean numéricos). También comprueba que
	 * el cliente que envia el comando ha iniciado sesion previamente.
	 */
	private static int addPosition(String[] fields, Statement stmt) throws SQLException {
		if(Integer.parseInt(fields[0]) == 0) return -46; //user not authenticated
		if(fields.length != 4) return -42;
		if(isNumeric(fields[2]) && isNumeric(fields[3])){
			return addPosition(
					Integer.parseInt(fields[0]),
					Double.parseDouble(fields[2]),
					Double.parseDouble(fields[3]),
					stmt);
		}
		return -421;
	}

	/**
	 * Comprueba si un String es numerico y puede convertirse a un double.
	 * @param num String sobre el que se realiza la comprobacion.
	 * @return Verdadero si el string es numerico, falso sino.
	 */
	private static boolean isNumeric(String num){
		return num.matches("-?\\d+(\\.\\d+)?"); //expresion regular.
	}


	/**
	 * Comrpueba si el usuario tiene permiso para ejecutar el comando listUsers
	 * @param fields Mensaje recibido del cliente
	 * @return 0 si exito, -47 si no esta autorizado.
	 */
	private static int listUsers(String[] fields, Statement stmt) throws SQLException {
		if(!isAdmin(Integer.parseInt(fields[0]), stmt)) return -47;
		return 0;
	}

	/**
	 * Compruba si un usuario dado es Administrador o no.
	 * @param userId usuario sobre el que se realiza la comprobacion
	 * @return true si es admin, false si no.
	 */
	private static boolean isAdmin(int userId, Statement stmt) throws SQLException {
		String adminIdQuery = String.format(
				"SELECT userId FROM Admin WHERE userId = %d", userId);
		ResultSet adminUserIdRS = stmt.executeQuery(adminIdQuery);
		return adminUserIdRS.next();
	}

	/**
	 * Consulta y devuelve los usuarios del sistema
	 * @return String con los usernames separados por un espacio en blanco.
	 */
	private static String getUsers(Statement stmt) throws SQLException {
		ResultSet usersRS = stmt.executeQuery("SELECT username FROM User");
		String users = "";
		while(usersRS.next()){
			users += usersRS.getString(1) + " ";
		}
		return users;
	}


	/**
	 * Comprueba que tipo de listPositions se ha ejecutado: El normal o el de
	 * administrador (y llama a dicho metodo). También comprueba si se ha
	 * suministrado un parametro de tiempo (por ejemplo -10d = ultimos 10 dias).
	 * Si no se suministra parametro de tiempo, utiliza el valor por defecto
	 * almacenado en defaultTimeFrame.
	 * @param fields mensaje recibido del cliente.
	 * @param positions Se utiliza para guardar las posiciones en caso de exito.
	 * @return 0 si exito, codigo de error sino.
	 */
	private static int listPositions(String[] fields, Statement stmt,
									 StringBuilder positions) throws SQLException {
		int userId = Integer.parseInt(fields[0]);
		long defaultTF = getTimeFrame(defaultTimeFrame);
		if(fields.length == 2) //listPositions
			return listPositionsUser(userId, stmt, positions, defaultTF);
		if(isTimeParameter(fields[2])) { //listPositions -10s (ejemplo)
			long timeFrame = getTimeFrame(fields[2]);
			return listPositionsUser(userId, stmt, positions, timeFrame);
		}
		//listPositions ussername ... ?
		return listPositionsAdmin(fields, stmt, positions, defaultTF);
	}

	/**
	 * Lista las posiciones del usuario indicado siempre que este exista.
	 * @param userId Usuario sobre el que se listan las posiciones.
	 * @param positions Parametro de salida que se utiliza para almacenar las
	 * posiciones del usuario.
	 * @return 0 si exito, -46 en caso de que el usuario no haya inciado sesión.
	 */
	private static int listPositionsUser(int userId, Statement stmt,
										 StringBuilder positions, long timeFrame) throws SQLException {
		if(userId == 0) return -46; //usuario no ha iniciado sesion.
		getPositions(userId, stmt, positions, timeFrame);
		return 0;
	}

	/**
	 * Lista las posiciones del usuario pasado como parametro con el comando
	 * listPositions. Solo disponible para usuarios administradores.
	 * @param fields Mensaje recibido del cliente.
	 * @param positions Parametro de salida que se utiliza para almacenar las
	 * posiciones del usuario.
	 * @return 0 si exito; -47 si el usuario es administrador; -45 si el usuario
	 * usuario pasado como parametro no existe (consultar diccionario de errores).
	 */
	private static int listPositionsAdmin(String[] fields, Statement stmt,
										  StringBuilder positions, long defaultTimeFrame) throws SQLException {
		if(!isAdmin(Integer.parseInt(fields[0]), stmt)) return -47; //permission denied
		int userId = getUserId(fields[2], stmt);
		if(userId <= 0)
			return -45; //user not found
		if(fields.length == 3) { //no parametro de tiempo (se usa el default)
			getPositions(userId, stmt, positions, defaultTimeFrame);
			return 0;
		}
		if(isTimeParameter(fields[3])){ //parametro de tiempo pasado
			long timeFrame = getTimeFrame(fields[3]);
			return listPositionsUser(userId, stmt, positions, timeFrame);
		}
		return -421; // wrong type arguments
	}

	/**
	 * Almacena en el StringBuilder positions todas las posiciones del usuario a
	 * partir de la fecha indicada. Esta puede ser especificada por el usuario,
	 * o ser el valor por defecto (-1y = ultimo año).
	 * El formato de cada posicion es: fecha_hora|latitud|longitud. Las posiciones
	 * se separan entre si utilizando el simbolo '//' sin las comillas.
	 * @param userId Id del usuario sobre el que se consultan las posiciones.
	 * @param positions Parametro de salida que se utiliza para almacenar las
	 * posiciones del usuario.
	 */
	private static void getPositions(int userId, Statement stmt,
									 StringBuilder positions, long timeFrame) throws SQLException {
		String queryPositions = String.format(
				"SELECT time, latitude, longitude FROM Location WHERE userId = %d "
						+ "AND time > %d", userId, timeFrame);
		ResultSet posRS = stmt.executeQuery(queryPositions);
		while(posRS.next()){
			//cambiamos el espacio entre fecha y hora por una '_':
			String time = posRS.getTimestamp(1).toString().replace(" ", "_");
			positions.append(time + "|");
			positions.append(posRS.getString(2) + "|");
			positions.append(posRS.getString(3) + "//");
		}
	}

	/**
	 * Compruba si una determinada cadena cumple el format: guión, numero, y una
	 * de las letras s (second), m (minute), h(hour)...
	 */
	private static boolean isTimeParameter(String input){
		String regex = "-{1}\\d+[s,m,h,d,w,p,y]";
		return input.matches(regex);
	}

	/**
	 * Dado un parametro (que previamente ha pasado el test isTimeParameter) del
	 * comando listPositions del tipo '-10d', crea un timestamp del momento actual
	 * y lo modifica de acuerdo al parametro. La letra del comando indica el
	 * periodo de modificacion (segundos = s, semanas = w, etc.) y la parte
	 * numerica la cantidad. Por ejemplo, el parametro -10d creará una marca
	 * temporal referente al momento de creacion menos 10 dias.
	 * @param timeParameter parametro recibido que ha pasado el test
	 * isTimeParameter(String input)
	 * @return marca temporal modificada en el fomato "milisegundos transucrridos
	 * desde 1970" (unix epoch).
	 */
	private static long getTimeFrame(String timeParameter){
		//Extrer el numero y la letra del timeParameter:
		long amount = Long.parseLong(timeParameter.split("\\D")[1]);
		char periodLetter = timeParameter.charAt(timeParameter.length()-1);

		//Modificar instancia de LocalDateTime de acuerdo a los parametros
		LocalDateTime ldt = LocalDateTime.now();
		switch(periodLetter){
			case 's':
				ldt = ldt.minusSeconds(amount);
				break;
			case 'm':
				ldt = ldt.minusMinutes(amount);
				break;
			case 'h':
				ldt = ldt.minusHours(amount);
				break;
			case 'd':
				ldt = ldt.minusDays(amount);
				break;
			case 'w':
				ldt = ldt.minusWeeks(amount);
				break;
			case 'p':
				ldt = ldt.minusMonths(amount);
				break;
			case 'y':
				ldt = ldt.minusYears(amount);
				break;
		}
		//Convertir ldt a milisegundos epoch
		ZonedDateTime zdt = ldt.atZone(ZoneId.of("Europe/Madrid"));
		long timeFrame = zdt.toInstant().toEpochMilli();
		return timeFrame;
	}


	/**
	 * Comprubea si el usuario ha iniciado sesión para poder ejecutar el comando
	 * startPositions.
	 */
	private static int startPositions(String[] fields){
		if(userLoggedIn(fields)) return 0;
		return -46;
	}

	private static int stopPositions(String[] fields){
		if(userLoggedIn(fields)) return 0;
		return -46;
	}

	private static boolean userLoggedIn(String[] fields){
		return Integer.parseInt(fields[0]) > 0;
	}


	/**
	 * Este metodo es de la interfaz Runnable. Envuelve el bloque que se ejecutara
	 * cuando se inice un Thread de tipo ServerInstance. (Antes este metodo era en
	 * realidad el metodo main de la clase server).
	 */
	public void run() {
		try {
			Connection con = DriverManager.getConnection("jdbc:sqlite:Contactus.db");
			Statement stmt = con.createStatement();

			stmt.execute("PRAGMA foreign_keys = ON"); //enable foreign key behavior

			//ServerSocket serverSocket = new ServerSocket(8000);
			//System.out.println("Waiting for client conexions...");
			//Socket clientSocket = serverSocket.accept();
			System.out.println("Conection established from a ServerInstance!");

			BufferedReader in = new BufferedReader(
					new InputStreamReader(clientSocket.getInputStream()));
			PrintWriter out = new PrintWriter(
					clientSocket.getOutputStream(), true);

			boolean execute = true;
			while(execute){
				//Mensaje recibido del cliente (lo dividimos por palabras con split):
				String msgReceived = in.readLine();
				String[] fields = msgReceived.split(" ");
				System.out.print("Message received from user " + fields[0] + ": ");
				System.out.println(msgReceived);

				//if(msgReceived.equals("close")) break; //temporal

				//Implementar lógica aquí


				//Informacion adcional para contestar al cliente
				String info1 = "";
				String info2 = "";
				StringBuilder info3 = new StringBuilder();
				int res = -1; //resultado del comando
				if(fields.length > 1){
					switch(fields[1]) {
						case "addUser":
							res = addUser(fields, stmt);
							break;
						case "removeUser":
							res = removeUser(fields, stmt);
							break;
						case "login":
							res = login(fields, stmt);
							if(res == 0) {
								info1 = Integer.toString(getUserId(fields[2], stmt));
								info2 = fields[2]; //devolvemos tambien el username
							}
							stmt.close();
							break;
						case "logout":
							res = 0;
							break;
						case "addPosition":
							res = addPosition(fields, stmt);
							break;
						case "listUsers":
							res = listUsers(fields, stmt);
							if(res == 0) info1 = getUsers(stmt);
							break;
						case "listPositions":
							res = listPositions(fields, stmt, info3);
							break;
						case "startPositions":
							res = startPositions(fields);
							break;
						case "stopPositions":
							res = stopPositions(fields);
							break;
						case "exit":
							res = 0;
							execute = false;
							break;
						case "debug":
							res = 0;

						default: break;
					}
				}

				//Preparacion del mensaje a envíar:
				String command = (res == -1) ? "unrecognized" : fields[1];
				String msgToSend = command + " " + res + " " + info1 + " " + info2 + " "
						+ info3.toString();
				msgToSend = msgToSend.replaceAll("\\s{2,}", " "); //eliminamos dobles/triples espacios

				//Envio del mensaje:
				out.println(msgToSend);;
			}

			con.close();
			clientSocket.close();

		} catch(IOException|SQLException e){
			e.printStackTrace();
		}
	}

}