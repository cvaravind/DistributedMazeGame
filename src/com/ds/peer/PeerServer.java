package com.ds.peer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import com.ds.peer.Commons.ServerType;


public class PeerServer implements Runnable
{
	//GameImpl gameObj;
	GameIntf stub;
	//GameIntf anotherServerStub;
	Registry registry = null;
	int port;
	//ServerType serverType;
	
	public PeerServer(GameImpl gameObj, int pid,/* int existingPort ,*/ ServerType serverType,boolean bootstrap)
	{
		//gameObj = obj;	
		//serverType = stype;
		
		try 
		{
	//System.out.println("came here: ");
			gameObj.serverType = serverType;			
			gameObj.setMyPeerID(pid);
		
			stub = (GameIntf) UnicastRemoteObject.exportObject(gameObj, 0);
		    //registry = LocateRegistry.getRegistry();
		    if(serverType == ServerType.PRIMARY && bootstrap)
		    {
		    	
		    	
			    port = 1234;//(existingPort==Commons.PORT1) ? Commons.PORT2 : Commons.PORT1;
			    registry = LocateRegistry.createRegistry(port);
			    //registry = LocateRegistry.getRegistry();
			    registry.rebind("MazeGame", stub);
		    }
		    else if (serverType == ServerType.BACKUP && bootstrap)
		    {
		    	//stub = (GameIntf) UnicastRemoteObject.exportObject(gameObj, 0);
		    	port = 1234;
		    	registry = LocateRegistry.getRegistry(port);
		    	gameObj.anotherServerStub = (GameIntf) registry.lookup("MazeGame");
		    	gameObj.anotherServerStub.informBStubToPrimary(stub);
		    	//gameObj.startServerPingThread();
		    	
		    }
		    else if (serverType == ServerType.PRIMARY && bootstrap==false)
		    {
		    	
		    }
		    else if (serverType == ServerType.BACKUP && bootstrap==false)
		    {
		    	
		    }
		    
		    gameObj.startServerPingThread();
		    
		    
		    System.err.println("Server ready");
		} 
		
		catch (Exception e) 
		{
		    try
		    {
				//System.out.println("Inside exception............................");
				//e.printStackTrace();
				registry.unbind("MazeGame");
				registry.bind("MazeGame",stub);
		    	System.err.println("Server ready");
		    }
		    catch(Exception ee)
		    {
		    	System.err.println("Server exception: " + ee.toString());
		    	//ee.printStackTrace();
		    }
		}
	}

	public GameIntf getStub() {
		return stub;
	}

	@Override
	public void run() {
		
		//System.out.println("Came to run");
		
	}
}
