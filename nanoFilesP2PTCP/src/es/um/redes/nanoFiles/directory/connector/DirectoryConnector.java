package es.um.redes.nanoFiles.directory.connector;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.Set;

import es.um.redes.nanoFiles.client.application.NanoFiles;
import es.um.redes.nanoFiles.directory.message.DirMessage;

/**
 * Cliente con métodos de consulta y actualización específicos del directorio
 */
public class DirectoryConnector {
	/**
	 * Puerto en el que atienden los servidores de directorio
	 */
	private static final int DEFAULT_PORT = 6868;
	/**
	 * Tiempo máximo en milisegundos que se esperará a recibir una respuesta por el
	 * socket antes de que se deba lanzar una excepción SocketTimeoutException para
	 * recuperar el control
	 */
	private static final int TIMEOUT = 1000;
	/**
	 * Número de intentos máximos para obtener del directorio una respuesta a una
	 * solicitud enviada. Cada vez que expira el timeout sin recibir respuesta se
	 * cuenta como un intento.
	 */
	private static final int MAX_NUMBER_OF_ATTEMPTS = 5;

	/**
	 * Socket UDP usado para la comunicación con el directorio
	 */
	private DatagramSocket socket;
	/**
	 * Dirección de socket del directorio (IP:puertoUDP)
	 */
	private InetSocketAddress directoryAddress;

	public DirectoryConnector(String address) throws IOException {
		/*
		 * Crear el socket UDP para comunicación con el directorio durante el
		 * resto de la ejecución del programa, y guardar su dirección (IP:puerto) en
		 * atributos
		 */
		socket = new DatagramSocket();
		socket.setSoTimeout(TIMEOUT);
		directoryAddress = new InetSocketAddress(InetAddress.getByName(address), DEFAULT_PORT);
	}

	/**
	 * Método para enviar y recibir datagramas al/del directorio
	 * 
	 * @param requestData los datos a enviar al directorio (mensaje de solicitud)
	 * @return los datos recibidos del directorio (mensaje de respuesta)
	 */
	public byte[] sendAndReceiveDatagrams(byte[] requestData) {
		byte responseData[] = new byte[DirMessage.PACKET_MAX_SIZE];
		int attempts = 0;
		boolean done = false;
		/*
		 * Enviar datos en un datagrama al directorio y recibir una respuesta.
		 * Debe implementarse un mecanismo de reintento usando temporizador, en caso de
		 * que no se reciba respuesta en el plazo de TIMEOUT. En caso de salte el
		 * timeout, se debe reintentar como máximo en MAX_NUMBER_OF_ATTEMPTS ocasiones
		 */
		
		while(attempts < MAX_NUMBER_OF_ATTEMPTS && !done) {
			try {
				// ** SEND TO SERVER **
				DatagramPacket packetToServer = new DatagramPacket(requestData, requestData.length, directoryAddress);
				socket.send(packetToServer);
				// ** RECEIVE FROM SERVER **
				DatagramPacket packetFromServer = new DatagramPacket(responseData, responseData.length);
				socket.receive(packetFromServer);
				done = true;
			} catch (SocketTimeoutException e) {
				attempts++;
				System.err.println("Timeout: Attempt " + attempts + " of " + MAX_NUMBER_OF_ATTEMPTS);
				// System.exit(1);
			} catch (IOException e) {
				// Auto-generated catch block
				System.err.println("Error: " + e.getMessage());
				done = true;
			}
		}
		
		return responseData;
	}

	public int logIntoDirectory() { // Returns number of file servers
		byte[] requestData = DirMessage.buildLoginRequestMessage();
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processLoginResponse(responseData);
	}
	/*
	 * TODO: Crear un método distinto para cada intercambio posible de mensajes con
	 * el directorio, basándose en logIntoDirectory o registerNickname, haciendo uso
	 * de los métodos adecuados de DirMessage para construir mensajes de petición y
	 * procesar mensajes de respuesta
	 */
	public boolean registerNickname(String nick) {
		byte[] requestData = DirMessage.buildRegisterRequestMessage(nick);
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processRegisterResponseMessage(responseData);
	}
	
	public Set<String> getUserList(){
		byte[] requestData = DirMessage.buildUserListRequestMessage();
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processUserListResponse(responseData);
	}
	
	public InetSocketAddress getUserAddress(String nick) {
		byte[] requestData = DirMessage.buildUserLookupRequestMessage(nick);
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processUserLookupResponse(responseData);
	}
	
	public Set<String> getFileList(){
		byte[] requestData = DirMessage.buildFileListRequestMessage();
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processFileListResponseMessage(responseData);
	}
	
	public boolean serveFiles(int port, String nick) {
		byte[] requestData = DirMessage.buildServeFilesRequestMessage(port, nick, NanoFiles.db.getFiles());
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processServeFilesResponseMessage(responseData);
	}
	
	public void logout(String nick) {
		if(nick != null) {
			byte[] requestData = DirMessage.buildLogoutRequestMessage(nick);
			this.sendAndReceiveDatagrams(requestData);
		}
		socket.close();
	}
}
