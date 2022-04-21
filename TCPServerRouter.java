// One TCPServerRouter should be on port 5555 and the other on port 5556.

import java.net.*;
import java.io.*;

public class TCPServerRouter
{
	public static final int ROUTING_SIZE = 100;
	
    public static void main(String[] args) throws IOException
    {
        // Port number for this router and information for other router
        int port = 5555;
        String otherRouterAddress = "127.0.0.1";
        int otherRouterPort = 5556;
        
        // Parse arguments for port and other router port/address if given
        if (args.length == 0)
        {
            System.out.println("Using defaults.");
        }
        else if (args.length == 3)
        {
            try
            {
                port = Integer.parseInt(args[0]);
                otherRouterPort = Integer.parseInt(args[2]);
            }
            catch (NumberFormatException e)
            {
                System.err.println("Arguments: [port] [other router address] [other router port]");
                System.exit(1);
            }
            otherRouterAddress = args[1];
        }
        
        // Routing table and information
        Object[][] RoutingTable = new Object[ROUTING_SIZE][2];
        int firstEntry = 0;
        int lastEntry = 0;
        int routingCount = 0;

        //Accepting connections
        ServerSocket serverSocket = null; // server socket for accepting connections
        try
        {
            serverSocket = new ServerSocket(port);
            System.out.println("ServerRouter is listening on port " + port + ".");
        }
        catch (IOException e)
        {
            System.err.println("Could not listen on port: " + port + ".");
            System.exit(1);
        }
        
        System.out.println("Other ServerRouter will be located at " + otherRouterAddress + ":" + otherRouterPort);
        System.out.println();

        Socket clientSocket = null; // socket for a client that connects to this server
        boolean running = true;
        
        // Creating threads with accepted connections
        while (running)
        {
            try
            {
                clientSocket = serverSocket.accept();
                
                System.out.println("Received connection from " + clientSocket.getInetAddress().getHostAddress());
                
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
                String message = in.readLine();
                
                System.out.println("Message Received: " + message);
                
                if (message.equals("Receiver"))
                {
                	if (routingCount < ROUTING_SIZE)
                	{
                    	// Set the port that the receiver will use, based on ServerRouter's port and index in routing table
                    	int receiverPort = port + lastEntry + 1;
                    	
                    	// Add the receiver to the routing table
                    	RoutingTable[lastEntry][0] = clientSocket.getInetAddress().getHostAddress();
                    	RoutingTable[lastEntry][1] = receiverPort;
                    	
                    	// Increment the routing count and update the last index
                    	lastEntry = (lastEntry + 1) % ROUTING_SIZE;
                    	routingCount++;
                    	
                    	String response = receiverPort + "";
                    	System.out.println("ServerRouter Response: " + response);
                    	out.println(response);
                    	
                		System.out.println("Routing table entries: " + routingCount);
                	}
                	else
                	{
                		out.println("ROUTING_TABLE_FULL");
                		System.out.println("ServerRouter Response: ROUTING_TABLE_FULL");
                	}
                }
                else if (message.equals("Sender"))
                {
                	// Connect to other server router and send "Lookup"
                	System.out.println("Connecting to ServerRouter at " + otherRouterAddress + ":" + otherRouterPort);
                	Socket otherRouter = new Socket(otherRouterAddress, otherRouterPort);

                    BufferedReader inOther = new BufferedReader(new InputStreamReader(otherRouter.getInputStream()));
                    PrintWriter outOther = new PrintWriter(new OutputStreamWriter(otherRouter.getOutputStream()), true);

                    System.out.println("Message to ServerRouter: Lookup");
                    outOther.println("Lookup");
                    
                    // Receive response from other server router and send it back to client
                    String response = inOther.readLine();
                    System.out.println("Message from ServerRouter: " + response);
                    out.println(response);
                    System.out.println("Forwarded to Client: " + response);
                    
                    // Close when finished
                    inOther.close();
                    outOther.close();
                    otherRouter.close();
                }
                else if (message.equals("Lookup"))
                {
                	// Received from other ServerRouter - looking for a Receiver client
                	if (routingCount > 0)
                	{
                		// Send back the IP address and port number of the client and remove it from the routing table
                		String response = RoutingTable[firstEntry][0] + ":" + RoutingTable[firstEntry][1];
                		
                		firstEntry++;
                		routingCount--;

                		System.out.println("ServerRouter Response: " + response);
                		out.println(response);
                		
                		System.out.println("Routing table entries: " + routingCount);
                	}
                	else
                	{
                		// No client available to receive
                		out.println("NO_RECEIVER_AVAILABLE");
                		System.out.println("ServerRouter Response: NO_RECEIVER_AVAILABLE");
                	}
                }
                
                System.out.println();
                
                // Close when finished
                in.close();
                out.close();
                clientSocket.close();
                
                /*SThread t = new SThread(RoutingTable, clientSocket, ind); // creates a thread with a random port
                t.start(); // starts the thread
                ind++; // increments the index
                System.out.println("ServerRouter connected with Client/Server: " + clientSocket.getInetAddress().getHostAddress());*/
            }
            catch (IOException e)
            {
                System.err.println("Client/Server failed to connect.");
                e.printStackTrace();
                System.exit(1);
            }
        }//end while

        //closing connections
        serverSocket.close();
    }
}
