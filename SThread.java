import java.io.*;
import java.net.*;
import java.lang.Exception;


public class SThread extends Thread
{
    private Object [][] RTable; // routing table
    private PrintWriter out, outTo; // writers (for writing back to the machine and to destination)
    private BufferedReader in; // reader (for reading from the machine connected to)
    private String inputLine, outputLine, destination, addr; // communication strings
    private Socket outSocket; // socket for communicating with a destination
    private int ind; // indext in the routing table

    // Input socket
    private Socket inSocket;

    // Constructor
    SThread(Object [][] Table, Socket toClient, int index) throws IOException
    {
        out = new PrintWriter(toClient.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(toClient.getInputStream()));
        RTable = Table;
        addr =  toClient.getInetAddress().getHostAddress();
        RTable[index][0] = addr; // IP addresses
        RTable[index][1] = toClient; // sockets for communication
        ind = index;
        inSocket = toClient;
	}

    // Run method (will run for each machine that connects to the ServerRouter)
    public void run()
    {
        try
        {
            // Initial sends/receives
            destination = in.readLine(); // initial read (the destination for writing)
            System.out.println("Forwarding to " + destination);
            out.println("Connected to the router."); // confirmation of connection

            // waits 10 seconds to let the routing table fill with all machines' information
            try
            {
                Thread.currentThread().sleep(10000);
            }
            catch(InterruptedException ie)
            {
                System.out.println("Thread interrupted");
            }

            long t0 = System.currentTimeMillis();

            // loops through the routing table to find the destination
            for ( int i=0; i<10; i++)
            {
                if (destination.equals((String) RTable[i][0]))
                {
                    outSocket = (Socket) RTable[i][1]; // gets the socket for communication from the table
                    System.out.println("Found destination: " + destination);
                    outTo = new PrintWriter(outSocket.getOutputStream(), true); // assigns a writer
                }
            }

            long t1 = System.currentTimeMillis();
            long t = t1 - t0;
            outTo.println("!RTTime: " + t);

            // Communication loop
            while ((inputLine = in.readLine()) != null)
            {
                System.out.println("Client/Server said: " + inputLine);
                if (inputLine.equals("Bye.")) // exit statement
                {
                    outTo.println("Bye."); // Send first 'Bye.' to the other.
                    break;
                }
                outputLine = inputLine; // passes the input from the machine to the output string for the destination

                if (outSocket != null)
                {
                    outTo.println(outputLine); // writes to the destination
                    if (inputLine.startsWith("!BYTES:"))
                    {
                        String numBytesStr = inputLine.substring(7);
                        int numBytes = Integer.parseInt(numBytesStr);
                        byte[] data = new byte[numBytes];
                        int bytesRead;
                        int current = 0;

                        System.out.println("Receiving " + numBytes + " bytes.");

                        InputStream inStream = inSocket.getInputStream();

                        do {
                            bytesRead = inStream.read(data, current, (numBytes - current));
                            if (bytesRead > 0)
                                current += bytesRead;
                        } while (bytesRead > -1 && current < numBytes);

                        System.out.println("Sending bytes.");

                        OutputStream outStream = outSocket.getOutputStream();
                        outStream.write(data, 0, numBytes);
                        outStream.flush();
                    }
                }
            } // end while
        } // end try
        catch (IOException e)
        {
            System.err.println("Could not listen to socket.");
            System.exit(1);
        }
    }
}
