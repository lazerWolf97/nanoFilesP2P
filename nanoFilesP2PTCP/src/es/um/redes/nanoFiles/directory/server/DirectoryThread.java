package es.um.redes.nanoFiles.directory.server;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import es.um.redes.nanoFiles.client.application.NanoFiles;
import es.um.redes.nanoFiles.directory.message.DirMessage;
import es.um.redes.nanoFiles.directory.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

public class DirectoryThread extends Thread {

	/**
	 * Socket de comunicación UDP con el cliente UDP (DirectoryConnector)
	 */
	protected DatagramSocket socket = null;

	/**
	 * Probabilidad de descartar un mensaje recibido en el directorio (para simular
	 * enlace no confiable y testear el código de retransmisión)
	 */
	protected double messageDiscardProbability;

	/**
	 * Estructura para guardar los nicks de usuarios registrados, y la fecha/hora de
	 * registro
	 * 
	 */
	private HashMap<String, LocalDateTime> nicks;
	/**
	 * Estructura para guardar los usuarios servidores (nick, direcciones de socket
	 * TCP)
	 */
	// TCP)
	private HashMap<String, InetSocketAddress> servers;
	/**
	 * Estructura para guardar la lista de ficheros publicados por todos los peers
	 * servidores, cada fichero identificado por su hash
	 */
	private HashMap<String, FileInfo> files;

	public DirectoryThread(int directoryPort, double corruptionProbability) throws SocketException {
		// Crear dirección de socket con el puerto en el que escucha el directorio
		InetSocketAddress addr = new InetSocketAddress(directoryPort);
		// Crear el socket UDP asociado a la dirección de socket anterior
		socket = new DatagramSocket(addr);
		messageDiscardProbability = corruptionProbability;
		nicks = new HashMap<String, LocalDateTime>();
		servers = new HashMap<String, InetSocketAddress>();
		files = new HashMap<String, FileInfo>();
	}

	public void run() {
		byte[] receptionBuffer = new byte[DirMessage.PACKET_MAX_SIZE];
		DatagramPacket requestPacket = new DatagramPacket(receptionBuffer, receptionBuffer.length);
		InetSocketAddress clientId = null;

		System.out.println("Directory starting...");

		while (true) {
			try {

				// Recibimos a través del socket el datagrama con mensaje de solicitud
				socket.receive(requestPacket);

				// Averiguamos quién es el cliente
				clientId = (InetSocketAddress) requestPacket.getSocketAddress();

				// Vemos si el mensaje debe ser descartado por la probabilidad de descarte
				double rand = Math.random();
				if (rand < messageDiscardProbability) {
					System.err.println("Directory DISCARDED datagram from " + clientId);
					continue;
				}

				// Analizamos la solicitud y la procesamos

				if (requestPacket.getData().length > 0) {
					processRequestFromClient(requestPacket.getData(), clientId);
				} else {
					System.err.println("Directory received EMPTY datagram from " + clientId);
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Directory received EMPTY datagram from " + clientId);				
				break;
			}
		}
		// Cerrar el socket
		socket.close();
	}

	// Método para procesar la solicitud enviada por clientAddr
	public void processRequestFromClient(byte[] data, InetSocketAddress clientAddr) throws IOException {
		// Construir un objeto mensaje (DirMessage) a partir de los datos recibidos
		// PeerMessage.buildMessageFromReceivedData(data)
		boolean status;
		DirMessage msg = DirMessage.buildMessageFromReceivedData(data);
		
		// TODO: Actualizar estado del directorio y enviar una respuesta en función del
		// tipo de mensaje recibido
		switch(msg.getOpcode()) {
			case DirMessageOps.OPCODE_LOGIN:
				this.sendLoginOK(clientAddr);
			break;
			case DirMessageOps.OPCODE_REGISTER_USERNAME:
				status = register(msg.getUserName());
				this.sendRegisterStatus(status, clientAddr);
			break;
			case DirMessageOps.OPCODE_GETUSERS:
				this.sendUserList(clientAddr);
			break;
			case DirMessageOps.OPCODE_LOOKUP_USERNAME:
				this.sendUserAddress(msg.getUserName(), clientAddr);
			break;
			case DirMessageOps.OPCODE_GETFILES:
				this.sendFileList(clientAddr);
			break;
			case DirMessageOps.OPCODE_SERVE_FILES:
				status = serveFiles(msg.getUserName(), msg.getPort(), msg.getFiles(), clientAddr);
				this.sendServingConfirmation(status, clientAddr);
			break;
			case DirMessageOps.OPCODE_LOGOUT:
				nicks.remove(msg.getUserName());
				this.sendLogoutOK(clientAddr);
			break;
		}
	}



	// Método para enviar la confirmación del registro
	private void sendLoginOK(InetSocketAddress clientAddr) throws IOException {
		// Construir el datagrama con la respuesta y enviarlo por el socket al cliente
		byte[] responseData = DirMessage.buildLoginOKResponseMessage(servers.size());
		DatagramPacket packet = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(packet);
	}
	
	private void sendLogoutOK(InetSocketAddress clientAddr) throws IOException {
		// Construir el datagrama con la respuesta y enviarlo por el socket al cliente
		byte[] responseData = DirMessage.buildLogoutResponseMessage();
		DatagramPacket packet = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(packet);
	}
	
	private void sendRegisterStatus(boolean status, InetSocketAddress clientAddr) throws IOException {
		byte[] responseData = DirMessage.buildRegisterResponseMessage(status);
		DatagramPacket packet = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(packet);
	}
	
	private void sendUserList(InetSocketAddress clientAddr) throws IOException {
		byte[] responseData = DirMessage.buildUserListResponseMessage(nicks.keySet());
		DatagramPacket packet = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(packet);
	}
	
	private void sendUserAddress(String nick, InetSocketAddress clientAddr) throws IOException {
		byte[] responseData = DirMessage.buildUserLookupResponseMessage(servers.get(nick));
		DatagramPacket packet = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(packet);
	}
	
	private void sendFileList(InetSocketAddress clientAddr) throws IOException {
		Set<FileInfo> fi = new HashSet<FileInfo>(files.values());
		Set<String> fset = new HashSet<String>();
		fi.stream().forEach(f -> {
			fset.add(f.fileName + ";" + f.fileSize + ";" + f.fileHash);
		});
		byte[] responseData = DirMessage.buildFileListResponseMessage(fset);
		DatagramPacket packet = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(packet);
	}
	
	private void sendServingConfirmation(boolean status, InetSocketAddress clientAddr) throws IOException {
		byte[] responseData = DirMessage.buildServeFilesResponseMessage(status);
		DatagramPacket packet = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(packet);
	}

	private boolean register(String nick) {
		if(nicks.containsKey(nick)) {
			return false;
		}
		else {
			nicks.put(nick, LocalDateTime.now());
			System.out.println("Registered[" + nick + ", " + nicks.get(nick) + "] Tam: " + nick.length());
			return true;
		}
	}
	
	private boolean serveFiles(String userName, int port, FileInfo[] fileset, InetSocketAddress clientAddr) {
		if(servers.containsKey(userName)) return false;
		InetSocketAddress addr = new InetSocketAddress(clientAddr.getHostName(), port);
		servers.put(userName, addr);
		for(FileInfo fi : fileset) {
			files.putIfAbsent(fi.fileHash, fi);
		}
		return true;
	}
	
}
