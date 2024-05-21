package es.um.redes.nanoFiles.client.application;

import java.io.IOException;
import java.net.InetSocketAddress;

import es.um.redes.nanoFiles.directory.connector.DirectoryConnector;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFControllerLogicDir {
	// Conector para enviar y recibir mensajes del directorio
	private DirectoryConnector directoryConnector;

	/**
	 * Método para conectar con el directorio y obtener el número de peers que están
	 * sirviendo ficheros
	 * 
	 * @param directoryHostname el nombre de host/IP en el que se está ejecutando el
	 *                          directorio
	 * @return true si se ha conseguido contactar con el directorio.
	 */
	boolean logIntoDirectory(String directoryHostname) {
		/*
		 * Debe crear un objeto DirectoryConnector a partir del parámetro
		 * directoryHostname y guardarlo en el atributo correspondiente. A continuación,
		 * utilizarlo para comunicarse con el directorio y realizar tratar de realizar
		 * el "login", informar por pantalla del éxito/fracaso y devolver dicho valor
		 */
		boolean result = false;
		try {
			directoryConnector = new DirectoryConnector(directoryHostname);
			result = (directoryConnector.logIntoDirectory() != -1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Método para registrar el nick del usuario en el directorio
	 * 
	 * @param nickname el nombre de usuario a registrar
	 * @return true si el nick es válido (no contiene ":") y se ha registrado
	 *         nickname correctamente con el directorio (no estaba duplicado), falso
	 *         en caso contrario.
	 */
	boolean registerNickInDirectory(String nickname) {
		/*
		 * Registrar un nick. Comunicarse con el directorio (a través del
		 * directoryConnector) para solicitar registrar un nick. Debe informar por
		 * pantalla si el registro fue exitoso o fallido, y devolver dicho valor
		 * booleano. Se debe comprobar antes que el nick no contiene el carácter ':'.
		 */
		boolean result = false;
		if(!nickname.contains(":")) {
			result = directoryConnector.registerNickname(nickname);
		} else {
			System.out.println("Error: The nickname cannot contain the \":\" character.");
		}
		if(result) System.out.println("Nickname " + nickname + " successfully registered.");
		else System.out.println("Nickname " + nickname + " couldn\'t be registered.");
		return result;
	}

	/**
	 * Método para obtener de entre los peer servidores registrados en el directorio
	 * la IP:puerto del peer con el nick especificado
	 * 
	 * @param nickname el nick del peer por cuya IP:puerto se pregunta
	 * @return La dirección de socket del peer identificado por dich nick, o null si
	 *         no se encuentra ningún peer con ese nick.
	 */
	InetSocketAddress lookupUserInDirectory(String nickname) {
		/*
		 * Obtener IP:puerto asociada a nickname. Comunicarse con el directorio (a
		 * través del directoryConnector) para preguntar la dirección de socket en la
		 * que el peer con 'nickname' está sirviendo ficheros. Si no se obtiene una
		 * respuesta con IP:puerto válidos, se debe devolver null.
		 */
		InetSocketAddress peerAddr = directoryConnector.getUserAddress(nickname);
		return peerAddr;
	}

	/**
	 * Método para publicar la lista de ficheros que este peer está compartiendo.
	 * 
	 * @param port     El puerto en el que este peer escucha solicitudes de conexión
	 *                 de otros peers.
	 * @param nickname El nick de este peer, que será asociado a lista de ficheros y
	 *                 su IP:port
	 */
	void publishLocalFilesToDirectory(int port, String nickname) {
		/*
		 * Enviar la lista de ficheros servidos. Comunicarse con el directorio (a
		 * través del directoryConnector) para enviar la lista de ficheros servidos por
		 * este peer con nick 'nickname' en el puerto 'port'. Los ficheros de la carpeta
		 * local compartida están disponibles en NanoFiles.db).
		 */
		boolean status = directoryConnector.serveFiles(port, nickname);
		if(status) {
			System.out.println("Files served successfully.");
		}
		else {
			System.out.println("Error serving files.");
		}
	}

	/**
	 * Método para obtener y mostrar la lista de nicks registrados en el directorio
	 */
	void getUserListFromDirectory() {
		/*
		 * Obtener la lista de usuarios registrados. Comunicarse con el directorio
		 * (a través del directoryConnector) para obtener la lista de nicks registrados
		 * e imprimirla por pantalla.
		 */
		System.out.println("Registered users:");
		directoryConnector.getUserList().stream().forEach(
				n -> {
					System.out.println("- " + n);
				});
		System.out.println(directoryConnector.getUserList().size() + " users registered.");
	}

	/**
	 * Método para obtener y mostrar la lista de ficheros que los peer servidores
	 * han publicado al directorio
	 */
	void getFileListFromDirectory() {
		/*
		 * Obtener la lista de ficheros servidos. Comunicarse con el directorio (a
		 * través del directoryConnector) para obtener la lista de ficheros e imprimirla
		 * por pantalla.
		 */
		System.out.println("Served files:");
		StringBuffer strBuf = new StringBuffer();
		
		strBuf.append(String.format("%1$-30s", "Name"));
		strBuf.append(String.format("%1$10s", "Size"));
		strBuf.append(String.format(" %1$-45s", "Hash"));
		System.out.println(strBuf);
		directoryConnector.getFileList().stream().forEach(f -> {
			int index1 = f.indexOf(';');
			int index2 = f.lastIndexOf(';');
			String name = f.substring(0, index1);
			String size = f.substring(index1 + 1, index2);
			String hash = f.substring(index2 + 1);
			StringBuffer strBuf2 = new StringBuffer();
			strBuf2.append(String.format("%1$-30s", name));
			strBuf2.append(String.format("%1$10s", size));
			strBuf2.append(String.format(" %1$-45s", hash));
			System.out.println(strBuf2);
		});
		System.out.println(directoryConnector.getFileList().size() + " files served.");
	}
	
	/**
	 * Método para desconectarse del directorio (cerrar sesión) 
	 */
	public void logout(String nick) {
		/*
		 * TODO: Dar de baja el nickname. Al salir del programa, se debe dar de baja el
		 * nick registrado con el directorio y cerrar el socket usado por el
		 * directoryConnector.
		 */
		if(directoryConnector != null) directoryConnector.logout(nick);
	}
}
