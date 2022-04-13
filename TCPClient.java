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
        PrintWriter out = null; // for writing to ServerRouter
        BufferedReader in = null; // for reading form ServerRouter
        //InetAddress addr = InetAddress.getLocalHost();
        //String host = addr.getHostAddress(); // Client machine's IP

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

        // Make connection to Receiver client
        // TODO: Try/catch
        socket = new Socket(receiverAddress, receiverPort);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        // Variables for message passing
        String fromServer; // messages received from ServerRouter
        String fromUser; // messages sent to ServerRouter
        //String address = "192.168.0.101"; // destination IP (Server) - laptop

        
        // Communication process (initial sends/receives)
        /*out.println(address); // initial send (IP of the destination Server)
        fromServer = in.readLine(); // initial receive from router (verification of connection)
        System.out.println("ServerRouter: " + fromServer);

        String rtTime = "";
        fromServer = in.readLine(); // receive RTTime
        System.out.println("ServerRouter: " + fromServer);
        if (fromServer.startsWith("!RTTime"))
        {
            rtTime = fromServer.substring(9);
        }

        out.println(host); // Client sends the IP of its machine as initial send
		*/

        if (fileName.endsWith(".txt"))
        {
            long fileSize = f.length();

            Reader reader = new FileReader(fileName);
            BufferedReader fromFile =  new BufferedReader(reader); // reader for the string file
            long t0, t1, t;

            t = 0;
            t0 = System.currentTimeMillis();

            int charCount = 0;
            int numMsg = 0;
            long startTime = System.currentTimeMillis();
            
            // ADDED - client now sends first
            fromUser = fromFile.readLine();
            System.out.println("Client: " + fromUser);
            out.println(fromUser);

            // Communication while loop
            while ((fromServer = in.readLine()) != null)
            {
                System.out.println("Server: " + fromServer);
                t1 = System.currentTimeMillis();
                if (fromServer.equals("Bye.")) // exit statement
                    break;
                /*else if(fromServer.startsWith("!RTTime"))
                {
                    rtTime = fromServer.substring(9);
                }*/

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
                    System.out.println("File read finished. Sending 'Bye.'.");
                    out.println("Bye.");
                    fromFile.close();
                    break;
                }
            }

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;

            int avgMsgSize = charCount / numMsg;
            long avgMsgTime = totalTime / numMsg;

            //Write stuff to CSV file
            /*File csv = new File("data.csv");
            csv.createNewFile();

            BufferedWriter outCsv = new BufferedWriter(new FileWriter(csv, true));
            outCsv.write(fileName + "," + avgMsgSize + "," + avgMsgTime + "," + rtTime + "," + fileSize + "\n");
            outCsv.close();*/
        }
        else
        {
            // Receive first message (server's host) from server
            if ((fromServer = in.readLine()) != null)
                System.out.println("Server: " + fromServer);

            // Send the server the name of the file
            fromUser = "!FILENAME:" + fileName;
            System.out.println("Client: " + fromUser);
            out.println(fromUser);

            // Read the file as bytes
            byte[] data = new byte[(int) f.length()];
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
            bis.read(data, 0, data.length);
            bis.close();

            // Send the server the size of the file
            fromUser = "!BYTES:" + data.length;
            System.out.println(fromUser);
            out.println(fromUser);

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
            }

            long t1 = System.currentTimeMillis();
            long t = t1 - t0;

            System.out.println("RTT: " + t);

            //Write stuff to CSV file
            /*File csv = new File("data.csv");
            csv.createNewFile();

            BufferedWriter outCsv = new BufferedWriter(new FileWriter(csv, true));
            outCsv.write(fileName + "," + data.length + "," + t + "," + rtTime + "\n");
            outCsv.close();*/
        }

        System.out.println("Closing connection.");
        // closing connections
        out.close();
        in.close();
        socket.close();
    }
}
