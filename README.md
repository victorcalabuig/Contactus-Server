ISO_lated Project
========================
Welcome to the **ISO_lated** team project.
==========================================

El proyecto "CONTACTus" aquí implementado facilita a todos sus usuarios 
registrados un servicio de seguridad sanitaria contra el virus, 
informándonos cada vez que tuviéramos un contacto contagiado cerca.

En este sistema se permite la interactuación de diferentes participantes, 
donde podremos diferenciar un administrador principal y multitud de 
usuarios externos, a los que nos referiremos en adelante como "cliente".

Estos usuarios registrados como "cliente" podrán recibir una alarma de 
advertencia tan pronto el sistema detecte una situación de riesgo debida 
a que algún otro usuario, también registrado y que haya indicado estar 
infectado por el virus, se encuentre a una distancia inferior a un límite 
de seguridad establecido. 

Esta función tan util precisa a cambio, por supuesto, que cada usuario 
"cliente" comunique secuancialmente su posición y su estado presente de 
infección respecto al virus.

Comunicando con el sistema a través de la unidad terminal, éste nos ofrece 
acceso a sus diferentes funciones a partir de los comandos introducidos:

    - Función de LOGIN (Necesaria para optar al resto de comandos):
            >"login+[USER_NAME]+[USER_PASSWORD]"
        A través de la introducción de un comando acorde a la 
        sintaxis anterior, se identifica el usuario en 
        comunicación con el sistema.

	- Función de LOGOUT:
            >"logout"
        A través de la introducción de un comando acorde a la 
        sintaxis anterior, el sistema dejará de atender al usuario 
        activo.

	- Función de LISTUSERS:
            >"listUsers"
        A través de la introducción de un comando acorde a la 
        sintaxis anterior, el sistema imprimirá por terminal los 
        usuarios registrados y almacenados en su base de datos.

	- Función de LISTPOSITIONS:
            >"listPositions+[USER_NAME]+[#{s,m,h,d,w,p,y}]"
        A través de la introducción de un comando acorde a la 
        sintaxis anterior, el sistema dejará de atender al usuario 
        que introduzca el comando.
        
    - Función de STARTPOSITIONS:
            >"startPositions"
        A través de la introducción de un comando acorde a la 
        sintaxis anterior, el sistema activará un nuevo thread con 
        el que indicar secuencialmente sus coordenadas al sistema.

	- Función de STOPPOSITIONS:
            >"stopPositions"
        A través de la introducción de un comando acorde a la 
        sintaxis anterior, el sistema detendrá el thread con 
        encargado de comunicar secuencialmente la posición del 
        usuario.

    - Función ADDPOSITION:
            >"addPosition+[USER_ID]+[LATITUD]+[LONGITUD]"
        A través de la introducción de un comando acorde a la 
        sintaxis anterior, el sistema añade una posición a la base 
        de datos asociada al usuario que introduzca el comando.

    - Función de INFECTED:
            >"infected dd/MM/aaaa hh:mm"
        A través de la introducción de un comando acorde a la 
        sintaxis anterior, el sistema actualizará el parámetro 
        de salud propio del "cliente" como positivo, para dejar 
        constancia del evento.
        En caso de que el momento indicado en el comando sea 
        anterior al actual, el sistema se encargará de procesar 
        la evolución de su posición desde el momento indicado y 
        notificará las alarmas retrasadas a los usuarios que 
        hubieran estado próximos al "cliente" ahora expuesto como
        infectado.

    - Función de HEALTHY:
            >"healthy dd/MM/aaaa hh:mm"
        A través de la introducción de un comando acorde a la 
        sintaxis anterior, el sistema actualizará el parámetro 
        de salud propio del "cliente" como negativo, para dejar 
        constancia del evento.
        En caso de que el momento indicado en el comando sea 
        anterior al actual, el sistema se encargará de procesar 
        la base de datos donde se registran las posiciones de los
        "clientes" y rectificar como sano el parámetro de salud 
        del "cliente" activo desde el momento indicado en el 
        comando.

    - Función de LISTALARMS:
            >"listAlarms"
        A través de la introducción de un comando acorde a la 
        sintaxis anterior, el sistema activará un nuevo thread con 
        el que indicar secuencialmente al "cliente" activo, en 
        caso de haber algún otro cliente con estado de salud de 
        infectado cerca, la distancia que presenta respecto al él, 
        con un mensaje de aviso tal que:
            (alarm #m)	Donde '#' indicaría la distancia 
                    en metros con el cliente positivo.
            

    - Función de EXIT:
			>"exit"
		A través de la introducción de un comando acorde a la 
		sintaxis anterior, el sistema detendrá su ejecución.	