import java.io.*;
import java.net.*;

class ServerThread extends Thread
{
    private int threadNumber;

    public ServerThread(int threadNumber)
    {
        this.threadNumber = threadNumber;
    }

    public void run()
    {
        System.out.println("Starting server thread " + threadNumber);
        try
        {
            runServer();
        }
        catch (IOException e)
        {
            System.out.println("Uncaught IOException occurred in thread " + threadNumber);
        }
    }

    public void runServer() throws IOException
    {
        String routerName = "127.0.0.1"; // ServerRouter host name
        int routerPort = 5556; // port number
        String downloadDir = "downloads/"; // directory to save downloaded A/V files

        // Variables for setting up connection and communication
        Socket socket = null; // socket to connect with ServerRouter
        BufferedReader in = null; // for reading from ServerRouter
        PrintWriter out = null; // for writing to ServerRouter

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
            return;
        }
        catch (IOException e)
        {
            System.err.println("Couldn't get I/O for the connection to: " + routerName);
            return;
        }

        // Once a connection is established to the ServerRouter, state whether this is a Sender or Receiver
        System.out.println("Message to ServerRouter: Receiver");
        out.println("Receiver");

        // Response is the port number that the server will listen on
        String response = in.readLine();
        System.out.println("Response from ServerRouter: " + response);

        if (response.equals("ROUTING_TABLE_FULL"))
        {
        	System.out.println("Sender client could not connect; routing table was full. Exiting.");
        	return;
        }

        int receiverPort = Integer.parseInt(response);

        // Start a server socket to listen for a client
        ServerSocket serverSocket =  null;
        try
        {
            serverSocket = new ServerSocket(receiverPort);
            System.out.println("Listening for a connection on port " + receiverPort);
        }
        catch (IOException e)
        {
            System.err.println("Could not listen on port " + receiverPort);
            System.out.println("Message to ServerRouter: Failed.");
            out.println("Failed.");
            response = in.readLine();
            System.out.println("Response from ServerRouter: " + response);
            return;
        }

        // Server socket opened successfully - communicate this to ServerRouter
        System.out.println("Message to ServerRouter: Success.");
        out.println("Success.");
        response = in.readLine();
        System.out.println("Response from ServerRouter: " + response);

        // Communication with ServerRouter finished
        in.close();
        out.close();
        socket.close();

        if (response.equals("Added."))
        {
            System.out.println("Waiting for connection from client...");
        }
        else
        {
            System.out.println("Unexpected response from ServerRouter. Exiting.");
            return;
        }

        // Accept the first client and close the server socket
        try
        {
            socket = serverSocket.accept();
            System.out.println("Accepted a connection from " + socket.getInetAddress().getHostAddress());
        }
        catch (IOException e)
        {
            System.err.println("Error occurred when accepting a connection.");
            serverSocket.close();
            return;
        }

        serverSocket.close();

        // Make new reader/writer for message passing with sender client
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Variables for message passing
        String fromServer; // messages sent to other client
        String fromClient; // messages received from other client

        String fileName = null;

        // Communication while loop
        while ((fromClient = in.readLine()) != null)
        {
            System.out.println("Client: " + fromClient);
            if (fromClient.equals("Bye.")) // exit statement
            {
                System.out.println("Server: Bye.");
                out.println("Bye.");
                break;

            }
            else if (fromClient.startsWith("!FILENAME:"))
            {
                fileName = fromClient.substring(10);
                System.out.println("Received file name: " + fileName);
                System.out.println("Server: Received.");
                out.println("Received.");
            }
            else if (fromClient.startsWith("!BYTES:"))
            {
                if (fileName == null)
                {
                    System.out.println("File name was never received.");
                    out.println("Bye.");
                    break;
                }

                // Get the number of bytes from the string first
                String numBytesStr = fromClient.substring(7);
                int numBytes = Integer.parseInt(numBytesStr);

                System.out.println("Server: Ready.");
                out.println("Ready.");

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
                String filePath = downloadDir + fileName;
                BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(filePath));
                fileOut.write(data, 0, numBytes);

                System.out.println("Wrote " + numBytes + " bytes to file " + filePath);

                if (fileOut != null) fileOut.close();

                // Close the connection after file is received
                System.out.println("Server: Success.");
                out.println("Success.");
            }
            else
            {
                fromServer = fromClient.toUpperCase(); // converting received message to upper case
                System.out.println("Server: " + fromServer);
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

public class ThreadedServer
{
    public static void main(String[] args)
    {
        int numServers = 50;
        if (args.length == 1)
        {
            numServers = Integer.parseInt(args[0]);
        }
        else if (args.length > 1)
        {
            System.err.println("Expected up to one argument: [number of server threads]");
        }

        for (int i = 0; i < numServers; i++)
        {
            ServerThread t = new ServerThread(i);
            t.start();
        }
    }
}
