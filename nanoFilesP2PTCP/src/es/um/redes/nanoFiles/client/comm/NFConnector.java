package es.um.redes.nanoFiles.client.comm;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import es.um.redes.nanoFiles.client.application.NanoFiles;
import es.um.redes.nanoFiles.message.PeerMessage;
import es.um.redes.nanoFiles.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileDigest;

//Esta clase proporciona la funcionalidad necesaria para intercambiar mensajes entre el cliente y el servidor de NanoChat
public class NFConnector {
	private Socket socket;
	protected DataOutputStream dos;
	protected DataInputStream dis;

	public NFConnector(InetSocketAddress serverAddress) throws UnknownHostException, IOException {
		/*
		 * Se crea el socket a partir de la dirección del servidor (IP, puerto). La
		 * creación exitosa del socket significa que la conexión TCP ha sido
		 * establecida.
		 */
		socket = new Socket(serverAddress.getAddress(), serverAddress.getPort());
		
		/*
		 * Se crean los DataInputStream/DataOutputStream a partir de los streams de
		 * entrada/salida del socket creado. Se usarán para enviar (dos) y recibir (dis)
		 * datos del servidor mediante los métodos readUTF y writeUTF (mensajes
		 * formateados como cadenas de caracteres codificadas en UTF8)
		 */
		dis = new DataInputStream(socket.getInputStream());
		dos = new DataOutputStream(socket.getOutputStream());
	}

	/**
	 * Método que utiliza el Shell para ver si hay datos en el flujo de entrada.
	 * Permite "sondear" el socket con el fin evitar realizar una lectura bloqueante
	 * y así poder realizar otras tareas mientras no se ha recibido ningún mensaje.
	 * 
	 * @return
	 * @throws IOException
	 */
	public boolean isDataAvailable() throws IOException {
		return (dis.available() != 0);
	}

	/**
	 * Método para descargar un fichero a través del socket mediante el que estamos
	 * conectados con un peer servidor.
	 * 
	 * @param targetFileHashSubstr El hash del fichero a descargar
	 * @param file                 El objeto File que referencia el nuevo fichero
	 *                             creado en el cual se escriben los datos
	 *                             descargados del servidor (contenido del fichero
	 *                             remoto)
	 * @return Verdadero si la descarga se completa con éxito, falso en caso
	 *         contrario.
	 * @throws IOException Si se produce algún error al leer/escribir del socket.
	 */
	public boolean download(String targetFileHashSubstr, File file) throws IOException {
		boolean downloaded = false;
		PeerMessage peermsg;
		String inmsg, outmsg;
		String fOld;
		/*
		 * Construir objeto PeerMessage que modela un mensaje de solicitud de
		 * descarga de fichero (indicando el fichero a descargar), convertirlo a su
		 * codificación en String (mediante toEncodedString) y enviarlo al servidor.
		 */
		peermsg = new PeerMessage(PeerMessageOps.OP_DOWNLOAD, targetFileHashSubstr);
		outmsg = peermsg.toEncodedString();
		dos.writeUTF(outmsg);
		
		/*
		 * Recibir mensajes del servidor codificados como cadena de caracteres,
		 * convertirlos a PeerMessage (mediante "fromString"), y actuar en función del
		 * tipo de mensaje recibido.
		 */
		inmsg = dis.readUTF();
		peermsg = PeerMessage.fromString(inmsg);
		if(peermsg.getOperation().compareTo(PeerMessageOps.OP_UPLOAD) != 0) return false;
		fOld = peermsg.getName();
		
		/*
		 * Crear un FileOutputStream a partir de "file" para escribir cada
		 * fragmento recibido en el fichero. Cerrar el FileOutputStream una vez se han
		 * escrito todos los fragmentos.
		 */
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(fOld.getBytes());
		fos.close();
		
		/*
		 * Comprobar la integridad del fichero creado, calculando su hash y
		 * comparándolo con el hash del fichero solicitado.
		 */
		String newFileHash = FileDigest.getChecksumHexString(FileDigest.computeFileChecksum(file.getName()));
		if(newFileHash.compareTo(targetFileHashSubstr) == 0) {
			System.out.println("File downloaded successfully.");
		}
		else {
			System.err.println("File's integrity compromised.");
			downloaded = false;
		}

		return downloaded;
	}
}
