package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.lang.Thread.sleep;

import java.net.Socket;

import java.sql.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;


/**
 * Las intancias de esta clase se ejecutan en threads secundarios que
 * va lanzando la clase server cada vez que recibe una nueva conexion de un
 * cliente. Lo que se ejecuta está en el metodo run().
 */
public class ServerInstance implements Runnable {

	private static Socket clientSocket;

	private static Connection con;


	public ServerInstance(Socket clientSocket, Connection con){
		this.clientSocket = clientSocket;
		this.con = con;
	}

	//¡¡IMPORTANTE!! No convirtais las variables staticas globales a variables locales,
	// aunque os lo proponga el IntelliJ.

	/**
	 * Utilizado cuando el comando listPositions no suministra un parametro
	 * de tiempo.
	 */
	private static String defaultTimeFrame = "-1y"; //ultimo año

	/**
	 * Cantidad de tiempo que vamos a saltar hacia atrás para rastrear los conctactos
	 * cercanos de los infectados para detectar usuarios suspects/sospechosos.
	 */
	private static String traceRetrospectiveReach = "-2d";

	/**
	 * Tiempo máximo que pueden estar 2 usuarios juntos. Si se supera, se considerarán
	 * a esos usuarios como contactos cercanos.
	 */
	public static long CLOSE_CONTACT_TIME = 10000; //10 segundos, para depurar y testear

	//Definición de los estados, para el campo state de la tabla user
	private static final String healthyState = "healthy";
	private static final String suspectState = "suspect";
	private static final String infectedState = "infected";

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

		insertNewUser(username, pwd, stmt);
		return 0;
	}

	public static int checkCloseContacts(String[] fields, Statement stmt) throws SQLException {
		int userId = Integer.parseInt(fields[0]);
		if(!isAdmin(userId, stmt)) return -47;

		checkCloseContacts(stmt);
		return 0;
	}

	public static void checkCloseContacts(Statement stmt) throws SQLException {
		checkCloseContactsOfType("Infected", true, stmt);
		checkCloseContactsOfType("Suspect", false, stmt);
	}

	public static void checkCloseContactsOfType(String table, boolean infected, Statement stmt)
		throws SQLException
	{
		List<Integer> typeUserIds = getTypeUserIds(table, stmt);
		for(int userId : typeUserIds){
			checkCloseContacts(userId, infected, stmt);
		}
	}

	public static void checkCloseContacts(int DangerUserId, boolean infected, Statement stmt)
			throws SQLException
	{
		//Obtener instante a partir del cual rastrear contactos cercanos
		long timeFrame = getLastCloseContactCheck(DangerUserId, infected, stmt);

		//Obtenemos todas las posiciones sin rastrear del usuario peligroso
		List<Location> dangerUserUncheckedLocations = getUserLocationsInRange(DangerUserId, timeFrame, stmt);

		//juntamos ids de suspect y healthy en la lista healthyUsersIdsList
		List<Integer> healthyUserIdsList = new ArrayList();
		healthyUserIdsList.addAll(getTypeUserIds("Healthy", stmt));
		healthyUserIdsList.addAll(getTypeUserIds("Suspect", stmt));

		//Para cada usuario healthy o suspect...
		while(!healthyUserIdsList.isEmpty()){
			int currHealthyUser = healthyUserIdsList.get(0);

			//Obtenemos todas sus posiciones en el mismo rango de tiempo que el usuario infectado
			List<Location> healhtyUserLocations = getUserLocationsInRange(currHealthyUser, timeFrame, stmt);

			//Si han estado juntos lo suficiente, marcamos al usuario healthy como suspect
			Contact contact = new Contact();
			if(haveBeenTogetherLongEnough(dangerUserUncheckedLocations, healhtyUserLocations, contact, stmt)) {
				suspect(currHealthyUser, DangerUserId, contact.contactTime, stmt);
			}

			healthyUserIdsList.remove(0);
		}

	}

	/**
	 * Dadas 2 listas de posiciones correspondientes a un mismo interval temporal, compara
	 * todas las posiciones de una lista con todas las posicoines de la otra buscando matches.
	 * Si detecta un match (isNear devuelve true), llama al método calculateTimeTogether para
	 * determinar si han estado el suficiente tiempo juntos como para considerarse ese contacto
	 * un contacto cercano.
	 *
	 * @param dangerUserLocations Lista de posisciones del usuario infectado o suspect.
	 * @param healthyUserLocations Lista de posiciones del usuario healthy
	 * @param contact Objeto utilizado para pasar como referencia el valor entero contactTime.
	 * @return true si se ddtecta un contacto cercano, false sino.
	 */
	public static boolean haveBeenTogetherLongEnough(
			List<Location> dangerUserLocations, List<Location> healthyUserLocations,
			Contact contact, Statement stmt)
			throws SQLException
	{
		for (Location currDangerLocation : dangerUserLocations) {
			for (Location currHealhtyLocation : healthyUserLocations) {
				if (currDangerLocation.isNear(currHealhtyLocation)) {
					long timeTogether = calculateTimeTogether(currDangerLocation, currHealhtyLocation, stmt);
					if(timeTogether > CLOSE_CONTACT_TIME){
						contact.contactTime = timeTogether;
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Dadas 2 posiciones iniciales de 2 usuarios distitntos que se encuentran en el mismo lugar
	 * en el mismo momento, reconstruye los siguientes movimientos de esos usuarios para determinar
	 * el tiempo que han permanecido juntos. Se asume que hasta que no se registre una nueva
	 * posición para un usuario, dicho usuario no se ha movido.
	 *
	 * @param startLocUser1 Posición de partida del usuario 1
	 * @param startLocUser2 Posición de partida del usuario 2
	 * @return Tiempo que han permanecido juntos los 2 usuarios en milisegundos.
	 */
	public static long calculateTimeTogether(Location startLocUser1, Location startLocUser2,
											 Statement stmt)
		throws SQLException
	{
		//Juntamos en un única lista todas las posiciones de los 2 usuarios a partir de sus
		//posiciones iniciales.
		List<Location> user1And2Locations = new ArrayList<>();
		user1And2Locations.addAll(getUserLocationsFromStartLocation(startLocUser1, stmt));
		user1And2Locations.addAll(getUserLocationsFromStartLocation(startLocUser2, stmt));

		//Convertimos la lista en Array y lo ordenamos según la fecha de las posiciones.
		//Este comportamiento para ordenar está definido en el método compareTo de la clase Location.
		Location [] path = new Location[user1And2Locations.size()];
		user1And2Locations.toArray(path);
		Arrays.sort(path);

		Location u1Location = startLocUser1;
		Location u2Location = startLocUser2;

		long timeTogether = 0;

		int user1 = startLocUser1.userId;
		int user2 = startLocUser2.userId;

		//Cada nueva posición es un movimiento de uno de los 2 usuarios
		for(Location move : path){
			timeTogether += Location.calculateTimeDiff(u1Location, move);

			//Determinar quien de los 2 usuario se ha movido.
			if(move.userId == user1) u1Location = move;
			else u2Location = move;

			//Si la nueva distancia supera MIN_DISTANCE significa que los usuarios se han alejado,
			//Por lo que la iteración se detiene.
			double distance = Location.calculateDistance(u1Location, u2Location);
			if(distance > Location.MIN_DISTANCE){
				return timeTogether;
			}
		}
		//Si se llega hasta aquí significa que ningúna de las nuevas posiciones ha roto la distancia
		//de seguridad, por lo que hasta el momento actual permanecen juntos.
		return Math.abs(u1Location.time - getCurrentTime());
	}


	/**
	 * Devuelve en una lista de ints todos los ids de los usuarios healthy.
	 */
	public static List<Integer> getHealthyUserIds(Statement stmt) throws SQLException {
		List<Integer> healhtyUserIds = new ArrayList<>();
		ResultSet healthyUserIdsRS = stmt.executeQuery("SELECT userId FROM Healthy");
		while(healthyUserIdsRS.next()){
			healhtyUserIds.add(healthyUserIdsRS.getInt(1));
		}
		return healhtyUserIds;
	}

	/**
	 * Devuelve lista con ids de usuarios de un tipo concreto (healthy, suspect, infected...)
	 * @param table Tabla del tipo de usuario (Healthy, Suspect o Infected)
	 */
	public static List<Integer> getTypeUserIds(String table, Statement stmt) throws SQLException {
		List<Integer> typeUserIds = new ArrayList();
		ResultSet typeUserIdsRS = stmt.executeQuery(String.format(
				"SELECT userId FROM %s", table));
		while(typeUserIdsRS.next()){
			typeUserIds.add(typeUserIdsRS.getInt(1));
		}
		return typeUserIds;
	}


	/**
	 * Devuelve todas las locations de un usuario a partir de un determinado instante, pasado
	 * en el campo timeFrame.
	 * @param timeFrame Intante a partir del cual se obtienen las posiciones, en milisegundos desde
	 *                  el epoch.
	 */
	public static List<Location> getUserLocationsInRange(int userId, long timeFrame, Statement stmt)
		throws SQLException
	{
		List<Location> locationsList = new ArrayList<>();
		ResultSet locationsRS = stmt.executeQuery(String.format(
				"SELECT locationId, userId, time, latitude, longitude FROM Location WHERE " +
						"userId = %d AND time > %d", userId, timeFrame));
		while(locationsRS.next()){
			locationsList.add(new Location(locationsRS));
		}
		return locationsList;
	}

	/**
	 * Devuelve una lista de posiciones a partir de una posición inicial.
	 * @param startLocation
	 */
	public static List<Location> getUserLocationsFromStartLocation(Location startLocation, Statement stmt)
			throws SQLException {
		return getUserLocationsInRange(startLocation.userId, startLocation.time, stmt);
	}

	/**
	 * Devuelve la fecha en la que se hizo el último rastreo de contactos cercanos del usuario
	 * pasado como parametro en userId.
	 * @param infected boolean para indicar si se trata de un usuario infected o suspect, ya que
	 *                 para ambos se aplica el rastreo.
	 */
	public static long getLastCloseContactCheck(int userId, boolean infected, Statement stmt)
			throws SQLException
	{
		String state = infected ? "Infected" : "Suspect";
		ResultSet lastCloseContactCheckRS = stmt.executeQuery(String.format(
				"SELECT lastCloseContactsCheck FROM %s WHERE userId = %d", state, userId));
		lastCloseContactCheckRS.next();
		return lastCloseContactCheckRS.getLong(1);
	}

	/**
	 * Inserta un usuario nuevo en las tablas User y healthy.
	 */
	private static void insertNewUser(String username, String pwd, Statement stmt) throws SQLException {
		stmt.executeUpdate(String.format(
				"INSERT INTO User (username, password) VALUES ('%s', '%s')", username, pwd));

		//Now update Healthy table as well (need to first fetch the new user's id)
		ResultSet newUserIdRS = stmt.executeQuery(String.format(
				"SELECT userId FROM User WHERE username = '%s'", username));
		newUserIdRS.next();
		int userId = newUserIdRS.getInt(1);
		stmt.executeUpdate(String.format("INSERT INTO Healthy VALUES (%d)", userId));
	}

	/**
	 * Borra un usuario y toda su información relacionada en cascada (posiciones, historial, etc).
	 * (las posiciones y el resto de información relacionado con el user se borra automáticamente
	 * gracias a poner en sqlite DELETE CASCADE en los foreign keys de userId).
	 * @return 0 sí consigue borrar un usuario; -4 si no encuentra el usuario a borrar o falla la
	 * contraseña.
	 */
	private static int removeUser (String username, String pwd, Statement stmt) throws SQLException{
		int deleted = stmt.executeUpdate(String.format(
				"DELETE FROM User WHERE (username = '%s' AND password = '%s')", username, pwd));
		System.out.println("deleted: " + deleted);
		return deleted > 0 ? 0 : -4;
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
		return -42;
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
				"INSERT INTO Location (userId, time, latitude, longitude) VALUES ('%d', '%d', '%f', '%f')",
				userId, time, latitude, longitude);
		try{
			stmt.executeUpdate(updatePos);
		}catch (Exception e){
			e.printStackTrace();
			return -4;
		}

		//Actualizar campo lastLocation de la tabla User con la nueva posición
		int locationId = getLocationId(userId, time, stmt);
		updateUserLastPosition(userId, locationId, stmt);
		return 0;
	}

	/**
	 * Devuelve la clave primaria locationId de una location dados su userId y el instante time.
	 */
	private static int getLocationId(int userId, long time, Statement stmt) throws SQLException {
		ResultSet locationIdRS = stmt.executeQuery(String.format(
				"SELECT locationId FROM Location WHERE userId = %d AND time = %d", userId, time));
		locationIdRS.next();
		return locationIdRS.getInt(1);
	}

	/**
	 * Actualiza la última posición del usuario dado (el campo lastLocation de la tabla User).
	 */
	private static int updateUserLastPosition(int userId, int locationId, Statement stmt) throws SQLException {
		int rowUpdated = stmt.executeUpdate(String.format(
				"UPDATE User SET lastLocation = %d WHERE userId = %d", locationId, userId));
		return rowUpdated;
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
	private static String getUserss(Statement stmt) throws SQLException {
		ResultSet usersRS = stmt.executeQuery("SELECT username FROM User");
		String users = "";
		while(usersRS.next()){
			users += usersRS.getString(1) + " ";
		}
		return users;
	}

	private static String getUsers(Statement stmt) throws SQLException{
		//Obtener listas de los tres tipos de usuarios
		List<User> healthies = getHealthyUsers(stmt);
		List<User> infecteds = getInfectedUsers(stmt);
		List<User> suspects = getSuspectUsers(stmt);

		//Añadir usuarios a StringBuilder.
		StringBuilder sb = new StringBuilder();
		appendUserList(sb, healthies);
		appendUserList(sb, infecteds);
		appendUserList(sb, suspects);

		return sb.toString();
	}

	private static void appendUserList(StringBuilder sb, List<User> users){
		sb.append("f"); //para evitar null Pointer si no hay usuarios de algún tipo.
		for(User user : users){
			sb.append(user + "//");
		}
		sb.append("|");
	}

	private static List<User> getSuspectUsers(Statement stmt) throws SQLException {
		ResultSet suspectsRS = stmt.executeQuery(
				"SELECT S.userId, Uusr.username, suspectSince, Uinf.username, contactDuration " +
					"FROM Suspect S " +
					"INNER JOIN User Uusr USING(userId) " +
					"INNER JOIN User Uinf ON S.infectedBy = Uinf.userId");
		List<User> suspects = new ArrayList();
		while(suspectsRS.next()){
			suspects.add(new SuspectUser(suspectsRS));
		}
		return suspects;
	}

	private static List<User> getInfectedUsers(Statement stmt) throws SQLException {
		ResultSet infectedsRS = stmt.executeQuery(
				"SELECT I.userId, username, infectedSince " +
					"FROM User INNER JOIN Infected I USING(userId)");
		List<User> infecteds = new ArrayList();
		while(infectedsRS.next()){
			infecteds.add(new InfectedUser(infectedsRS));
		}
		return infecteds;
	}

	private static List<User> getHealthyUsers(Statement stmt) throws SQLException {
		ResultSet healthiesRS = stmt.executeQuery(
				"SELECT H.userId, username FROM User INNER JOIN Healthy H USING(userId)");
		List<User> healthies = new ArrayList<>();
		while(healthiesRS.next()){
			healthies.add(new HealthyUser(healthiesRS));
		}
		return healthies;
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

	/**
	 * Comprueba que el usuario ha iniciado sesión y no está infectado para dejarle ejecutar
	 * startAlarms.
	 */
	private static int startAlarms(String[] fields, Statement stmt) throws SQLException {
		int userId = Integer.parseInt(fields[0]);
		if(!userLoggedIn(fields)) return -46;
		if(isInfected(userId, stmt)) return -482;
		return 0;
	}

	private static int stopPositions(String[] fields){
		if(userLoggedIn(fields)) return 0;
		return -46;
	}

	private static int stopAlarms(String[] fields){
		if(userLoggedIn(fields)) return 0;
		return -46;
	}

	private static boolean userLoggedIn(String[] fields){
		return Integer.parseInt(fields[0]) > 0;
	}
	private static boolean userLoggedIn(int userId){
		return userId > 0;
	}

	/**
	 * Envuelve al metodo infected principal, y realiza 3 comprobaciones: que el usuario
	 * esté autenticado, que no esté infectado, y el número de argumentos.
	 * @param fields Mensaje recibido del cliente.
	 * @return 0 sí exito, código de error sino.
	 */
	private static int infected(String[] fields, Statement stmt) throws SQLException {
		int userId = Integer.parseInt(fields[0]);
		if(fields.length != 2) return -42;
		if(!userLoggedIn(fields)) return -46; //not authenticated
		if(isInfected(userId, stmt)) return -471; //user already infected

		return infected(userId, stmt);
	}

	/**
	 * Modifica el estado de un usuario a infected. Incluye la lógica para gestionar
	 * las diferentes tablas (healthy, infected y suspect).
	 */
	private static int infected(int userId, Statement stmt) throws SQLException {
		//quitar al usuario de las tablas healthy y suspect (si está)
		int delHealthy = deleteUserFromTable("Healthy", userId, stmt);
		int delSuspect = deleteUserFromTable("Suspect", userId, stmt);

		//Averiguar el estado del usuario antes de infectarse
		String previousState = delHealthy > 0 ? healthyState : suspectState;

		//Insertar en Infected
		long infectionTime = getCurrentTime();
		long defaultLastCheck = getTimeFrame(traceRetrospectiveReach); //fecha actual - 2 días
		stmt.executeUpdate(String.format(
				"INSERT INTO INFECTED VALUES (%d, %d, %d)", userId, infectionTime, defaultLastCheck));

		updateUserStateField(userId, infectedState, stmt); //actualizar campo state de la tabla User

		//Actualizar historial (tabla StateHistory) con el cambio
		updateStateHistory(userId, infectionTime, previousState, infectedState, stmt);

		checkCloseContacts(userId, true, stmt);

		return 0;
	}

	/**
	 * Devuelve el instante actual en milisegundos transcurridos desde el epoch.
	 */
	private static long getCurrentTime(){
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		return timestamp.getTime();
	}

	/**
	 * Checks if a given user is infected.
	 */
	private static boolean isInfected(int userId, Statement stmt) throws SQLException {
		ResultSet infectedUserIdRS = stmt.executeQuery(String.format(
				"SELECT userId FROM Infected WHERE userId = %s", userId));
		return infectedUserIdRS.next();
	}

	/**
	 * Elimina a un usuario de la tabla dada, si lo encuentra.
	 * @return 1 si elimina un usuario, 0 sino.
	 */
	private static int deleteUserFromTable(String table, int userId, Statement stmt) throws SQLException{
		int deletedRows = stmt.executeUpdate(String.format(
				"DELETE FROM %s WHERE userId = %d", table, userId));
		return deletedRows;
	}

	/**
	 * Envuelve al metodo healthy principal, y realiza 3 comprobaciones: que el usuario
	 * esté autenticado, que no esté healthy ya, y el número de argumentos.
	 * @param fields Mensaje recibido del cliente.
	 * @return 0 sí exito, código de error sino.
	 */
	private static int healthy(String[] fields, Statement stmt) throws SQLException {
		int userId = Integer.parseInt(fields[0]);
		if(fields.length != 2) return -42;
		if(!userLoggedIn(fields)) return -46; //not authenticated
		if(isHealthy(userId, stmt)) return -472; //user already healthy

		return healthy(userId, stmt);
	}

	/**
	 * Modifica el estado de un usuario a healthy. Incluye la lógica para gestionar
	 * las diferentes tablas (healthy, infected y suspect).
	 */
	private static int healthy(int userId, Statement stmt) throws SQLException {
		//quitar al usuario de las infected y suspect (si está)
		int delInfected = deleteUserFromTable("Infected", userId, stmt);
		int delSuspect = deleteUserFromTable("Suspect", userId, stmt);

		//Averiguar el estado del usuario antes de infectarse
		String previousState = delInfected > 0 ? infectedState : suspectState;

		//Insertar en Healthy
		stmt.executeUpdate(String.format(
				"INSERT INTO Healthy VALUES (%d)", userId));

		updateUserStateField(userId, healthyState, stmt); //actualizar campo state de la tabla User

		//Actualizar historial (tabla StateHistory) con el cambio
		long recoveryTime = getCurrentTime();
		updateStateHistory(userId, recoveryTime, previousState, healthyState, stmt);
		return 0;
	}

	/**
	 * Actualiza el campo state de la tabla User al estado indicado en el parametro state.
	 * Recordar que 0 = healthy; 1 = infected; null = suspect
	 */
	private static int updateUserStateField(int userId, String state, Statement stmt) throws SQLException {
		String newState = "";
		switch(state){
			case healthyState -> newState = "0";
			case suspectState -> newState = "null";
			case infectedState -> newState = "1";
		}
		int updatedRowCount = stmt.executeUpdate(String.format(
				"UPDATE User SET state = %s WHERE userId = %d", newState, userId));
		return updatedRowCount;
	}

	/**
	 * Inserta un nuevo registro en la tabla StateHistory (registra cambios de estado).
	 * @param prevState Estado previo
	 * @param curState Estado actual.
	 */
	private static int updateStateHistory(int userId, long time, String prevState,
										  String curState, Statement stmt) throws SQLException {
		int insertedRow = stmt.executeUpdate(String.format(
				"INSERT INTO StateHistory VALUES (%d, %d, '%s', '%s')",
				userId, time, prevState, curState));
		return insertedRow;
	}

	/**
	 * Comprueba si el estado de un usuario es healthy
	 */
	private static boolean isHealthy(int userId, Statement stmt) throws SQLException {
		ResultSet healthyUserIdRS = stmt.executeQuery(String.format(
				"SELECT userId FROM Healthy WHERE userId = %s", userId));
		return healthyUserIdRS.next();
	}

	/**
	 * Envuelve al metodo suspect principal, y realiza 4 comprobaciones: que el usuario
	 * esté autenticado, que no sea suspect, que no sea infected (no se permite pasar de
	 * infected a suspect) y el número de argumentos.
	 * @param fields Mensaje recibido del cliente.
	 * @return 0 sí exito, código de error sino.
	 */
	private static int suspect(String[] fields, Statement stmt) throws SQLException {
		int userId = Integer.parseInt(fields[0]);
		if(fields.length != 2) return -42;
		if(!userLoggedIn(fields)) return -46; //not authenticated
		if(isSuspect(userId, stmt)) return -473; //user already suspect
		if(isInfected(userId, stmt)) return -481; //No permitimos pasar de infected a suspect

		return suspect(userId, stmt);
	}

	/**
	 * Modifica el estado de un usuario a healthy. Incluye la lógica para gestionar
	 * las diferentes tablas (healthy, infected y suspect). Deprecated, ya que ahora
	 * cuando añadimos un suspect también guardamos info sobre quién ha sido su
	 * contacto cercano y cuanto tiempo ha estado con el.
	 */
	@Deprecated
	private static int suspect(int userId, Statement stmt) throws SQLException {
		//quitar al usuario de las tabla healthy
		int delHealthy = deleteUserFromTable("Healthy", userId, stmt);

		String previousState = healthyState; //solo se puede pasar a suspect desde healthy

		//Insertar en Suspect
		long suspectSince = getCurrentTime();
		long defaultLastCheck = getTimeFrame(traceRetrospectiveReach); //fecha actual - 2 días
		stmt.executeUpdate(String.format(
				"INSERT INTO Suspect (userId, suspectSince, lastCloseContactCheck) VALUES (%d, %d, %d)",
				userId, suspectSince, defaultLastCheck));

		updateUserStateField(userId, suspectState, stmt); //actualizar campo state de la tabla User

		//Actualizar historial (tabla StateHistory) con el cambio
		updateStateHistory(userId, suspectSince, previousState, suspectState, stmt);
		return 0;
	}

	/**
	 * Modifica el estado de un usuario healthy a suspect.
	 * @param userId id del usuario que pasa a estado suspect.
	 * @param dangerUserId id del usuario que ha provocado que el usuario userId pase a estado suspect.
	 * @param contactDuration Tiempo que ha durado el contacto cercano.
	 */
	private static int suspect(int userId, int dangerUserId, long contactDuration, Statement stmt)
			throws SQLException
	{
		//quitar al usuario de las tabla healthy
		int delHealthy = deleteUserFromTable("Healthy", userId, stmt);

		String previousState = healthyState; //solo se puede pasar a suspect desde healthy

		//Si yá es suspect, unicamente hay que actualizar contact duration y lastCloseContactCheck
		if(isSuspect(userId, stmt)){
			stmt.executeUpdate(String.format(
					"UPDATE Suspect SET lastCloseContactsCheck = %d, contactDuration = %d WHERE userId = %d",
					getCurrentTime(), contactDuration, userId));
		}
		else {
			//Sino, Insertar en Suspect
			long suspectSince = getCurrentTime();
			long defaultLastCheck = getTimeFrame(traceRetrospectiveReach); //fecha actual - 2 días
			stmt.executeUpdate(String.format(
					"INSERT INTO Suspect (userId, suspectSince, lastCloseContactsCheck, infectedBy, contactDuration) " +
							"VALUES (%d, %d, %d, %d, %d)",
					userId, suspectSince, defaultLastCheck, dangerUserId, contactDuration));

			updateUserStateField(userId, suspectState, stmt); //actualizar campo state de la tabla User

			//Actualizar historial (tabla StateHistory) con el cambio
			updateStateHistory(userId, suspectSince, previousState, suspectState, stmt);
		}

		return 0;
	}

	/**
	 * Comprueba si el estado de un usuario es healthy
	 */
	private static boolean isSuspect(int userId, Statement stmt) throws SQLException {
		ResultSet suspectUserIdRS = stmt.executeQuery(String.format(
				"SELECT userId FROM Suspect WHERE userId = %s", userId));
		return suspectUserIdRS.next();
	}

	/**
	 * Envuelve al método principal de listAlarms. Realiza varias comprobaciones sobre el
	 * mensaje recibido del cliente, como el número de parametros, si ha iniciado sesión,
	 * o su estado (solo pueden ejecutar listAlarms los usuarios helathy y suspect).
	 *
	 * @param fields Mensaje recibido del cliente separado por espacios.
	 * @param alarmsSB StringBuilder utilizado como parametro de salida para devolver la lista de alarmas.
	 * @return 0 si exito, código de error si falla alguna validación.
	 * @throws SQLException
	 */
	private static int listAlarms(String[] fields, Statement stmt, StringBuilder alarmsSB) throws SQLException {
		int userId = Integer.parseInt(fields[0]);
		if(fields.length != 2) return -42; //CAMBIAR más adelante para el listAlarms de admin
		if(!userLoggedIn(userId)) return -46;
		if(isInfected(userId, stmt)) return  -482; //No avisamos a infectados si se les acerca otro infectado
		if(userHasNoLocation(userId, stmt)) return -491;

		return listAlarms(userId, stmt, alarmsSB);
	}

	/**
	 * Comprueba en tiempo real si hay algún usuario peligroso (suspect o infected) cerca del
	 * usuario indicado en userId. Para ello se cogen las últimas posiciones de todos los usuarios
	 * peligros y se comparán con la última posición de userId usando el método de la clase
	 * Location Location.isNear(Location l2). Para cada par de posiciones que devuelvan true a la
	 * llamada isNear se crea una alarma que se añade a una lista de alarmas. Finalmente se pasa
	 * la lista de alarmas al parámetro de salida alarmsSB (StringBuilder).
	 *
	 * @param userId Usuario que ejecuta listAlarms.
	 * @param alarmsSB StringBuilder que se utiliza como parametro de salida para devolver la lista
	 *                 de alarmas.
	 */
	private static int listAlarms(int userId, Statement stmt, StringBuilder alarmsSB) throws SQLException {
		//Obtenemos la posición del usuario healthy y todas las últimas posiciones peligrosas
		Location healthyUserLocation = getUserLastLocation(userId, stmt);
		List<Location> dangerLocations = getDangerLocations(stmt);

		//Si userId es un suspect, eliminamos su posición de la lista para no crear una alarma consigo mismo.
		dangerLocations.remove(new Location(getUserLastLocationId(userId, stmt)));

		List<Alarm> healthyUserAlarmsList = new ArrayList();

		//Comparamos la posición del healthyUser con todas las pocisiones peligrosas
		//Si el método isNear devuelve true, creamos una nueva alarma y la añadimos a la lista.
		while(!dangerLocations.isEmpty()){
			Location currentDangerLocation = dangerLocations.get(0);
			if(healthyUserLocation.isNear(currentDangerLocation)){
				healthyUserAlarmsList.add(new Alarm(healthyUserLocation, currentDangerLocation));
			}
			dangerLocations.remove(0);
		}

		//Si se ha detectado alguna alarma, añadirla al StringBuilder alarms SB.
		if(!healthyUserAlarmsList.isEmpty()) {
			int alarmCount = healthyUserAlarmsList.size();
			String plural = ((alarmCount == 1) ? "" : "s").trim(); //Si hay más de una alarma, añadimos una s a "Alarm"
			alarmsSB.append(String.format("%d Alarm%s detected: %s", alarmCount, plural, healthyUserAlarmsList));
		}
		return 0;
	}

	/**
	 * Recupera todas las últimas posiciones de usuarios infectados y sospechosos (las posiciones
	 * peligrosas). Cada location la empaqueta en un objeto Location y la añade a una lista de
	 * Locations.
	 * @return Lista de Locations peligrosas.
	 */
	private static List<Location> getDangerLocations(Statement stmt) throws SQLException {
		List<Location> dangerLocationsList = new ArrayList();

		//subconsulta con referencias a todas ultimas posiciones de infectados o sospechosos:
		String dangerLocationIdsSubQuery = "SELECT lastLocation FROM User WHERE state = 1 OR state IS NULL";

		//Consultamos y añadimos todas esas Locations a la lista dangerLocationsList
		ResultSet dangerLocationsRS = stmt.executeQuery(String.format(
				"SELECT locationId, userId, time, latitude, longitude FROM Location WHERE locationId IN (%s)",
				dangerLocationIdsSubQuery));
		while(dangerLocationsRS.next()){
			dangerLocationsList.add(new Location(dangerLocationsRS));
		}

		return dangerLocationsList;
	}

	/**
	 * Dado un userId, devuelve un objeto Location correspondiente a su última posicion.
	 */
	private static Location getUserLastLocation(int userId, Statement stmt) throws SQLException {
		long locationId = getUserLastLocationId(userId, stmt);
		return getLocation(locationId, stmt);
	}

	/**
	 * Dado un locationId devuelve el objeto Location corresopndiente (basicamente un objeto
	 * con todos los campos de una Location).
	 */
	private static Location getLocation(long locationId, Statement stmt) throws SQLException {
		ResultSet locationRS = stmt.executeQuery(String.format(
				"SELECT locationId, userId, time, latitude, longitude FROM Location WHERE locationId = %d",locationId));
		locationRS.next();
		return new Location(locationRS);
	}

	/**
	 * Dado un userId, devuelve el locationId de su última posición.
	 */
	private static long getUserLastLocationId(int userId, Statement stmt) throws SQLException {
		ResultSet locationIdRS = stmt.executeQuery(String.format(
				"SELECT lastLocation FROM User WHERE userId = %d", userId));
		locationIdRS.next();
		return locationIdRS.getLong(1);
	}

	/**
	 * Comprueba si un usuario tiene alguna posición registrada. No tendrán ninguna posición
	 * los usuarios nuevos que no hayan añadido todavía ninguna posición.
	 */
	private static boolean userHasNoLocation(int userId, Statement stmt) throws SQLException {
		ResultSet lastLocationRS = stmt.executeQuery(String.format(
				"SELECT lastLocation FROM User WHERE userId = %d", userId));
		lastLocationRS.next();
		lastLocationRS.getLong(1);
		return lastLocationRS.wasNull();
	}


	/**
	 * Este metodo es de la interfaz Runnable. Envuelve el bloque que se ejecutara
	 * cuando se inice un Thread de tipo ServerInstance. (Antes este metodo era en
	 * realidad el metodo main de la clase server).
	 */
	public void run() {
		try {
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
							info1 = Integer.toString(getUserId(fields[2], stmt));
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
						case "startAlarms":
							res = startAlarms(fields, stmt);
							break;
						case "stopAlarms":
							res = stopAlarms(fields);
							break;
						case "infected":
							res = infected(fields, stmt);
							break;
						case "healthy":
							res = healthy(fields, stmt);
							break;
						//case "suspect":
						//	res = suspect(fields, stmt);
						//	break;
						case "listAlarms":
							res = listAlarms(fields, stmt, info3);
							break;
						case "closeContacts":
							res = checkCloseContacts(fields, stmt);
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
				out.println(msgToSend);
			}

			con.close();
			clientSocket.close();

		} catch(IOException|SQLException e){
			e.printStackTrace();
		}
	}

	//For debugging
	public static void main(String[] args) throws SQLException {
		Connection con = DriverManager.getConnection("jdbc:sqlite:Contactus.db");
		Statement stmt = con.createStatement();
		stmt.execute("PRAGMA foreign_keys = ON"); //enable foreign key behavior

		List<Location> list = new ArrayList<>();
		Object[] array = list.toArray();
		Arrays.sort(array);
		System.out.println(array.length);

	}

}