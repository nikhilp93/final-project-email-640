package trialsclient;


import java.io.InterruptedIOException;// timeout exception type
import java.net.*;// networking classes
import java.util.*;//scanner and other classes in java.util package
import java.io.*;//java.io classes

public class ClientApplication 
{
	// should be the same port as the one the
	// server is listening on!
	private final static int SERVER_PORT_NUMBER = 9999;
	// received message size
	private final static int RX_MESSAGE_SIZE=8192;
	//this variable will be used for 4 timeout events iteratively
	private static int FLAG=4;
	//initial timeout value in milliseconds
	private static int TIME_OUT_VALUE_MS = 1000;
	
	public static void main(String[] args) throws Exception 
	{
		//this should run forever unless it is terminated
		while(true)
		{
			// for console input
			Scanner inp=new Scanner(System.in);
		
			System.out.println("The available files in the server are:");
			System.out.println("A. directors_message.txt");
			System.out.println("B. program_overview.txt");
			System.out.println("C. scholarly_paper.txt");
			System.out.println("Please enter the text file which you want to access from server: A/B/C");
		
		
			String req_file=null;;
			String inp1=inp.nextLine();
			/*FORMING THE REQUEST MESSAGE BY CALLING THE METHOD requestMessage
			 the request message contains protocol and its version, filename and its extension, 
			 and integrity check value of its first two fields
			 all the fields are separated by CR+LF */
			String req_msg=requestMessage(inp1,req_file);
			
			// converting the request message into a byte array
			byte[] req_msgBytes=req_msg.getBytes();
			/*System.out.println("The sent byte sequence is:");
			for(int ind=0;ind<req_msgBytes.length;ind++)
			{
				System.out.printf("%d,",req_msgBytes[ind]);
			}*/
		
			// creating the IP address object for the server machine
			// method of looping back the request to the same machine
			InetAddress serverIp= InetAddress.getLocalHost();
			
			// creating the UDP packed to be sent
			DatagramPacket sentPacket=new DatagramPacket(req_msgBytes,req_msgBytes.length,serverIp,SERVER_PORT_NUMBER);

			// creating the UDP client socket
			DatagramSocket clientSocket=new DatagramSocket();

			// sending the UDP packet to the server
			clientSocket.send(sentPacket);
		
			while(FLAG>0)
			{
				// setting the timeout for the socket
				clientSocket.setSoTimeout(TIME_OUT_VALUE_MS);
			
				// receiving the server's response
				try
				{
					/*printing the trial number of timeout-we totally have 4 timeout events
					 * 1000ms,2000ms,4000ms and 8000ms */
					System.out.print("trial# "+(5-FLAG) +": waiting for server's response\n");
			
					// creating the receive UDP packet
					byte[] receivedBytes = new byte[RX_MESSAGE_SIZE];
					DatagramPacket receivedPacket=new DatagramPacket(receivedBytes, RX_MESSAGE_SIZE);
					clientSocket.receive(receivedPacket);
					// the timeout timer starts running here
					// the receive() method blocks here (program execution stops)
					// only two ways to continue:
					// a) packet is received (normal execution after the catch block)
					// b) timer expires after 4 timeout events (exception is thrown)
			
					InetAddress serverAddress = receivedPacket.getAddress(); // server's IP address
					int serverPort = receivedPacket.getPort(); // server's port number
					int dataLength = receivedPacket.getLength(); // the number of received bytes
					
					/*printing the received byte sequence if needed
					 * System.out.println("\nThe received byte sequence is: ");
					for(int ind = 0; ind < dataLength; ind++)
					{
						System.out.printf("%d", receivedBytes[ind]);
						if(ind < dataLength-1)
							System.out.print(", ");
					}*/
				
					// converting the received byte array into String
					String receivedText = new String(receivedBytes, 0, dataLength);
					System.out.println("\nThe received text received from the server is: ");
					System.out.println(receivedText);
					
					/*splitting the received string using CR+LF par
					 * although individual lines are also broken in text file, 
					 * we are using the length of response code and response code later in the code
					 *  to extract the contents of the file*/
					String[] delim = receivedText.split("\r\n");
				
					/*the response code is broken using CR+LF,
					 *  the first part of string is response message, 
					 *  second part is response code either 0/1/2/3/4*/
					/*the later parts contain the contents of the file and integrity check value */
					if(Integer.parseInt(delim[1])==0)
					{
						/*the delim5 string only contains the integrity check value of response message. 
						 * because we are clipping the first 4 parts
						 *  .i.e. ENTS/1.0 Response,response code,content length, contents of the text file. 
						 * additionally six is added to substring method because, it denotes 3 CR and LF pairs accompanied by the first 3 parts. 
						 * "Integer.parseInt(delim[2],10)" denotes the content length of the text file*/
						String delim5=receivedText.substring(delim[0].length()+delim[1].length()+delim[2].length()+Integer.parseInt(delim[2],10)+6);
						
						/*the dlim6 contains the characters of the selected text file.
						 *  here we are selecting the substring accordingly. 
						 *  ignoring the ENTS/1.0 Response,response code,content length, and selecting upto end of text file characters.
						 *   and we are also ignoring the integrity check value which exists in the received text*/
						String delim6=receivedText.substring(delim[0].length()+delim[1].length()+delim[2].length()+6,delim[0].length()+delim[1].length()+delim[2].length()+Integer.parseInt(delim[2],10)+6);
					
						/*calculating the integrity check value for received text,
						 *  by clipping the numeric integrity check value added to received text
						 *   and we are calculating the value only until the contents of file 
						 *   i.e. we are calculating integrity check value for 
						 *   ENTS/1.0 Response,response code,content length, and content */
						String newChecksum1=integrityCheck(receivedText.substring(0,delim[0].length()+delim[1].length()+delim[2].length()+Integer.parseInt(delim[2],10)+6));
					
						/* checking if the transmission was OK
						 *  .i.e. integrity check value that we calculated is same
						 *   as the value that we obtained at the end of received text*/
						if(newChecksum1.equals(delim5.substring(0, delim5.length()-2)))
						{
							/*Response code 0: the characters of the received file are displayed*/
							System.out.println("\nOK. The response has been created according to the request.");
							System.out.println("\nThe characters of the received file are: ");
							System.out.println(delim6+"\n");
							System.out.println("-------------------------------------------");
							
							break;
						}
						else
						{
							/*As integrity check failure occured,
							 *  we are prompting the user whether he likes to resend the request */
							System.out.print("\nError: integrity check failure. The request has one or more bit errors.");
							System.out.println("Would you like to resend the request: Y/N");
							String sel=inp.nextLine();
							if(sel.equals("Y"))
							{
								/*if user likes to resend the request, 
								 * we are sending the packet again and the process is repeated accordingly*/
								clientSocket.send(sentPacket);
							}
							else
							{
								/*as the user is not willing to resend the request, 
								 * we are prompting him to main menu to select the file
								 *  that he needs to access from the server*/
								System.out.print("prompting to the main menu after integrity check failure\n\n");
								
								break;
							}
						}
			
					}
					else if(Integer.parseInt(delim[1])==2)
					{
						/*Response code 2: syntax of the request message is incorrect*/
						System.out.print("\nError: malformed request. The syntax of the request message is not correct.\n\n");
						
						break;
					}
					else if(Integer.parseInt(delim[1])==3)
					{
						/*response code 3: non-existent file requested by the user*/
						System.out.print("\nError: non-existent file. The file with the requested name does not exist.\n\n");
						
						break;
					}
					else if(Integer.parseInt(delim[1])==4)
					{
						/*wrong protocol version (anything other than 1.0) entered by user*/
						System.out.print("\nError: wrong protocol version. The version number in the request is different from 1.0.\n\n");
						
						break;
					}
			
				}//try
				catch(InterruptedIOException e)
				{
					
					/*doubling timer value for every timeout event until 4 timeout events*/
					TIME_OUT_VALUE_MS=2*TIME_OUT_VALUE_MS;
					FLAG--;
					/*sending the packet to server again after every timeout event until 4 timeouts*/
					clientSocket.send(sentPacket);
					if(FLAG==0)
					{
						// timeout - timer expired (after 4 timeout events) before receiving the response from the server
						System.out.println("\n client socket timeout! Exception object e:"+e);
						
					}	
				}//catch
			}//while(flag)
		}//while(true)	
	}//main

	public static String requestMessage(String a,String b) throws Exception,IOException
	{
				
		/*calling userResponse() method with user selection string :
		 * A/B/C and requested file name as parameters*/
		String s1=userResponse(a,b);
		String s2=((("ENTS/1.0 Request").concat("\r\n")+s1).concat("\r\n"));
		String newChecksum=integrityCheck(s2);				
		String s3=s2+newChecksum+"\r\n";
		System.out.println("The Request message sent to the server is: ");
		System.out.println(s3);
		return s3;
	}//requestMessage() method
	public static String userResponse(String a,String b)
	{
		switch (a) 
		{
			case "A":b="directors_message.txt";
					break;
			case "B":b="program_overview.txt";
					break;
			case "C":b="scholarly_paper.txt";
				 	break;
			default:b="default.txt";
					System.out.println("-------------------------------------------");
					break;
		}
		return b;
	}//userResponse() method
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
			//System.out.println("index is"+index);
			S=(short)((7919*index)%65536);
			//System.out.println("S is"+S);
		}
			
		System.out.println("value of Integrity Check is: "+S);
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
				 * the word array has exactly half the length of char array 
				 * and has appended consecutive elements as its elements*/
				wordArray[i/2]=(String.format("%8s", Integer.toBinaryString((int) charArrayOfS2[i])).replace(' ', '0').concat(String.format("%8s", Integer.toBinaryString((int) charArrayOfS2[i+1])).replace(' ', '0')));
			}
			else
			{	
				/*if there are odd number of characters in char array, 
				 * the word array has exactly half the length of char array 
				 * and has appended consecutive elements as its elements and the last character has to be appended with 8-bit zeros "00000000"*/
				wordArray[i/2]=String.format("%8s", Integer.toBinaryString((int) charArrayOfS2[i])).replace(' ', '0')+String.format("%8s", Integer.toBinaryString((int) charArrayOfS2[i+1])).replace(' ', '0');
				wordArray[((charArrayOfS2.length)/2)-1]=String.format("%8s", Integer.toBinaryString((int) charArrayOfS2[charArrayOfS2.length-1])).replace(' ', '0')+"00000000";
			}
		}
		return wordArray;
	}//charArrayToWordArray() method
	public static int[] wordArrayToIntArray(String[] a)
	{
		/*the given wordarray has elements in string array, 
		 * so we need to convert it into integer array in order to perform calculations*/
		int[] convIntArray=new int[a.length];

		for(int i=0;i<a.length;i++)
		{
			convIntArray[i]=Integer.parseInt(a[i],2);
		}
		return convIntArray;
	}//wordArrayToIntArray() method
	
}//ClientApplication class