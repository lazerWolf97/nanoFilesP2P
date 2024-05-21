package es.um.redes.nanoFiles.directory.message;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import es.um.redes.nanoFiles.util.FileInfo;


public class DirMessage {

	public static final int PACKET_MAX_SIZE = 65536;

	public static final byte OPCODE_SIZE_BYTES = 1;
	
	public static final int BYTE_MAX_VALUE = 255;

	private byte opcode;

	private String userName;
	
	private Integer port;
	
	private FileInfo[] fi;

	public DirMessage(byte operation) {
		opcode = operation;
	}


	/*
	 * Añadir atributos y crear otros constructores específicos para crear
	 * mensajes con otros tipos de datos
	 * 
	 */
	public DirMessage(byte operation, String nick) {
		opcode = operation;
		userName = nick;
	}
	
	public DirMessage(byte operation, String nick, int port) {
		opcode = operation;
		userName = nick;
		this.port = port;
	}
	
	public DirMessage(byte operation, String nick, int port, FileInfo[] files) {
		opcode = operation;
		userName = nick;
		this.port = port;
		this.fi = files;
	}
	
	/**
	 * Método para obtener el tipo de mensaje (opcode)
	 * @return
	 */
	public byte getOpcode() {
		return opcode;
	}

	public String getUserName() {
		if (userName == null) {
			System.err.println(
					"PANIC: DirMessage.getUserName called but 'userName' field is not defined for messages of type "
							+ DirMessageOps.opcodeToOperation(opcode));
			System.exit(-1);
		}
		return userName;
	}
	
	public int getPort() {
		if (port == null) {
			System.err.println(
					"PANIC: DirMessage.getPort called but 'port' field is not defined for messages of type "
							+ DirMessageOps.opcodeToOperation(opcode));
			System.exit(-1);
		}
		return port;
	}
	
	public FileInfo[] getFiles() {
		if (fi == null) {
			System.err.println(
					"PANIC: DirMessage.getFiles called but 'files' field is not defined for messages of type "
							+ DirMessageOps.opcodeToOperation(opcode));
			System.exit(-1);
		}
		return fi;
	}


	/**
	 * Método de clase para parsear los campos de un mensaje y construir el objeto
	 * DirMessage que contiene los datos del mensaje recibido
	 * 
	 * @param data El
	 * @return
	 */
	public static DirMessage buildMessageFromReceivedData(byte[] data) {
		/*
		 * En función del tipo de mensaje, parsear el resto de campos para extraer
		 * los valores y llamar al constructor para crear un objeto DirMessage que
		 * contenga en sus atributos toda la información del mensaje
		 */
		ByteBuffer bb = ByteBuffer.wrap(data);
		byte opcode = bb.get();
		DirMessage msg;
		switch(opcode) {
			case DirMessageOps.OPCODE_LOGIN:
				msg = new DirMessage(opcode);
			break;
			case DirMessageOps.OPCODE_REGISTER_USERNAME:
				byte size = bb.get();
				byte[] b = new byte[size];
				bb.get(b);
				msg = new DirMessage(opcode, new String(b));
			break;
			case DirMessageOps.OPCODE_GETUSERS:
				msg = new DirMessage(opcode);
			break;
			case DirMessageOps.OPCODE_LOOKUP_USERNAME:
				byte size_ = bb.get();
				byte[] b_ = new byte[size_];
				bb.get(b_);
				int port_ = bb.getInt();
				msg = new DirMessage(opcode, new String(b_), port_);
				
			break;
			case DirMessageOps.OPCODE_GETFILES:
				msg = new DirMessage(opcode);
			break;
			case DirMessageOps.OPCODE_LOGOUT:
				byte size___ = bb.get();
				byte[] b___ = new byte[size___];
				bb.get(b___);
				msg = new DirMessage(opcode, new String(b___));
			break;
			case DirMessageOps.OPCODE_SERVE_FILES:
				byte size__ = bb.get();
				byte[] b__ = new byte[size__];
				bb.get(b__);
				int port__ = bb.getInt();
				int tamSet = bb.getInt();
				FileInfo[] files = new FileInfo[tamSet];
				for(int i = 0; i < tamSet; i++) {
					byte hashsize = bb.get();
					byte[] hash = new byte[hashsize];
					bb.get(hash);
					
					byte namesize = bb.get();
					byte[] name = new byte[namesize];
					bb.get(name);
					
					byte pathsize = bb.get();
					byte[] path = new byte[pathsize];
					bb.get(path);
					
					int filesize = bb.getInt();
					files[i] = new FileInfo(new String(hash), new String(name), filesize, new String(path));
				}
				msg = new DirMessage(opcode, new String(b__), port__, files);
			break;
			default:
				msg = null;
		}
		return msg;
	}

	/**
	 * Método para construir una solicitud de ingreso en el directorio
	 * 
	 * @return El array de bytes con el mensaje de solicitud de login
	 */
	public static byte[] buildLoginRequestMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_LOGIN);
		return bb.array();
	}

	/**
	 * Método para construir una respuesta al ingreso del peer en el directorio
	 * 
	 * @param numServers El número de peer registrados como servidor en el
	 *                   directorio
	 * @return El array de bytes con el mensaje de solicitud de login
	 */
	public static byte[] buildLoginOKResponseMessage(int numServers) {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES + Integer.BYTES);
		bb.put(DirMessageOps.OPCODE_LOGIN_OK);
		bb.putInt(numServers);
		return bb.array();
	}

	/**
	 * Método que procesa la respuesta a una solicitud de login
	 * 
	 * @param data El mensaje de respuesta recibido del directorio
	 * @return El número de peer servidores registrados en el directorio en el
	 *         momento del login, o -1 si el login en el servidor ha fallado
	 */
	public static int processLoginResponse(byte[] data) {
		ByteBuffer buf = ByteBuffer.wrap(data);
		byte opcode = buf.get();
		if (opcode == DirMessageOps.OPCODE_LOGIN_OK) {
			return buf.getInt(); // Return number of available file servers
		} else {
			return -1;
		}
	}
	
	public static byte[] buildRegisterRequestMessage(String nick) {
		byte[] cadena = nick.getBytes();
		byte longitud = (byte) nick.length();
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES * 2 + longitud);
		bb.put(DirMessageOps.OPCODE_REGISTER_USERNAME);
		bb.put(longitud);
		bb.put(cadena);
		return bb.array();
	}
	
	public static byte[] buildRegisterResponseMessage(boolean success) {
		ByteBuffer bb = ByteBuffer.allocate(OPCODE_SIZE_BYTES);
		if(success) bb.put(DirMessageOps.OPCODE_REGISTER_USERNAME_OK);
		else bb.put(DirMessageOps.OPCODE_REGISTER_USERNAME_FAIL);
		return bb.array();
	}
	
	public static boolean processRegisterResponseMessage(byte[] responseData) {
		ByteBuffer buf = ByteBuffer.wrap(responseData);
		byte opcode = buf.get();
		if(opcode == DirMessageOps.OPCODE_REGISTER_USERNAME_OK) {
			return true;
		} else {
			return false;
		}
	}
	
	public static byte[] buildUserListRequestMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_GETUSERS);
		return bb.array();
	}
	
	public static byte[] buildUserListResponseMessage(Set<String> nicks) {
		int tamSet = nicks.size();
		// Opcode + Tam lista + (Tam usuario + usuario) por cada entrada de la lista
		ByteBuffer bb = ByteBuffer.allocate(OPCODE_SIZE_BYTES + Integer.BYTES + tamSet * (OPCODE_SIZE_BYTES + BYTE_MAX_VALUE));
		bb.put(DirMessageOps.OPCODE_USERLIST);
		bb.putInt(tamSet);
		for(String n : nicks) {
			bb.put((byte) n.length());
			bb.put(n.getBytes());
		}
		return bb.array();
	}
	
	public static Set<String> processUserListResponse(byte[] data) {
		Set<String> response = new HashSet<String>();
		ByteBuffer bb = ByteBuffer.wrap(data);
		byte opcode = bb.get();
		if(opcode == DirMessageOps.OPCODE_USERLIST) {
			int tamSet = bb.getInt();
			byte tamString;
			byte[] _string;
			for(int i = 0; i < tamSet; i++) {
				tamString = bb.get();
				_string = new byte[tamString];
				bb.get(_string);
				response.add(new String(_string));
			}
			return response;
		}
		else return null;
	}


	public static byte[] buildUserLookupRequestMessage(String nick) {
		byte[] cadena = nick.getBytes();
		byte longitud = (byte) nick.length();
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES * 2 + longitud);
		bb.put(DirMessageOps.OPCODE_LOOKUP_USERNAME);
		bb.put(longitud);
		bb.put(cadena);
		return bb.array();
	}
	
	public static byte[] buildUserLookupResponseMessage(InetSocketAddress addr) {
		ByteBuffer bb;
		if(addr == null) {
			bb = ByteBuffer.allocate(OPCODE_SIZE_BYTES);
			bb.put(DirMessageOps.OPCODE_LOOKUP_USERNAME_NOTFOUND);
		}
		else {
			String dir = addr.getHostName();
			int port = addr.getPort();
			byte[] cadena = dir.getBytes();
			byte longitud = (byte) dir.length();
			bb = ByteBuffer.allocate(OPCODE_SIZE_BYTES * 2 + longitud + Integer.BYTES);
			bb.put(DirMessageOps.OPCODE_LOOKUP_USERNAME_FOUND);
			bb.put(longitud);
			bb.put(cadena);
			bb.putInt(port);
		}
		return bb.array();
	}

	public static InetSocketAddress processUserLookupResponse(byte[] data) {
		InetSocketAddress addr;
		ByteBuffer bb = ByteBuffer.wrap(data);
		byte opcode = bb.get();
		if(opcode == DirMessageOps.OPCODE_LOOKUP_USERNAME_FOUND) {
			byte tam = bb.get();
			byte[] dir = new byte[tam];
			for(int i = 0; i < tam; i++) {
				dir[i] = bb.get();
			}
			int port = bb.getInt();
			addr = new InetSocketAddress(new String(dir), port);
			return addr;
		}
		else return null;
	}

	public static byte[] buildFileListRequestMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_GETFILES);
		return bb.array();
	}

	public static byte[] buildFileListResponseMessage(Set<String> filelist) {
		int tamSet = filelist.size();
		// Opcode + Tam lista + (Tam file + file) por cada entrada de la lista
		ByteBuffer bb = ByteBuffer.allocate(OPCODE_SIZE_BYTES + Integer.BYTES + tamSet * (OPCODE_SIZE_BYTES + BYTE_MAX_VALUE));
		bb.put(DirMessageOps.OPCODE_FILELIST);
		bb.putInt(tamSet);
		for(String n : filelist) {
			bb.put((byte) n.length());
			bb.put(n.getBytes());
		}
		return bb.array();
	}
	
	public static Set<String> processFileListResponseMessage(byte[] data) {
		Set<String> response = new HashSet<String>();
		ByteBuffer bb = ByteBuffer.wrap(data);
		byte opcode = bb.get();
		if(opcode == DirMessageOps.OPCODE_FILELIST) {
			int tamSet = bb.getInt();
			byte tamString;
			byte[] _string;
			for(int i = 0; i < tamSet; i++) {
				tamString = bb.get();
				_string = new byte[tamString];
				bb.get(_string);
				response.add(new String(_string));
			}
			return response;
		}
		else return null;
	}
	
	public static byte[] buildServeFilesRequestMessage(int port, String nick, FileInfo[] fi) {
		int tamSet = fi.length;
		byte tamNick = (byte) nick.length();
		// Opcode + Tam nick + nick + Puerto (int) +
		// Tam lista + (Tam string + String[entrada]) por cada entrada de la lista
		ByteBuffer bb = ByteBuffer.allocate(OPCODE_SIZE_BYTES + Integer.BYTES +
				OPCODE_SIZE_BYTES + tamNick +
				tamSet *(Integer.BYTES + 3 * (OPCODE_SIZE_BYTES + BYTE_MAX_VALUE)));
		bb.put(DirMessageOps.OPCODE_SERVE_FILES);
		bb.put(tamNick);
		bb.put(nick.getBytes());
		bb.putInt(port);
		bb.putInt(tamSet);
		for(FileInfo n : fi) {
			bb.put((byte) n.fileHash.length());
			bb.put(n.fileHash.getBytes());
			bb.put((byte) n.fileName.length());
			bb.put(n.fileName.getBytes());
			bb.put((byte) n.filePath.length());
			bb.put(n.filePath.getBytes());
			bb.putInt((int) n.fileSize);
		}
		return bb.array();
	}
	
	public static byte[] buildServeFilesResponseMessage(boolean success) {
		ByteBuffer bb = ByteBuffer.allocate(OPCODE_SIZE_BYTES);
		if(success) bb.put(DirMessageOps.OPCODE_SERVE_FILES_OK);
		else bb.put(DirMessageOps.OPCODE_SERVE_FILES_FAIL);
		return bb.array();
	}
	
	public static boolean processServeFilesResponseMessage(byte[] responseData) {
		ByteBuffer buf = ByteBuffer.wrap(responseData);
		byte opcode = buf.get();
		if(opcode == DirMessageOps.OPCODE_SERVE_FILES_OK) {
			return true;
		} else {
			return false;
		}
	}

	public static byte[] buildLogoutRequestMessage(String nick) {
		byte[] cadena = nick.getBytes();
		byte longitud = (byte) nick.length();
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES * 2 + longitud);
		bb.put(DirMessageOps.OPCODE_LOGOUT);
		bb.put(longitud);
		bb.put(cadena);
		return bb.array();
	}
	
	public static byte[] buildLogoutResponseMessage() {
		ByteBuffer bb = ByteBuffer.allocate(OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_LOGOUT_OK);
		return bb.array();
	}

	/*
	 * Crear métodos buildXXXXRequestMessage/buildXXXXResponseMessage para
	 * construir mensajes de petición/respuesta
	 */
	// public static byte[] buildXXXXXXXResponseMessage(byte[] responseData)
	/*
	 * Crear métodos processXXXXRequestMessage/processXXXXResponseMessage para
	 * parsear el mensaje recibido y devolver un objeto según el tipo de dato que
	 * contiene, o boolean si es únicamente éxito fracaso.
	 */
	// public static boolean processXXXXXXXResponseMessage(byte[] responseData)

}
