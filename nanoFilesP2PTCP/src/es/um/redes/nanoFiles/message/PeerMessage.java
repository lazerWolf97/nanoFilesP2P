package es.um.redes.nanoFiles.message;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import es.um.redes.nanoFiles.client.application.NanoFiles;
import es.um.redes.nanoFiles.util.FileDatabase;

/**
 * Clase que modela los mensajes del protocolo de comunicación entre pares para
 * implementar el explorador de ficheros remoto (servidor de ficheros). Estos
 * mensajes son intercambiados entre las clases NFServerComm y NFConnector, y se
 * codifican como texto en formato "campo:valor".
 * 
 * @author rtitos
 *
 */
public class PeerMessage {
	private static final char DELIMITER = ':'; // Define el delimitador
	private static final char END_LINE = '\n'; // Define el carácter de fin de línea

	/**
	 * Nombre del campo que define el tipo de mensaje (primera línea)
	 */
	private static final String FIELDNAME_OPERATION = "operation";
	
	private static final String FIELDNAME_FILE = "file";
	private static final String FIELDNAME_FILEHASH = "filehash";
	private static final String FIELDNAME_DATA = "data";
	
	/**
	 * Tipo del mensaje, de entre los tipos definidos en PeerMessageOps.
	 */
	private String operation;
	/*
	 * Crear un atributo correspondiente a cada uno de los campos de los
	 * diferentes mensajes de este protocolo.
	 */
	private String name;
	private List<String> namelist;
	
	/*
	 * Crear diferentes constructores adecuados para construir mensajes de
	 * diferentes tipos con sus correspondientes argumentos (campos del mensaje)
	 */
	// Mensajes simples
	public PeerMessage(String operation) {
		this.operation = operation;
	}
	// Mensajes de un valor
	public PeerMessage(String operation, String name) {
		this.operation = operation;
		this.name = name;
	}
	// Mensajes de varios valores
	public PeerMessage(String operation, List<String> namelist) {
		this.operation = operation;
		this.namelist = namelist;
	}
	
	/* 
	 * GETTERS
	 */

	public String getOperation() {
		return operation;
	}
	
	public String getName() {
		return name;
	}
	
	public List<String> getNameList() {
		return namelist;
	}

	/**
	 * Método que convierte un mensaje codificado como una cadena de caracteres, a
	 * un objeto de la clase PeerMessage, en el cual los atributos correspondientes
	 * han sido establecidos con el valor de los campos del mensaje.
	 * 
	 * @param message El mensaje recibido por el socket, como cadena de caracteres
	 * @return Un objeto PeerMessage que modela el mensaje recibido (tipo, valores,
	 *         etc.)
	 */
	public static PeerMessage fromString(String message) {
		/*
		 * Usar un bucle para parsear el mensaje línea a línea, extrayendo para
		 * cada línea el nombre del campo y el valor, usando el delimitador DELIMITER, y
		 * guardarlo en variables locales.
		 */
		String decodedmsg = new String(java.util.Base64.getDecoder().decode(message));
		Scanner sc = new Scanner(new StringReader(decodedmsg));
		int index;
		char[] buf = new char[4096];
		String line, op, value;
		String res = new String();
		List<String> list;
		PeerMessage msg = null;
		try {
			// Campo Operacion
			line = sc.next();
			index = line.indexOf(DELIMITER);
			op = line.substring(index + 1).trim();
			
			switch(op) {
				case PeerMessageOps.OP_CLOSE:
					msg = new PeerMessage(op);
				break;
					
				case PeerMessageOps.OP_DOWNLOAD:
					line = sc.next();
					index = line.indexOf(DELIMITER);
					value = line.substring(index + 1).trim();
					msg = new PeerMessage(op, value);
				break;
				
				case PeerMessageOps.OP_UPLOAD:
					line = sc.next();
					index = line.indexOf(DELIMITER);
					value = line.substring(index + 1).trim();
					res = new String(value);
					while(sc.hasNext()) {
						
					}
					msg = new PeerMessage(op, res);
				break;
					
				case PeerMessageOps.OP_SERVEDFILES:
					line = reader.readLine();
					list = new LinkedList<String>();
					while(!line.equals("")) {
						index = line.indexOf(DELIMITER);
						value = line.substring(index + 1).trim();
						list.add(value);
						line = reader.readLine();
					}
					msg = new PeerMessage(op, list);
				break;
				
				default:
					System.err.println("Undefined operation.");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		/*
		 * En función del tipo del mensaje, llamar a uno u otro constructor con
		 * los argumentos apropiados, para establecer los atributos correpondiente, y
		 * devolver el objeto creado. Se debe detectar que sólo aparezcan los campos
		 * esperados para cada tipo de mensaje.
		 */
		return msg;
	}

	/**
	 * Método que devuelve una cadena de caracteres con la codificación del mensaje
	 * según el formato campo:valor, a partir del tipo y los valores almacenados en
	 * los atributos.
	 * 
	 * @return La cadena de caracteres con el mensaje a enviar por el socket.
	 */
	public String toEncodedString() {
		/*
		 * TODO: En función del tipo de mensaje, crear una cadena con el tipo y
		 * concatenar el resto de campos necesarios usando los valores de los atributos
		 * del objeto.
		 */
		StringBuffer sb = new StringBuffer();
		sb.append(FIELDNAME_OPERATION + DELIMITER + " " + operation + END_LINE);
		switch(operation) {
			case PeerMessageOps.OP_DOWNLOAD:
				sb.append(FIELDNAME_FILEHASH + DELIMITER + " " + name + END_LINE);
			break;
			
			case PeerMessageOps.OP_SERVEDFILES:
				namelist.stream().forEach(fn -> {
					sb.append(FIELDNAME_FILE + DELIMITER + " " + fn + END_LINE);
				});
			break;
			
			case PeerMessageOps.OP_UPLOAD:
				sb.append(FIELDNAME_DATA + DELIMITER + " " + name + END_LINE);
			break;
		}
		sb.append(END_LINE);
		return java.util.Base64.getEncoder().encodeToString(sb.toString().getBytes());
	}
	
	public static void main(String[] args) {
		PeerMessage msg1 = new PeerMessage(PeerMessageOps.OP_DOWNLOAD, "1234");
		String msg2str = "operation: servedFiles\nfile: notas.txt\nfile: hola.exe\n\n";
		PeerMessage msg2 = PeerMessage.fromString(msg2str);
		String msg3str = "operation: close\n\n";
		PeerMessage msg3 = PeerMessage.fromString(msg3str);
		NanoFiles.db = new FileDatabase("nf-shared");
		File f = new File(NanoFiles.db.getFiles()[0].filePath);
		File fout = new File("nf-shared//test2.txt");
		long filelength = f.length();
		byte[] filedata = new byte[(int) filelength];
		FileInputStream fs;
		FileOutputStream fos;
		try {
			fout.createNewFile();
			fs = new FileInputStream(f);
			fos = new FileOutputStream(fout);
			fos.write(fs.readAllBytes());
			
			fs.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		String fileStr = java.util.Base64.getEncoder().encodeToString(filedata);
		byte[] fileStrDecoded = java.util.Base64.getDecoder().decode(fileStr);
		System.out.print("Str1\n" + msg1.toEncodedString());
		System.out.print("Str2\n" + msg2.toEncodedString());
		System.out.print("Str3\n" + msg3.toEncodedString());
		System.out.println("FILE:\n");
		for(byte b : fileStrDecoded) {
			System.out.print((char) b);
		}
	}
}
