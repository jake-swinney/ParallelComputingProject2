import java.io.*;
import java.net.*;

public class TCPClient
{
    public static void main(String[] args) throws IOException
    {
        if (args.length == 0)
        {
            System.out.println("One argument required: fileName");
            System.exit(1);
        }

        String fileName = args[0];
        File f = new File(fileName);
        if (!f.exists() || f.isDirectory())
        {
            System.out.println("File could not be opened");
            System.exit(1);
        }

        String routerName = "127.0.0.1"; // ServerRouter host name
        int routerPort = 5555; // port number

        // Variables for setting up connection and communication
        Socket socket = null; // socket to connect with ServerRouter
        BufferedReader in = null; // for reading from ServerRouter
        PrintWriter out = null; // for writing to ServerRouter

        // Lookup timing
        long lookup_t0 = System.currentTimeMillis();
        
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
        System.out.println("Message to ServerRouter: Sender");
        out.println("Sender");
        
        // Response is the IP and port of the receiver client that is found
        String response = in.readLine();
        System.out.println("Response from ServerRouter: " + response);
        
        if (response.equals("NO_RECEIVER_AVAILABLE"))
        {
        	System.out.println("Client could not connect; no receiver was available. Exiting.");
        	System.exit(1);
        }
        
        String[] parts = response.split(":");
        
        String receiverAddress = parts[0];
        int receiverPort = Integer.parseInt(parts[1]);
        
        // Communication with ServerRouter finished
        in.close();
        out.close();
        socket.close();
        
        long lookupTime = System.currentTimeMillis() - lookup_t0;
        System.out.println("Lookup time: " + lookupTime);

        // Make connection to Receiver client
        try
        {
            socket = new Socket(receiverAddress, receiverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        catch (UnknownHostException e)
        {
            System.err.println("Unknown host for client at " + receiverAddress + ":" + receiverPort);
            System.exit(1);
        }
        catch (IOException e)
        {
            System.err.println("Couldn't get I/O for the connection to " + receiverAddress + ":" + receiverPort);
            System.exit(1);
        }
        
        System.out.println("Connected with receiver client at " + receiverAddress + ":" + receiverPort);
        
        // Variables for message passing
        String fromServer; // messages received from ServerRouter
        String fromUser; // messages sent to ServerRouter

        if (fileName.endsWith(".txt"))
        {
            Reader reader = new FileReader(fileName);
            BufferedReader fromFile =  new BufferedReader(reader); // reader for the string file

            // Metrics
            long t0 = System.currentTimeMillis();
            long t1, t;
            long startTime = System.currentTimeMillis();
            long endTime = 0;
            int charCount = 0;
            int numMsg = 0;
            
            // Client sends first line of the file
            fromUser = fromFile.readLine();
            numMsg++;
            charCount += fromUser.length();
            System.out.println("Client: " + fromUser);
            out.println(fromUser);

            // Communication while loop
            while ((fromServer = in.readLine()) != null)
            {
                System.out.println("Server: " + fromServer);
                t1 = System.currentTimeMillis();
                if (fromServer.equals("Bye.")) // exit statement
                    break;

                t = t1 - t0;
                System.out.println("Cycle time: " + t);

                fromUser = fromFile.readLine(); // reading strings from a file
                if (fromUser != null)
                {
                    numMsg++;
                    charCount += fromUser.length();
                    System.out.println("Client: " + fromUser);
                    out.println(fromUser); // sending the strings to the Server via ServerRouter
                    t0 = System.currentTimeMillis();
                }
                else
                {
                    endTime = System.currentTimeMillis();
                    System.out.println("File read finished. Sending 'Bye.'.");
                    out.println("Bye.");
                    fromFile.close();
                }
            }

            long totalTime = endTime - startTime;
            int avgMsgSize = charCount / numMsg;
            long avgMsgTime = totalTime / numMsg;

            System.out.println("\n-- Metrics --");
            System.out.println("File Name: " + fileName);
            System.out.println("File Size: " + f.length());
            System.out.println("Average Message Size: " + avgMsgSize);
            System.out.println("Average Message Time: " + avgMsgTime);
            System.out.println("Lookup Time: " + lookupTime);
            
            // Write stuff to CSV file
            File csv = new File("data_txt.csv");
            csv.createNewFile();

            BufferedWriter outCsv = new BufferedWriter(new FileWriter(csv, true));
            outCsv.write(fileName + "," + f.length() + "," + avgMsgSize + "," + avgMsgTime + "," + lookupTime + "\n");
            outCsv.close();
        }
        else
        {
            // Send the server the name of the file
            fromUser = "!FILENAME:" + fileName;
            System.out.println("Client: " + fromUser);
            out.println(fromUser);
            
            // Expect "Received." response from server
            fromServer = in.readLine();
            System.out.println("Server: " + fromServer);
            if (!fromServer.equals("Received."))
            {
                System.err.println("Unexpected response from server. Exiting.");
                System.exit(1);
            }

            // Read the file as bytes
            byte[] data = new byte[(int) f.length()];
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
            bis.read(data, 0, data.length);
            bis.close();

            // Send the server the size of the file
            fromUser = "!BYTES:" + data.length;
            System.out.println(fromUser);
            out.println(fromUser);
            
            // Expect "Ready." response from server
            fromServer = in.readLine();
            System.out.println("Server: " + fromServer);
            if (!fromServer.equals("Ready."))
            {
                System.err.println("Unexpected response from server. Exiting.");
                System.exit(1);
            }

            // Set start time and begin sending the file
            long t0 = System.currentTimeMillis();

            OutputStream os = socket.getOutputStream();
            os.write(data, 0, data.length);
            os.flush();

            System.out.println("Sent " + data.length + " bytes.");

            while ((fromServer = in.readLine()) != null)
            {
                System.out.println("Server: " + fromServer);
                if (fromServer.equals("Bye."))
                    break;
                System.out.println("Client: Bye.");
                out.println("Bye.");
            }

            long t1 = System.currentTimeMillis();
            long t = t1 - t0;
            
            long bytesPerSec = f.length() / t * 1000;

            System.out.println("\n-- Metrics --");
            System.out.println("File Name: " + fileName);
            System.out.println("File Size: " + f.length());
            System.out.println("Transfer Time: " + t);
            System.out.println("B/s: " + bytesPerSec);
            System.out.println("Lookup Time: " + lookupTime);

            //Write stuff to CSV file
            File csv = new File("data_av.csv");
            csv.createNewFile();

            BufferedWriter outCsv = new BufferedWriter(new FileWriter(csv, true));
            outCsv.write(fileName + "," + data.length + "," + t + "," + bytesPerSec + "," + lookupTime + "\n");
            outCsv.close();
        }

        System.out.println("Closing connection.");
        // closing connections
        out.close();
        in.close();
        socket.close();
    }
}
