package utils;

import java.util.HashMap;
import java.util.Map;

public class Code {

public static Map<String, String> codes = new HashMap();
static {
	codes.put("0", "success");

	codes.put("-1", "ignored");

	codes.put("-4", "failure");

	//errores de parametros y argumentos enviados por el cliente
	codes.put("-42", "wrongNumberOfArguments");
	codes.put("-421", "wrongArgumentTypes");

	//errores de usuarios y sesiones
	codes.put("-41", "userExists");
	codes.put("-43", "userAlreadyLoggedIn");
	codes.put("-44", "authenticationFailure");
	codes.put("-45", "userNotFound");
	codes.put("-46", "userNotAuthenticated");

	//errores de contactos cercanos, infectados y covid en general
	codes.put("-471", "userAlreadyInfected");
	codes.put("-472", "userAlreadyHealthy");
	codes.put("-473", "userAlreadySuspect");
	codes.put("-481", "suspectStateNotAllowedFromInfected"); //No permitimos pasar de infected a suspect
	codes.put("-482", "infectedUsersDontHaveAlarms"); //No permitimos a usuarios infectados hacer el listAlarms

	//errores de posiciones
	codes.put("-491", "userHasNotRegisteredALocation"); //Usuario no tiene ningúna location asociada

	codes.put("-47", "permissionDenied");

}

}