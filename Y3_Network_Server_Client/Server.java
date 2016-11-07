import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.InetSocketAddress;

import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Adler32;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.BasicAuthenticator;

/** 
  * the server class for sending the file to the clients
 */
public class Server {

	public static void main(String[] args) throws Exception 
	{
		// initiate the server at port 8088
		HttpServer server = HttpServer.create(new InetSocketAddress(8088), 0);
		
		// create the file object pointing at the folder storing all the available files
		File folder = new File("files");
		
		// create a list of file from the folder above
		File[] listOfFiles = folder.listFiles();
		
		if(listOfFiles != null && listOfFiles.length != 0)
		{
			// call functions to handle different file requests
			createContext(server, listOfFiles);
		}
		else
		{
			server.createContext("/Info", new InfoHandler(null));
		}
		
		// create a multi threading executors
		server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(5)); 
		
		// start the server
		server.start();
		
		// inform where the server is binded
		System.out.println("Server binded to port 8088");
	}
  
	/** 
	 * function for creating multiple file request handlers
	 * 
	 * @param server 		the current server
	 * @param listOfFiles   the list of the files available
	 */
	private static void createContext(HttpServer server, File[] listOfFiles)
	{
		// create an info page
		String[] nameOfFiles = getNameOfFiles(listOfFiles);
		server.createContext("/Info", new InfoHandler(nameOfFiles));
		System.out.println("info page created");
		
		// iteration through the list of available files from the second file
		for(int i = 0; i < listOfFiles.length; i++)
		{	
			// create the individual request handlers
			HttpContext context = server.createContext("/" + listOfFiles[i].getName(), new Handler(listOfFiles[i]));

			// set authentication
			context.setAuthenticator(new BasicAuthenticator("admin") 
			{
				@Override
				public boolean checkCredentials(String user, String pwd) 
				{
					return user.equals("admin") && pwd.equals("admin");
				}
			});
			
			// print out the list of available files
			System.out.println(listOfFiles[i].getName());
		}
	}
	
	/** 
	 * Handler class for handle request for files
	 */
	private static class Handler implements HttpHandler 
	{
		/**
		 * the file attached to the handler 
		 */
		private File file;

		/** 
		 * constructor
		 * @param file the attached file
		 */
		public Handler(File file)
		{
			this.file = file;
		} 
	 
		/**
		 * the actual handling action
		 */
		public void handle(HttpExchange t) throws IOException 
		{
			
			// add the required response header
			Headers h = t.getResponseHeaders();
			
			// get the file type of the file
			String fileType = file.getName().substring(file.getName().lastIndexOf(".") +1 );
			
			// check if the file is a directory
			if (file.isDirectory())
			{	
				// add the content type header
				h.add("Content-Type", "Zipped Folder");
				
				// if the file is a folder, get all the files inside the folder
				File[] files = file.listFiles();
				
				// zip the folder into a zip file
				File file2 = new File(zip(files, file.getName() + ".zip"));

				// encrypt the file
				Encryption encTool = new Encryption();
				File file3 = new File(encTool.encryptAESCTR(file2.getAbsolutePath()));
				
				// send the zip file
				sendFile(t, file2);
				
				// delete the zip file after transmission
				file2.delete();

				// delete the encrypted file after transmission
				file3.delete();
				
				// notify about the transmission
				System.out.println("Zipped Folder" + " " + file.getName() + " sent to client");
			}
			else
			{	
				// add the file type header						
				h.add("Content-Type", fileType);

				// encrypt the file
				Encryption encTool = new Encryption();
				File file2 = new File(encTool.encryptAESCTR(file.getAbsolutePath()));
				
				// send the file
				sendFile(t, file2);

				// delete the encrypted file after transmission
				file2.delete();
				
				// notify about the transmission
				System.out.println(fileType + " " + file.getName() + " sent to client");
			}
		}   
	}
	
	/** 
	 * Handler class for handle request for info
	 */
	static class InfoHandler implements HttpHandler
	{
		/**
		 * the list of the name of files attached to the handler 
		 */
		private String[] nameOfFiles;

		/** 
		 * constructor
		 * @param file the attached file
		 */
		public InfoHandler(String[] nameOfFiles)
		{
			this.nameOfFiles = nameOfFiles;
		} 
	 
		/**
		 * the actual handling action
		 */
		public void handle(HttpExchange t) throws IOException 
		{
			// add the required response header
			Headers h = t.getResponseHeaders();

			// add the content type header
			h.add("Content-Type", "Info");
			
			String info = "The list of file(s) available is: ";

			if(nameOfFiles != null)
			{
				info = info + nameOfFiles[0];
				for (int i = 0; i < (nameOfFiles.length - 1); i++)
				{
					info = info + ", " + nameOfFiles[i+1];
				}
			}

			// send the info to the client in the form of a http response
			t.sendResponseHeaders(200, info.length());
			OutputStream os = t.getResponseBody();
			os.write(info.getBytes());
		
			// close the stream after use
			os.close();

			// notify the transmission
			System.out.println("Info sent to the client");
		}
	}


	/**
	 * the function for sending the file
	 * @param t 	the http exchange
	 * @param file  the file to send 
	 */
	private static void sendFile(HttpExchange t, File file) throws IOException 
	{
		// read in the file through a buffering stream
		byte [] bytearray  = new byte [(int)file.length()];
		FileInputStream fis = new FileInputStream(file);
		BufferedInputStream bis = new BufferedInputStream(fis);
		bis.read(bytearray, 0, bytearray.length);

		// send to the file in the form of a http response
		t.sendResponseHeaders(200, file.length());
		OutputStream os = t.getResponseBody();
		os.write(bytearray,0,bytearray.length);
		
		// close the stream after use
		os.close();
	}
	
	/**
	 * the function for zipping files
	 * @param files		the list of files to be zipped
	 * @param zipName	the name of the zip file
	 * 
	 * @return a string represent the zipped file
	 */
	private static String zip(File[] files, String zipName)
	{
		 try 
		 {
			 // initiate the stream for reading file
			 BufferedInputStream bis = null;
			 
			 // initiate the stream for writing file
			 FileOutputStream dest = new FileOutputStream("files/" + zipName);
			 CheckedOutputStream cos = new CheckedOutputStream(dest, new Adler32());
			 ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(cos));
			 byte data[] = new byte[2048];

			for (int i=0; i<files.length; i++) 
			{			
				// notifying of compressing files	
				System.out.println("Adding: "+files[i].getName());
				
				// reading in the files through stream
				FileInputStream fis = new FileInputStream(files[i]);				
				bis = new BufferedInputStream(fis, 2048);
				
				// preparing the zip object
				ZipEntry entry = new ZipEntry(files[i].getName());
				zos.putNextEntry(entry);
				int count;
				
				// writing the zip file
				while((count = bis.read(data, 0,  2048)) != -1) 
				{
					zos.write(data, 0, count);
				}
				
				// close the input stream
				bis.close();
			}
			// close the writing stream
			zos.close();
			
			return ("files/" + zipName);
		} 
		catch(Exception e) 
		{
			e.printStackTrace();
		}
		
		return ("files/" + zipName);
	}

	/**
	 * method for getting the name of the files from an array of files
	 * 
	 * @param listOfFiles 	 the array of the files
	 *
	 * @return nameOfFiles 	 the array of the names of the files
	 */
	private static String[] getNameOfFiles(File[] listOfFiles)
	{
		String[] nameOfFiles = new String[listOfFiles.length];
		for(int i = 0; i < listOfFiles.length; i++)
		{
			if(listOfFiles[i].isDirectory())
			{
				nameOfFiles[i] = "folder " + listOfFiles[i].getName();
			}
			else
			{
				nameOfFiles[i] = listOfFiles[i].getName();
			}
		}

		return nameOfFiles;
	}
}
