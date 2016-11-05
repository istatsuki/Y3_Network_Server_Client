import java.net.URL;
import java.net.URLConnection;

import java.util.Map;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedOutputStream;

/**
 * the client to download a file from a given url
 */
public class Client
{	
	public static void main(String[] args) throws IOException
	{
		// read commandLine input
		BufferedReader commandLine = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.print("Please write the url: ");

	    // get the url
		String urlstr = commandLine.readLine();
		
		// making sure the url is in the correct format
		if(urlstr.indexOf("http://") != 0)
		{
			urlstr = "http://" + urlstr;
		}
		
		// invoke the function to get the file
		receiveFile(urlstr);			
	}
	
	/**  
	 * function for receiving the file from a given url
	 * @param urlstr   the url in string format
	 */
	public static void receiveFile(String urlstr)
	{	
		try
    	{	
			// authentication
			String encoded = DatatypeConverter.printBase64Binary(("admin" + ":" + "admin").getBytes("UTF-8"));
			
			// make the url object
			URL url = new URL(urlstr);
			
			// make the connection
			URLConnection conn = url.openConnection();			
			conn.setRequestProperty("Authorization", "Basic " + encoded);
			conn.connect();

			// get the file type
			String fileType = conn.getHeaderField("Content-Type");
			
			if(fileType != null)
			{
				String fileName = "";
				
				// file name options
				if(fileType.contains("Zipped Folder"))
				{
					fileName = urlstr.substring(urlstr.lastIndexOf("/") + 1) + ".zip";
				}
				else
				{
					fileName = urlstr.substring(urlstr.lastIndexOf("/") + 1);		
				}
				
				// initiate the streams for getting the files
				DataInputStream dis= new DataInputStream(conn.getInputStream());
				
				// initiate the streams for putting the files at local directory
				OutputStream os = new BufferedOutputStream(new FileOutputStream(fileName));
				
				// get the files from the streams
				for (int b; (b = dis.read()) != -1;) 
				{
				os.write(b);
				}
				
				// close the streams
				os.close();
				dis.close();
				
				System.out.println(fileType + " " + fileName + " received from the server");
			}
			else
			{
				System.out.println("Incorrect Authentication");
			}
    	} catch (IOException e) {
			System.out.println("File not found from the url");
		}
	}
}
