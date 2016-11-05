import java.io.RandomAccessFile;
import java.io.FileOutputStream;

import java.security.SecureRandom;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;

import java.util.Arrays;
public class Encryption 
{
	static String hexKey="3eafda76cd8b015641cb946708675423";
	
	/**
     * for AESCTR decryption
     * 
     * @param inFile 	the path to the file to decrypt
     * @return 			the path to the encrypted file
     */
	public static String encryptAESCTR(String inFile) {
        
        String outFile = "";
        
        try {
            // Open and read the input file
            // N.B. this program reads the whole file into memory, not good for large programs!
            RandomAccessFile rawDataFromFile = new RandomAccessFile(inFile, "r");
            byte[] plainText = new byte[(int) rawDataFromFile.length()];
            rawDataFromFile.read(plainText);
            rawDataFromFile.close();

            //Set up the AES key & cipher object in CTR mode
            SecretKeySpec secretKeySpec = new SecretKeySpec(hexStringToByteArray(hexKey), "AES");
            Cipher encAESCTRcipher = Cipher.getInstance("AES/CTR/PKCS5Padding");    
            SecureRandom random = new SecureRandom();
            byte iv[] = new byte[16];
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            encAESCTRcipher.init(Cipher.ENCRYPT_MODE, secretKeySpec,ivSpec);

            //Encrypt the data
            byte[] cipherText = encAESCTRcipher.doFinal(plainText);

            //Write file to disk
            outFile = "Encrypted " + inFile.substring(inFile.lastIndexOf("/") +1);
            System.out.println("Openning file to write: " + outFile);
            FileOutputStream fos = new FileOutputStream("files/" + outFile);
            fos.write(iv);
            fos.write(cipherText);
            
            //close the stream
            fos.close();
            
            // notification about the encryption
            System.out.println(inFile + " encrypted as " + outFile);
            
            return ("files/" + outFile);
        } 
        catch (Exception e)
        {
            System.out.println("doh "+e);
        }
        
        return outFile;
    }
    
    /**
     * for AESCTR decryption
     * 
     * @param inFile 	the path to the file to decrypt
     */ 
    public static void decryptAESCTR(String inFile) {
	try {
            // Open and read the input file
            // N.B. this program reads the whole file into memory, not good for large programs!
            RandomAccessFile rawDataFromFile = new RandomAccessFile(inFile, "r");
            byte[] cipherText = new byte[(int) rawDataFromFile.length()];
            rawDataFromFile.read(cipherText);
            rawDataFromFile.close();

            //Set up the AES key & cipher object in CTR mode with the iv provided in the encrypted file
            SecretKeySpec secretKeySpec = new SecretKeySpec(hexStringToByteArray(hexKey), "AES");
            Cipher decAESCTRcipher = Cipher.getInstance("AES/CTR/PKCS5Padding");
            
			// because the Iv is at the start of the encrypted file and is only 16 bytes
            byte[] iv = Arrays.copyOfRange(cipherText, 0, 16);
			IvParameterSpec ivSpec = new IvParameterSpec(iv);  
            decAESCTRcipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);

            //Decrypt the encrypted text (from the 17th byte until the end of the file) in the file provided
			byte[] encOriginalText = Arrays.copyOfRange(cipherText, 16, cipherText.length);
            byte[] originalText = decAESCTRcipher.doFinal(encOriginalText);

            //Write the original text file only to disk
            String outFile = "Decrypted " + inFile.substring(inFile.lastIndexOf("/") +1);
            System.out.println("Openning file to write: " + outFile);
            FileOutputStream fos = new FileOutputStream(outFile);
            fos.write(originalText);
            
            // close the stream
            fos.close();
            
            // notification about the decryption
            System.out.println(inFile+" dencrypted as " + outFile);
        } 
        catch (Exception e)
        {
            System.out.println("doh "+e);
        }
    }
    
    /**
     * Code for type conversion
     */
    private static byte[] hexStringToByteArray(String s) 
    {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) 
        {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
