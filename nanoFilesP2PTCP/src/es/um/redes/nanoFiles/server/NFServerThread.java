package es.um.redes.nanoFiles.server;

import java.io.IOException;
import java.net.Socket;

public class NFServerThread extends Thread {
	/*
	 * TODO: Esta clase modela los hilos que son creados desde NFServer y cada uno
	 * de los cuales simplemente se encarga de invocar a
	 * NFServerComm.serveFilesToClient con el socket retornado por el método accept
	 * (un socket distinto para "conversar" con un cliente)
	 */
	
	private Socket socket = null;
	
	public NFServerThread(Socket socket) throws IOException {
		this.socket = socket;
	}
	
	
	public void run() {
		NFServerComm.serveFilesToClient(socket);
	}
	
}
