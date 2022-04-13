import java.io.*;
import java.net.*;

public class TCPServer
{
    public static void main(String[] args) throws IOException
    {
        String routerName = "127.0.0.1"; // ServerRouter host name
        int routerPort = 5556; // port number
        
        // Variables for setting up connection and communication
        Socket socket = null; // socket to connect with ServerRouter
        PrintWriter out = null; // for writing to ServerRouter
        BufferedReader in = null; // for reading form ServerRouter
        //InetAddress addr = InetAddress.getLocalHost();
        //String host = addr.getHostAddress(); // Server machine's IP

        // Tries to connect to the ServerRouter
        try
        {
            socket = new Socket(routerName, routerPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        catch (UnknownHostException e)
        {
            System.err.println("Don't know about router: " + routerName);
            System.exit(1);
        }
        catch (IOException e)
        {
            System.err.println("Couldn't get I/O for the connection to: " + routerName);
            System.exit(1);
        }
        
        // Once a connection is established to the ServerRouter, state whether this is a Sender or Receiver
        System.out.println("Message to ServerRouter: Receiver");
        out.println("Receiver");
        
        // Response is the port number that the server will listen on
        String response = in.readLine();
        System.out.println("Response from ServerRouter: " + response);
        
        //while (response == null)
        //	response = in.readLine();
        
        if (response.equals("ROUTING_TABLE_FULL"))
        {
        	System.out.println("Sender client could not connect; routing table was full. Exiting.");
        	System.exit(1);
        }
        
        int receiverPort = Integer.parseInt(response);
        
        // Communication with ServerRouter finished
        in.close();
        out.close();
        socket.close();
        
        // Start a ServerSocket to listen for a client. Close after opening a socket to the first client.
        System.out.println("Opening a ServerSocket on port " + receiverPort);
        ServerSocket serverSocket = new ServerSocket(receiverPort);
        socket = serverSocket.accept();
        serverSocket.close();
        
        // Make new reader/writer for message passing with sender client
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Variables for message passing
        String fromServer; // messages sent to ServerRouter
        String fromClient; // messages received from ServerRouter
        //String address = "192.168.0.103"; // destination IP (Client) - desktop

        // Communication process (initial sends/receives)
        /*out.println(address); // initial send (IP of the destination Client)
        fromClient = in.readLine(); // initial receive from router (verification of connection)
        System.out.println("ServerRouter: " + fromClient);

        fromServer = in.readLine(); // receive RTTime
        System.out.println("ServerRouter: " + fromServer);*/

        String fileName = null;

        // Communication while loop
        while ((fromClient = in.readLine()) != null)
        {
            System.out.println("Client said: " + fromClient);
            if (fromClient.equals("Bye.")) // exit statement
                break;
            else if (fromClient.startsWith("!FILENAME:"))
            {
                fileName = fromClient.substring(10);
                System.out.println("Received file name: " + fileName);
            }
            else if (fromClient.startsWith("!BYTES:"))
            {
                if (fileName == null)
                {
                    System.out.println("File name was never received.");
                    break;
                }

                // Get the number of bytes from the string first
                String numBytesStr = fromClient.substring(7);
                int numBytes = Integer.parseInt(numBytesStr);
                byte[] data = new byte[numBytes];  // the buffer for the data

                // Receive data
                int bytesRead;
                int current;

                System.out.println("Receiving " + numBytes + " bytes.");

                InputStream inStream = socket.getInputStream();

                bytesRead = inStream.read(data, 0, numBytes);
                current = bytesRead;

                do {
                    bytesRead = inStream.read(data, current, (numBytes - current));
                    if (bytesRead > 0) current += bytesRead;
                } while (bytesRead > -1 && current < numBytes);

                System.out.println("Bytes received.");

                // Write data to file
                BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(fileName));
                fileOut.write(data, 0, numBytes);

                System.out.println("Wrote " + numBytes + " bytes to file " + fileName);

                if (fileOut != null) fileOut.close();

                // Close the connection after file is received
                fromServer = "Bye.";
                System.out.println("Server said: " + fromServer);
                out.println(fromServer);
                break;
            }
            else
            {
                fromServer = fromClient.toUpperCase(); // converting received message to upper case
                System.out.println("Server said: " + fromServer);
                out.println(fromServer); // sending the converted message back to the Client via ServerRouter
            }
        }

        System.out.println("Closing connection.");
        // closing connections
        out.close();
        in.close();
        socket.close();
    }
}
