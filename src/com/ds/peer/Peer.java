package com.ds.peer;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import com.ds.peer.Commons.Messages;
import com.ds.peer.Commons.ServerType;

public class Peer
{
	
	public static void main(String[] args)
	{
		Registry registry = null;
		int port;
		GameIntf stub;
		ServerType playerType = ServerType.PLAYER;
		try
		{
			if(args.length==3) //Primary Server
			{
				playerType = ServerType.PRIMARY;
				int n,m;
				n = Integer.parseInt(args[0]);
				m = Integer.parseInt(args[1]);
				if (n<=0 || m<=0)
				{
					System.out.println("Please enter valid inputs for N and M");
					System.exit(0);
				}
				GameImpl obj = new GameImpl(n,m);
				new PeerServer(obj,1,ServerType.PRIMARY,true);
				//Thread t = new Thread(new PeerServer(obj,ServerType.PRIMARY,true));
				//t.start();
				
			    
			}
			
			else	//Players connecting
			{
				
				
				
				
			}
			
			port = 1234;
			//System.out.println("Before getRegistry(); req in Peer");
			registry = LocateRegistry.getRegistry(port);
			stub = (GameIntf) registry.lookup("MazeGame");
			//System.out.println("After lookup(); req in Peer");
			
			
			final PeerPlayerImpl peerPlayerObj = new PeerPlayerImpl(stub);
		    UnicastRemoteObject.exportObject((PeerPlayerIntf)peerPlayerObj, 0);
		    //System.out.println("Before join(); req in Peer");
		    ContainerIntf response = stub.joinGame((PeerPlayerIntf)peerPlayerObj);
		    
		    
		    if(!response.getMessage().contains(Messages.GAME_ACCEPTED.toString()))
		    {
		    	System.out.println(response.getMessage());
		    	System.exit(0);
		    }
		    
		    if(response.getMessage().contains(Messages.BACKUP.toString()))
		    {
		    	System.out.println("I am Backup server");
		    	GameImpl obj = response.getGameObj();
		    	new PeerServer(obj,2,ServerType.BACKUP,true);
		    }
	    	
		    System.out.println(response.getMessage());
		    System.out.println("Please wait for others to connect (enter \"q\" if you want to quit )");
		    peerPlayerObj.prepareGUI();
		}
		catch (NumberFormatException nfe)
		{
			System.out.println("Please Enter valid numbers for N and M");
		}
		catch (RemoteException e) {
			
			//e.printStackTrace();
		} catch (NotBoundException e) {
			
			//e.printStackTrace();
		}
		
		
		/*..
		String host = (args.length < 1) ? null : args[0];
		try 
		{
		    Registry registry = LocateRegistry.getRegistry(host,1234);
		    GameIntf stub;
			try {
				stub = (GameIntf) registry.lookup("MazeGame");
				System.out.println(stub.check());
			} catch (NotBoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    
		}
		catch(RemoteException e)
		{
			e.printStackTrace();
		}
		*/ 
	}
}

