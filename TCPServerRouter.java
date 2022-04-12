import java.net.*;
import java.io.*;

public class TCPServerRouter
{
    public static void main(String[] args) throws IOException
    {
        boolean reciever = false;
        Socket clientSocket = null; // socket for the thread
        Object [][] RoutingTable = new Object [10][2]; // routing table
        int SockNum = 5555; // port number
        String otherSRouter = "192.0.0.1";
        Boolean Running = true;
        int ind = 0; // indext in the routing table

        //Accepting connections
        ServerSocket serverSocket = null; // server socket for accepting connections
        Socket outSocket;
        if(reciever)
        {
            ServerSocket inSocket = new ServerSocket(5555);
            outSocket = inSocket.accept();
        }
        else
        {
            outSocket = new Socket(otherSRouter, 5555);
        }

        BufferedReader SRouterBR = new BufferedReader(new InputStreamReader(outSocket.getInputStream()));
        PrintWriter SRouterPR = new PrintWriter(outSocket.getOutputStream(), true);

        try
        {
            serverSocket = new ServerSocket(5555);
            System.out.println("ServerRouter is Listening on port: 5555.");
        }
        catch (IOException e)
        {
            System.err.println("Could not listen on port: 5555.");
            System.exit(1);
        }

        // Creating threads with accepted connections
        while (Running == true)
        {
            try
            {
                clientSocket = serverSocket.accept();
                System.out.println("ServerRouter connected with Client/Server: " + clientSocket.getInetAddress().getHostAddress());

                BufferedReader clientBR = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter clientPR = new PrintWriter(clientSocket.getOutputStream(), true);

                String addr = clientSocket.getInetAddress().toString();
                String port = clientBR.readLine();

                RoutingTable[ind][0] = addr;
                RoutingTable[ind][1] = port;

                SRouterPR.println(addr);
                SRouterPR.println(port);
                String newAddr = SRouterBR.readLine();
                String newPort = SRouterBR.readLine();

                ind++;

                RoutingTable[ind][0] = newAddr;
                RoutingTable[ind][1] = newPort;

                ind++;

                clientPR.println(newAddr);
                clientPR.println(newPort);

                //SThread t = new SThread(RoutingTable, clientSocket, ind); // creates a thread with a random port
                //t.start(); // starts the thread
            }
            catch (IOException e)
            {
                System.err.println("Client/Server failed to connect.");
                System.exit(1);
            }
        }//end while

        //closing connections
        outSocket.close();
        clientSocket.close();
        serverSocket.close();
    }
}
