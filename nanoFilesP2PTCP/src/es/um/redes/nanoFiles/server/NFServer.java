package es.um.redes.nanoFiles.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Servidor que se ejecuta en un hilo propio. Creará objetos
 * {@link NFServerThread} cada vez que se conecte un cliente.
 */
public class NFServer implements Runnable {

	private ServerSocket serverSocket = null;

	public NFServer(int port) throws IOException {
		/*
		 * Crear una direción de socket a partir del puerto especificado
		 */
		InetSocketAddress addr = new InetSocketAddress(port);
		
		/*
		 * Crear un socket servidor y ligarlo a la dirección de socket anterior
		 */
		serverSocket = new ServerSocket();
		serverSocket.bind(addr);
	}

	/**
	 * Método que ejecuta el hilo principal del servidor (creado por startServer).
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while (true) {
			try {
			/*
			 * Usar el socket servidor para esperar conexiones de otros peers que
			 * soliciten descargar ficheros
			 */
				Socket socket = serverSocket.accept();
				System.out.println("\nNew client connected: " +
						socket.getInetAddress().toString() + ":" + socket.getPort());
			
			/*
			 * Al establecerse la conexión con un peer, la comunicación con dicho
			 * cliente se hace en el método NFServerComm.serveFilesToClient(socket), al cual
			 * hay que pasarle el objeto Socket devuelto por accept (retorna un nuevo socket
			 * para hablar directamente con el nuevo cliente conectado)
			 */
				NFServerComm.serveFilesToClient(socket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Método que crea un hilo de esta clase y lo ejecuta en segundo plano,
	 * empezando por el método "run".
	 */
	public void startServer() {
		new Thread(this).start();
	}

	/**
	 * Método que detiene el servidor, cierra el socket servidor y termina los hilos
	 * que haya ejecutándose
	 */
	public void stopServer() {
		try {
			serverSocket.close();
			Thread.currentThread().interrupt();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
