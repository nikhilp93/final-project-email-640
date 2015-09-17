package trialsserver;

import java.nio.file.*;// "nio.file" package is imported to read data from file
import java.net.*;// networking classes
import java.util.*;//scanner and other classes in java.util package
import java.io.*;//java.io classes

public class ServerApplication 
{
	/*should be the same port as the client is sending packets to*/
	private final static int SERVER_PORT_NUM = 9999; 
	 /* UDP segments larger than this value are not supported by some OSs*/
	private final static int MAX_MESSAGE_SIZE = 8192;
	private final static int TX_MESSAGE_SIZE = 8192; // transmitted message size
	
	public static void main(String[] args) throws Exception,IOException
	{
		
		// send and receive data buffers
		byte[] receivedMessage = new byte[MAX_MESSAGE_SIZE];
		byte[] sentMessage = new byte[TX_MESSAGE_SIZE];
		
		// send and receive UDP packets
		DatagramPacket receivePacket = new DatagramPacket(receivedMessage,receivedMessage.length);
		
		// server socket, listening on port SERVER_PORT_NUM
		DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT_NUM);
		
		// do this forever
		while(true)
		{
			System.out.print("\n\nWaiting for a message from the client.");
			
			// receiving client's request
			serverSocket.receive(receivePacket);
			System.out.print("\nMessage received from the client.");
			
			// getting the client info out of the received UDP packet object
			InetAddress clientAddress = receivePacket.getAddress(); // client's IP address
			int clientPort = receivePacket.getPort(); // client's port number
			int dataLength = receivePacket.getLength(); // the number of received bytes
			
			//print the received byte sequence if needed
			/*System.out.print("\nThe received byte sequence is: ");
			for(int ind = 0; ind < dataLength; ind++)
			{
				System.out.printf("%d", receivedMessage[ind]);
				if(ind < dataLength-1)
					System.out.print(", ");
			}*/
			
			// converting the received byte array into String
			String receivedText = new String(receivedMessage, 0, dataLength);
			System.out.println("\nThe received text from the client is: ");
			System.out.println(receivedText);
			
			// setting up the response UDP packet object
			//generating the response code for the received String
			//calling responseCode() method to find the specific response code
			String s1=responseCode(receivedText);
			System.out.println("Sending an OK response to the client.");
			System.out.println("-------------------------------------------");
			
			// sending the response to the client
			receivePacket.setLength(MAX_MESSAGE_SIZE);
			// the socket is not closed, as this program is supposed to run "forever"
			
			byte[] responsemsgtotx=s1.getBytes();
			int portclient=receivePacket.getPort();
			InetAddress clientAddress1=receivePacket.getAddress();
			DatagramPacket sentPacket=new DatagramPacket(responsemsgtotx,responsemsgtotx.length,clientAddress1,portclient);
			serverSocket.send(sentPacket);
    
		} // while(true)
	
	}//main()
	public static String responseCode(String recText) throws Exception,IOException
	{
		/*splitting the string using CR+LF in order to extract different parts of request message*/ 
		String[] delim = recText.split("\r\n");
	
		String delim1 = delim[0];
		String delim2 = delim[1];
		String delim3 = delim[2];
	
		String S2=delim1+"\r\n"+delim2+"\r\n";
		/*calculating  integrity check value only for first two parts of request message
		 *  thereby verifying integrity check value later*/
		String newChecksum=integrityCheck(S2);
	
		/*checking whether the calculated integrity check value
		 *  matches the last part of request message*/
		if(newChecksum.equals(delim3))
		{//4if
			/*verifying syntax errors in different parts of request message as follows*/
			/*part11[] contains the request message "ENTS/1.0" and "Request" fields*/
			String[] part11=delim1.split(" ");
			/*part21[] contains "filename" and "extension" fields */
			String[] part21=delim2.split("\\.");
			/*part31[] contains integrity check value of request message*/
			String[] part31=delim3.split("");
			
			/*the filename field should only contain alphanumeric characters and underscore */
			/*the extension field can contain only alphanumeric characters*/
			/*the first character of filename filed should be a alphabet*/
			if(part21[0].matches("^[a-zA-Z0-9_]*") && part21[1].matches("^[a-zA-Z0-9]*") && part21[0].substring(1, 2).matches("^[a-zA-Z]*"))
			{//3if
				/*checking the version name*/
				if(delim1.contains("1.0"))
				{//2if
					/*checking user request with existing files on server*/
					if(delim2.equals("scholarly_paper.txt")||delim2.equals("directors_message.txt")||delim2.equals("program_overview.txt")) 
					{//1if
				 
						/*printing the specific response code if needed
						 * System.out.println("The Response code is 0");
						 */
				 
						/*the characters from the file can also be read in this way
						String s = new Scanner(new File("D:\\pa\\"+part2)).useDelimiter("\\Z").next();*/
						/*reading the characters from the selected file*/
						/*we can use FileReader and BufferedReader in this context, 
						 * but to simplify the code, an easy method is adopted here*/
						String s = new String(Files.readAllBytes(Paths.get("D:\\pa\\"+delim2)));
						String s3="ENTS/1.0 Response"+"\r\n"+"0\r\n"+s.length()+"\r\n"+s;
						String newChecksumr0=integrityCheck(s3);
						/*creating the response message according to the given directions*/
						/*as all syntaxes are correct and integrity check value is also matching,
						 *  response code 0 is sent along with file data accordingly*/
						String sr0="ENTS/1.0 Response"+"\r\n"+"0\r\n"+s.length()+"\r\n"+s+newChecksumr0+"\r\n";
						System.out.println("The response message that is being sent back to client is:");
						System.out.println(sr0);
						return sr0;   
					}//1if
					else
					{
						/*As the file with given name does not exist,
						 *  Response Code 3 is sent without any file data*/
						
						/*printing the specific response code if needed
						 * System.out.println("response code 3");
						 */
						String sr3=integrityResponse("3");
						return sr3;
				        
					}//1else	
				}//2if
				else
				{
					/*As the protocol version does not match, 
					 * Response code 4 is sent without any file data*/
					/*printing the specific response code if needed
					 *System.out.println("response code 4");*/
					
					String sr4=integrityResponse("4");
					return sr4; 
				}//2else
			}//3if
			else
			{	
				/*As the syntax of the request message fails,
				 *  Response code 2 is sent without any file data*/
				/*printing the specific response code if needed
				 *System.out.println("response code 2");*/
				String sr2=integrityResponse("2");
				return sr2;
			}//3else
		}//4if
		else
		{	
			/*As the integrity check value does not match,
			 *  Response code 1 is sent without any file data*/
			/*printing the specific response code if needed
			 *System.out.println("response code 1");*/
			String sr1=integrityResponse("1");
			return sr1;
		}//4else
	}//end of main() method
	public static String integrityCheck(String a)
	{
		/*converting string to char array*/
		char[] b=stringToCharArray(a);
		/*converting resultant char array to string/word array*/
		String[] c=charArrayToWordArray(b);
		/*converting the word array to integer array*/
		int[] d=wordArrayToIntArray(c);
		/*evaluating integrity check value according to the given instructions*/
		short S=0;
		for(int i=0;i<d.length-1;i++)
		{
			short index= (short)(S^d[i]);
			S=(short)((7919*index)%65536);
		}
			
		System.out.println("Value of Integrity Check is: "+S);
		String newChecksum=String.valueOf(S);
		return newChecksum; 
	}//integrityCheck() method
	public static char[] stringToCharArray(String a)
	{		
		char[] charArrayOfS2 = new char[a.length()];
		for(int i=0;i<charArrayOfS2.length;i++)
		{	
			charArrayOfS2[i]=a.charAt(i);
		}
		return charArrayOfS2;
		
	}//stringToCharArray() method
	public static String[] charArrayToWordArray(char[] charArrayOfS2)
	{
		String[] wordArray = new String[charArrayOfS2.length/2];	
		for(int i=0;i<charArrayOfS2.length-1;i=i+2)
		{
			if(charArrayOfS2.length%2==0)
			{	
				/*if there are even number of characters in char array, 
				 * the word array has exactly half the length of char array and has appended consecutive elements as its elements*/
				wordArray[i/2]=(String.format("%8s", Integer.toBinaryString((int) charArrayOfS2[i])).replace(' ', '0').concat(String.format("%8s", Integer.toBinaryString((int) charArrayOfS2[i+1])).replace(' ', '0')));
			}
			else
			{	
				/*if there are odd number of characters in char array, 
				 * the word array has exactly half the length of char array and has appended consecutive elements as its elements and the last character has to be appended with 8-bit zeros "00000000"*/
				wordArray[i/2]=String.format("%8s", Integer.toBinaryString((int) charArrayOfS2[i])).replace(' ', '0')+String.format("%8s", Integer.toBinaryString((int) charArrayOfS2[i+1])).replace(' ', '0');
				wordArray[((charArrayOfS2.length)/2)-1]=String.format("%8s", Integer.toBinaryString((int) charArrayOfS2[charArrayOfS2.length-1])).replace(' ', '0')+"00000000";
			}
		}
		return wordArray;
	}//charArrayToWordArray() method
	public static int[] wordArrayToIntArray(String[] a)
	{
		/*the given wordarray has elements in string array,
		 *  so we need to convert it into integer array in order to perform calculations*/
		int[] convIntArray=new int[a.length];
		for(int i=0;i<a.length;i++)
		{
			convIntArray[i]=Integer.parseInt(a[i],2);
		}
		return convIntArray;	
	}//wordArrayToIntArray() method

	public static String integrityResponse(String a)
	{
		/*if the response code is anything other than 0, we should not send any file data. 
		 * the following code forms the response code for rest other codes .i.e. 1,2,3,4*/
		String s4="ENTS/1.0 Response"+"\r\n"+a+"\r\n"+"0\r\n";
		String newChecksumr0=integrityCheck(s4);
		String sr2="ENTS/1.0 Response"+"\r\n"+a+"\r\n"+"0\r\n"+newChecksumr0+"\r\n";
		System.out.println(sr2);
		System.out.println("The length of sent string is: "+sr2.length());
		return sr2;
	}//integrityResponse() method
	
}//ServerApplication class