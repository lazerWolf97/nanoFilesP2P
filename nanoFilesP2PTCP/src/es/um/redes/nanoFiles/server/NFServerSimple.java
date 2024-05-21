package es.um.redes.nanoFiles.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class NFServerSimple {

	private static final int SERVERSOCKET_ACCEPT_TIMEOUT_MILISECS = 1000;
	private static final String STOP_SERVER_COMMAND = "fgstop";
	private static final int TIMEOUT_SECONDS = 60;
	private ServerSocket serverSocket = null;

	public NFServerSimple(int port) throws IOException {
		/*
		 * Crear una direción de socket a partir del puerto especificado
		 */
		InetSocketAddress FileServerSocketAddress = new InetSocketAddress(port);
		/*
		 * Crear un socket servidor y ligarlo a la dirección de socket anterior
		 */
		serverSocket = new ServerSocket();
		
		serverSocket.bind(FileServerSocketAddress);
		System.out.println("\nServer is listening on port " + port);
		/*
		 * (Opcional) Establecer un timeout para que el método accept no espere
		 * indefinidamente
		 */
		serverSocket.setSoTimeout(SERVERSOCKET_ACCEPT_TIMEOUT_MILISECS);
	}

	/**
	 * Método para ejecutar el servidor de ficheros en primer plano. Sólo es capaz
	 * de atender una conexión de un cliente. Una vez se lanza, ya no es posible
	 * interactuar con la aplicación a menos que se implemente la funcionalidad de
	 * detectar el comando STOP_SERVER_COMMAND (opcional)
	 * 
	 */
	public void run() {
		// Comprobar que el socket servidor está creado y ligado
		if (serverSocket == null) {
			System.err.println("Socket doesn't exist.");
			return;
		}
		if(serverSocket.isClosed()) {
			System.err.println("Socket isn't bound.");
			return;
		}

		boolean stopServer = false;
		int seconds = 0;
		System.out.println("Enter '" + STOP_SERVER_COMMAND + "' to stop the server");
		while (!stopServer) {
			/*
			 * Usar el socket servidor para esperar conexiones de otros peers que
			 * soliciten descargar ficheros
			 */
			try {
				/*
				 * Al establecerse la conexión con un peer, la comunicación con dicho
				 * cliente se hace en el método NFServerComm.serveFilesToClient(socket), al cual
				 * hay que pasarle el objeto Socket devuelto por accept (retorna un nuevo socket
				 * para hablar directamente con el nuevo cliente conectado)
				 */
				Socket socket = serverSocket.accept();
				System.out.println("\nNew client connected: " +
						socket.getInetAddress().toString() + ":" + socket.getPort());
				NFServerComm.serveFilesToClient(socket);
			/*
			 * TODO: (Para poder detener el servidor y volver a aceptar comandos).
			 * Establecer un temporizador en el ServerSocket antes de ligarlo, para
			 * comprobar mediante standardInput.ready()) periódicamente si se ha tecleado el
			 * comando "fgstop", en cuyo caso se cierra el socket servidor y se sale del
			 * bucle
			 */
				
				
				
			} catch (SocketTimeoutException ex) {
				seconds++;
				if(seconds == TIMEOUT_SECONDS) {
					System.err.println("Timeout.");
					stopServer = true;
				}
			} catch (IOException ex) {
				System.err.println("Server exception: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
		System.out.println("NFServerSimple stopped");
		try {
			serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}
}
