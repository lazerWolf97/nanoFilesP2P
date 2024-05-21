package es.um.redes.nanoFiles.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import es.um.redes.nanoFiles.client.application.NanoFiles;
import es.um.redes.nanoFiles.message.*;
import es.um.redes.nanoFiles.util.*;

public class NFServerComm {

	public static void serveFilesToClient(Socket socket) {
		boolean clientConnected = true;
		String readmsg, response;
		PeerMessage peermsg, peerresponse;
		
		// Bucle para atender mensajes del cliente
		try {
			/*
			 * Crear dis/dos a partir del socket
			 */
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			while (clientConnected) { // Bucle principal del servidor
				// Leer un mensaje de socket y convertirlo a un objeto PeerMessage
				readmsg = dis.readUTF();
				peermsg = PeerMessage.fromString(readmsg);
				
				/*
				 * TODO: Actuar en función del tipo de mensaje recibido. Se pueden crear
				 * métodos en esta clase, cada uno encargado de procesar/responder un tipo de petición.
				 */
				switch(peermsg.getOperation()) {
					case PeerMessageOps.OP_DOWNLOAD:
						String content = download(peermsg.getName());
						peerresponse = new PeerMessage(PeerMessageOps.OP_UPLOAD, content);
						response = peerresponse.toEncodedString();
						dos.writeUTF(response);
					break;
					
					case PeerMessageOps.OP_SERVEDFILES:
						
					break;
					
					case PeerMessageOps.OP_CLOSE:
						clientConnected = false;
					break;
				}
			}
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static String download(String hash) {
		String path = NanoFiles.db.lookupFilePath(hash);
		String content = null;
		File f = new File(path);
		try {
			FileInputStream fis = new FileInputStream(f);
			content = new String(fis.readAllBytes());
			fis.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return content;
	}
}
