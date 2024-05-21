package es.um.redes.nanoFiles.client.application;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import es.um.redes.nanoFiles.client.comm.NFConnector;
import es.um.redes.nanoFiles.server.NFServer;
import es.um.redes.nanoFiles.server.NFServerSimple;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFControllerLogicP2P {
	/**
	 * El servidor de ficheros de este peer
	 */
	private NFServer bgFileServer = null;
	/**
	 * El cliente para conectarse a otros peers
	 */
	NFConnector nfConnector;
	/**
	 * El controlador que permite interactuar con el directorio
	 */
	private NFControllerLogicDir controllerDir;

	protected NFControllerLogicP2P() {
	}

	protected NFControllerLogicP2P(NFControllerLogicDir controller) {
		// Referencia al controlador que gestiona la comunicación con el directorio
		controllerDir = controller;
	}

	/**
	 * Método para ejecutar un servidor de ficheros en primer plano. Debe arrancar
	 * el servidor y darse de alta en el directorio para publicar el puerto en el
	 * que escucha.
	 * 
	 * 
	 * @param port     El puerto en que el servidor creado escuchará conexiones de
	 *                 otros peers
	 * @param nickname El nick de este peer, parar publicar los ficheros al
	 *                 directorio
	 */
	protected void foregroundServeFiles(int port, String nickname) {
		/*
		 * Las excepciones que puedan lanzarse deben ser capturadas y tratadas en
		 * este método. Si se produce una excepción de entrada/salida (error del que no
		 * es posible recuperarse), se debe informar sin abortar el programa
		 */
		
		// Crear objeto servidor NFServerSimple ligado al puerto especificado
		
		try {
			NFServerSimple fgServer = new NFServerSimple(port);

		// Publicar ficheros compartidos al directorio
			controllerDir.publishLocalFilesToDirectory(port, nickname);

		// Ejecutar servidor en primer plano
			fgServer.run();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Método para ejecutar un servidor de ficheros en segundo plano. Debe arrancar
	 * el servidor y darse de alta en el directorio para publicar el puerto en el
	 * que escucha.
	 * 
	 * @param port     El puerto en que el servidor creado escuchará conexiones de
	 *                 otros peers
	 * @param nickname El nick de este peer, parar publicar los ficheros al
	 *                 directorio
	 */
	protected void backgroundServeFiles(int port, String nickname) {
		/*
		 * Las excepciones que puedan lanzarse deben ser capturadas y tratadas en
		 * este método. Si se produce una excepción de entrada/salida (error del que no
		 * es posible recuperarse), se debe informar sin abortar el programa
		 */
		try {
		// Comprobar que no existe ya un objeto NFServer previamente creado, en
		// cuyo caso el servidor ya está en marcha
			if(bgFileServer != null) {
				System.out.println("Server is already functioning.");
				return;
			}

		// Crear objeto servidor NFServer ligado al puerto especificado
			bgFileServer = new NFServer(port);

		// Arrancar un hilo servidor en segundo plano
			bgFileServer.startServer();

		// Publicar ficheros compartidos al directorio
			controllerDir.publishLocalFilesToDirectory(port, nickname);

		// Imprimir mensaje informando de que el servidor está en marcha
			System.out.println("Server successfully launched.");
		} catch (IOException e) {
			System.err.println("IOException.");
			e.printStackTrace();
		}
	}

	/**
	 * Método para establecer una conexión con un peer servidor de ficheros
	 * 
	 * @param nickname El nick del servidor al que conectarse (o su IP:puerto)
	 * @return true si se ha podido establecer la conexión
	 */
	protected boolean browserEnter(String nickname) {
		InetSocketAddress addr; 
		boolean validIP = true;
		boolean validPort;
		if(nickname == null || nickname.isBlank()) return false;
		int index = nickname.indexOf(":");
		if(index != -1) {
			String ip = nickname.substring(0, index);
			String port = nickname.substring(index + 1).trim();
			
			if(!ip.equals("localhost")) validIP = ip.matches("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");
			validPort = port.matches("^\\d{1,5}$");
			if(validIP && validPort){
				addr = new InetSocketAddress(ip, Integer.parseInt(port));
			}
			else {
				addr = controllerDir.lookupUserInDirectory(nickname);
				
				if(addr == null) {
					System.out.println("Not a valid user.");
					return false;
				}
			}
		}
		else {
			/*
			 * Si es un nickname, preguntar al directorio la IP:puerto asociada a
			 * dicho peer servidor.
			 */
			addr = controllerDir.lookupUserInDirectory(nickname);
			
			/*
			 * Comprobar si la respuesta del directorio contiene una IP y puerto
			 * válidos (el peer servidor al que nos queremos conectar ha comunicado
			 * previamente al directorio el puerto en el que escucha). En caso contrario,
			 * informar y devolver falso.
			 */
			if(addr == null) {
				System.out.println("Not a valid user.");
				return false;
			}
		}
		
		/*
		 * Crear un objeto NFConnector para establecer la conexión con el peer
		 * servidor de ficheros. Si la conexión se establece con éxito, informar y
		 * devolver verdadero.
		 */
		try {
			nfConnector = new NFConnector(addr);
			System.out.println("Connection successfully stablished.");
		} catch (UnknownHostException e) {
			System.err.println("Unknown host.");
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Método para descargar un fichero del peer servidor de ficheros al que nos
	 * hemos conectador mediante browser Enter
	 * 
	 * @param targetFileHash El hash del fichero a descargar
	 * @param localFileName  El nombre con el que se guardará el fichero descargado
	 */
	protected void browserDownloadFile(String targetFileHash, String localFileName) {
		/*
		 * Usar el NFConnector creado por browserEnter para descargar el fichero
		 * mediante el método "download". Se debe comprobar si ya existe un fichero con
		 * el mismo nombre en esta máquina, en cuyo caso se informa y no se realiza la
		 * descarga
		 */
		File f;
		for(FileInfo fi : NanoFiles.db.getFiles()) {
			if(fi.fileName.equals(localFileName)) {
				System.out.println("File " + localFileName + " already exists.");
				return;
			}
		}
		try {
			f = new File(localFileName);
			nfConnector.download(targetFileHash, f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	protected void browserClose() {
		/*
		 * TODO: Cerrar el explorador de ficheros remoto (informar al servidor de que se
		 * va a desconectar)
		 */
		
	}

	protected void browserQueryFiles() {
		/*
		 * TODO: Crear un objeto NFConnector y guardarlo el atributo correspondiente
		 * para ser usado por otros métodos de esta clase mientras se está en una sesión
		 * del explorador de ficheros remoto.
		 * 
		 */
		
	}
}
