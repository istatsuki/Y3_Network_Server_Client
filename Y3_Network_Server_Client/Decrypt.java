public class Decrypt
{
	public static void main(String[] args)
	{
		Encryption encTool = new Encryption();
		encTool.decryptAESCTR(args[0]);
	}
}
