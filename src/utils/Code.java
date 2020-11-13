package utils;

import java.util.HashMap;
import java.util.Map;

public class Code {

public static Map<String, String> codes = new HashMap();
static {
	codes.put("0", "success");

	codes.put("1", "ignored");

	codes.put("4", "failure");
	codes.put("41", "userExists");
	codes.put("42", "wrongNumberOfArguments");

	codes.put("3", "exit");
}

}