package com.ds.peer;

import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.ds.peer.Commons.Messages;
import com.ds.peer.Commons.ServerType;


class WaitingThread implements Runnable, Serializable
{
	private GameImpl callbackObj;
	public WaitingThread(GameImpl obj)
	{
		callbackObj = obj;
	}
	public void run()
	{
		//System.out.println("Thread started");
		try 
		{
			Thread.sleep(20000);	//Initial join waiting time
		} 
		catch (Exception e) 
		{
			//System.out.println("Exception at Waiting thread: "+e.getMessage());
		}
		
		
		callbackObj.waitingOver();
		callbackObj.initializeGameDetails();
		
	}
}

class TimerThread implements Runnable, Serializable
{
	private GameImpl gameObj;
	public TimerThread(GameImpl obj)
	{
		gameObj = obj;
	}
	
	public void run()
	{
		while(gameObj.isMoveReady)
		{
			//System.out.println("TimerThread executing");
			try 
			{
				Thread.sleep(2000);	//Client Inspection Interval
			}
			catch(InterruptedException e)
			{
				//System.out.println("**Awaking thread");
				break;
			}
			catch (Exception e) 
			{
				//System.out.println("Exception at TimerThread: "+e.getMessage());
				break;
			}
			
			if(gameObj.isMoveReady==false)
				break;
			
			//check client's latest request time
			Long currentTime = System.currentTimeMillis();
			//for(int pid : gameObj.clientObjMap.keySet())
			Iterator<Map.Entry<Integer,PeerPlayerIntf>> iter = gameObj.clientObjMap.entrySet().iterator();
			while(iter.hasNext())
			{
				Map.Entry<Integer,PeerPlayerIntf> entry = iter.next();
				int pid = entry.getKey();
				if(currentTime - gameObj.clientRecentRequestTimeMap.get(pid) >= 5000)	//Client's allowed idle time
				{
					//System.out.println("Sending ping request to player: "+pid);
					try 
					{
						//gameObj.clientObjMap.get(pid).ping();
						entry.getValue().ping();
					} 
					catch (RemoteException e) 
					{
						System.out.println("Player "+ pid + " has crashed");
						
						gameObj.lock.lock();
						try
						{
							gameObj.clientLivenessMap.put(pid, false);
							gameObj.clientRecentRequestTimeMap.remove(pid);
							iter.remove();
						}
						finally
						{
							gameObj.lock.unlock();
						}
						System.out.println("Player "+ pid + " has been removed from list");
						
					}
					catch (Exception e) 
					{
						//System.out.println("Exception at TimerThread - iterator loop");
						//e.printStackTrace();
					}
				}
			}
			
		}	
		
		while(gameObj.isGameOver==false);
		
		//Find the winner and inform the players that game is Over
		
		gameObj.lock.lock();
		try
		{
			Iterator<PlayerIntf> iter = gameObj.gameDetailsObj.playerList.iterator();
			while(iter.hasNext())
			{
				PlayerIntf p = iter.next();
				if(gameObj.clientLivenessMap.get(p.getPId())==false)
				{
					iter.remove();
				}
			}
			
			boolean isGameOverInformed = false;
			while(isGameOverInformed==false)
			{
				int max = gameObj.gameDetailsObj.findMaxTreasures();
				
				int winnerPlayerID = 0;
				long min = System.currentTimeMillis();
				for(PlayerIntf player : gameObj.gameDetailsObj.playerList)
				{
					if(player.getNumOfTreasures()==max && gameObj.clientLivenessMap.get(player.getPId()) == true)
					{
						if(gameObj.clientRecentRequestTimeMap.get(player.getPId())<min)
						{
							min = gameObj.clientRecentRequestTimeMap.get(player.getPId());
							winnerPlayerID = player.getPId();
						}
					}
				}
				try
				{
					if(winnerPlayerID>0)
					{
						gameObj.clientObjMap.get(winnerPlayerID).ping();
						System.out.println("Going to broadcast that game is over. Winner is player: "+ winnerPlayerID);

						gameObj.clientObjMap.get(winnerPlayerID).gameOverNotify(winnerPlayerID);
						for(int pid : gameObj.clientObjMap.keySet())
						{
							if(pid != winnerPlayerID)
							{
								gameObj.clientObjMap.get(pid).gameOverNotify(winnerPlayerID);
							}
						}
						isGameOverInformed = true;
					}
				}
				catch(RemoteException rexcep)
				{
					System.out.println("Player "+ winnerPlayerID + " is not alive anymore. Finding the next winner");
					
					Iterator<PlayerIntf> iter2 = gameObj.gameDetailsObj.playerList.iterator();
					while(iter2.hasNext())
					{
						PlayerIntf p = iter2.next();
						if(p.getPId()==winnerPlayerID)
						{
							gameObj.clientLivenessMap.put(winnerPlayerID,false);
							iter2.remove();
						}
					}
					
				}
				
				
			}
			
		}
		finally
		{
			gameObj.lock.unlock();
		}
		
		
	}
}

public class GameImpl implements GameIntf, Serializable
{
	List<PlayerIntf> clientList = new ArrayList<PlayerIntf>();

	HashMap<Integer,PeerPlayerIntf> clientObjMap = new HashMap<Integer,PeerPlayerIntf>();
	HashMap<Integer,Boolean> clientLivenessMap = new HashMap<Integer,Boolean>();
	HashMap<Integer,Long> clientRecentRequestTimeMap = new HashMap<Integer,Long>();
	
	GameDetailsImpl gameDetailsObj;
	Random randomObj1 = new Random();
	Random randomObj2 = new Random();
	Lock lock = new ReentrantLock(true);//put under activate()
	transient Thread timerThread = null;//put under activate()
	volatile private boolean isGameStarted = false;
	volatile boolean isMoveReady = false;
	volatile boolean isGameOver = false;
	
	private int pIdCounter = 0;
	private int N;
	private int M;
	
	//non-game states
	GameIntf anotherServerStub;
	volatile ServerType serverType;
	int myPeerID;
	volatile boolean isAnotherServerUp=true;
	volatile int isGameStateUpdated = 0;
	volatile boolean isBReady = false;
	GameImpl savedGameState=null;
	int NO_EXCEEDED = 10000000;
	

	
	public List<PlayerIntf> getClientList() {
		return clientList;
	}

	public HashMap<Integer, PeerPlayerIntf> getClientObjMap() {
		return clientObjMap;
	}

	public HashMap<Integer, Boolean> getClientLivenessMap() {
		return clientLivenessMap;
	}

	public HashMap<Integer, Long> getClientRecentRequestTimeMap() {
		return clientRecentRequestTimeMap;
	}

	public GameDetailsImpl getGameDetailsObj() {
		return gameDetailsObj;
	}

	public Random getRandomObj1() {
		return randomObj1;
	}

	public Random getRandomObj2() {
		return randomObj2;
	}

	public Lock getLock() {
		return lock;
	}

	public Thread getTimerThread() {
		return timerThread;
	}

	public boolean isGameStarted() {
		return isGameStarted;
	}

	public boolean isMoveReady() {
		return isMoveReady;
	}

	public boolean isGameOver() {
		return isGameOver;
	}

	public int getpIdCounter() {
		return pIdCounter;
	}

	public int getN() {
		return N;
	}

	public int getM() {
		return M;
	}

	public ServerType getServerType() {
		return serverType;
	}
	
	public int getMyPeerID() {
		return myPeerID;
	}

	public void setMyPeerID(int myPeerID) {
		this.myPeerID = myPeerID;
	}

	public GameIntf getAnotherServerStub() {
		return anotherServerStub;
	}

	public void setAnotherServerStub(GameIntf anotherServerStub) {
		this.anotherServerStub = anotherServerStub;
	}

	public GameImpl(int n,int m)
	{
		this.N = n;
		this.M = m;
	}
	
	public void informBStubToPrimary(GameIntf stub)
	{
		anotherServerStub = stub;
		//System.out.println("Inside informBStubToPrimary - "+stub.toString());
	}
	
	public void startServerPingThread()
	{
		//System.out.println("startServerPingThread()");
		Thread t = new Thread()
		{
			@Override
			public void run()
			{
				while(true)
				{try {
					//sleep(5);
					System.out.print("");
				} catch(Exception e2){// (InterruptedException e2) {
				}
					try 
					{
							
						if(anotherServerStub != null)
						{
						//System.out.println("h");
							anotherServerStub.ping();
						}
					} 
					catch (RemoteException e) 
					{
						//check which server is crashed and handle server crash
						//broadcast that the server has crashed
						
						isGameStateUpdated = -1;
						isAnotherServerUp = false;
						
						if(serverType==ServerType.BACKUP)
						{
							transferObjectStates(savedGameState);
						}
						
						if(serverType==ServerType.PRIMARY) lock.lock();
						try
						{
							for(int pid : clientObjMap.keySet())
							{
								try {
									clientObjMap.get(pid).broadcastDiedStub(anotherServerStub);
								} catch (RemoteException e1) {
									//e1.printStackTrace();
								}
							}
						}
						finally
						{
							if(serverType==ServerType.PRIMARY) lock.unlock();
						}
						
					if(serverType==ServerType.BACKUP)
					{
						//transferObjectStates(savedGameState);
						if(isGameOver==false)
						{
							timerThread = new Thread(new TimerThread(GameImpl.this),"Timer Thread");
							timerThread.start();
						}
						serverType = ServerType.PRIMARY;
						System.out.println("I am converted as the Primary Server");
					}
												
					//change the states previously such that backup now becomes primary
						if(serverType==ServerType.PRIMARY)
						{
							System.out.println("The other server has crashed. Regenerating....");
							int nextPeerID = 0;
							lock.lock();
							try
							{								
								for(int pid : clientObjMap.keySet())
								{
									PeerPlayerIntf client = clientObjMap.get(pid);
									try 
									{
										if(pid!=myPeerID)
										{
											client.ping();
											nextPeerID = pid;
											break;
										}
									} 
									catch (RemoteException e1) {}
								}
								if(nextPeerID==0)
								{
									System.out.println("Only one player is alive. Quitting....");
									System.exit(0);
									
								}
							}
							finally
							{
								lock.unlock();
							}
							
							lock.lock();	
							try 
							{
								System.out.println("Server selected is: "+nextPeerID);
								GameImpl gameState;								
								gameState = deepCopy(GameImpl.this);
								gameState.anotherServerStub = GameImpl.this;
								
/*New Backup server stub-->*/	anotherServerStub = clientObjMap.get(nextPeerID).informPeerB(gameState);
								//isAnotherServerUp = true;
								
								//broadcast to all clients about new B. give lock in client
								for(int pid : clientObjMap.keySet())
								{
									try {
										clientObjMap.get(pid).broadcastNewB(anotherServerStub);
									} catch (RemoteException e1) {
									}
								}
								//isAnotherServerUp = true;
								//start req threads after server creation
							} catch (Exception e1) {}
							finally
							{
								lock.unlock();
							}
							
							lock.lock();
							try
							{
								isAnotherServerUp = true;
							}
							finally
							{
								lock.unlock();
							}					
						}
					}
				}
			}
		};
		t.start();	
	}
	
	public void ping()
	{
		
	}
	
	public ContainerIntf joinGame(PeerPlayerIntf clientObj)
	{
		//synchronized (this)
		Container contObj = new Container();
		contObj.setMessage("");
		lock.lock();
		try
		{
			if (isGameStarted)
			{
				contObj.setMessage(Messages.GAME_STARTED.toString());
				return (ContainerIntf) contObj;
				//return Messages.GAME_STARTED;
			}
			else if(clientList.size()>=N*N)
			{
				contObj.setMessage(Messages.PLAYERS_EXCEEDED.toString());
				return (ContainerIntf) contObj;
				//return Messages.PLAYERS_EXCEEDED;
			}
			
			clientObjMap.put(new Integer(createPlayer()), clientObj);
			if(clientObjMap.size()==1)
			{
				Thread t = new Thread(new WaitingThread(this),"Waiting Thread");
				t.start();
			}
			if(clientObjMap.size()==2)
			{
				contObj.setMessage(Messages.BACKUP.toString());
				contObj.setGameObj(deepCopy(this));
			}
			
			
			contObj.setMessage(contObj.getMessage()+";"+Messages.GAME_ACCEPTED.toString());
			return (ContainerIntf) contObj;
			//return Messages.GAME_ACCEPTED;
		}
		finally
		{
			lock.unlock();
		}
	}
	
	private GameImpl deepCopy(GameImpl obj)
	{
		GameImpl copyObj = null;
        try
		{
            ByteArrayOutputStream baosObj = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(baosObj);
            out.writeObject(obj);
            out.flush();
            out.close();
            
            ObjectInputStream oisObj = new ObjectInputStream(new ByteArrayInputStream(baosObj.toByteArray()));
            copyObj = (GameImpl)oisObj.readObject();
            copyObj.anotherServerStub = null;
            copyObj.serverType = null;
            copyObj.myPeerID = -1;
            copyObj.isAnotherServerUp = true;
            copyObj.isGameStateUpdated = 0;
            copyObj.isBReady = false;
            copyObj.savedGameState = null;
            
            return copyObj;
        }
		catch(Exception e) 
		{
			//System.out.println("Exception - DeepCopy");
			return null;
		}
	}
	
	public PlayerIntf getPlayer(int pid)
	{
		for(PlayerIntf p : gameDetailsObj.getPlayerList())
		{
			if(p.getPId()==pid)
				return p;  
		}
		return null;
	}
	
	public void transferObjectStates(GameImpl gameState)
	{
		if(gameState != null)
		{
			this.clientList = gameState.getClientList();
			this.clientObjMap = gameState.getClientObjMap();
			this.clientLivenessMap = gameState.getClientLivenessMap();
			this.clientRecentRequestTimeMap = gameState.getClientRecentRequestTimeMap();
			this.gameDetailsObj = gameState.getGameDetailsObj();
			this.randomObj1 = gameState.getRandomObj1();
			this.randomObj2 = gameState.getRandomObj2();
			//this.lock = gameState.getLock();
			this.timerThread = gameState.getTimerThread();
			this.isGameStarted = gameState.isGameStarted();
			this.isMoveReady = gameState.isMoveReady();
			this.isGameOver = gameState.isGameOver();
			this.pIdCounter = gameState.getpIdCounter();
			this.N = gameState.getN();
			this.M = gameState.getM();
		}
	}
	
	public void updateReadinesstoP()
	{
		isBReady = true;
	}
	
	public void UpdateGameStateToB(GameImpl gameState, boolean first_time)
	{
		savedGameState = gameState;
		if(!first_time)
		 isGameStateUpdated = 1;
		//System.out.println("Server? at UpdateGameStateToB: - Thread ID: "+Thread.currentThread().getId());
	}
	
	public ContainerIntf move(int pid, int direction)
	{
		
		//System.out.println("Inside Move");
		ContainerIntf contObj = new Container();
		if(serverType == ServerType.PRIMARY)
		{
			lock.lock();
			try
			{
				contObj = move2(pid,direction);
				if(isAnotherServerUp)
				{
					while(isAnotherServerUp && isBReady==false);
					isBReady = false;					
					anotherServerStub.UpdateGameStateToB(deepCopy(this),false);
				}
			} catch (RemoteException e) {
				
				//e.printStackTrace();
			}
			finally
			{
				lock.unlock();
			}
			
			return contObj;
		}
		
		else //if Backup Server
		{
			lock.lock();
			//int num_of_tries = 0;
			try
			{
				if(isAnotherServerUp)
				{
					try {

						anotherServerStub.updateReadinesstoP();
					} catch (RemoteException e1) {
						//e1.printStackTrace();
					}
				}
				while(isGameStateUpdated==0)
				{try {
					//Thread.sleep(100);
					//num_of_tries = num_of_tries;
					//System.out.print(""+num_of_tries);
				} catch (Exception e){//(InterruptedException e) {
					//e.printStackTrace();
				}}
				
				if(isGameStateUpdated==-1)  //Primary Server crash
				{
					isGameStateUpdated = 0;
					//System.out.println("****B: Primary Crash");
					//isAnotherServerUp = false;
					while(serverType != ServerType.PRIMARY);
					contObj = move2(pid,direction);
					//lock.unlock();
					return contObj;
				}
				else if(isGameStateUpdated==1)
				{
					isGameStateUpdated = 0;
					//System.out.println("****B: Leaving backup's part with null");
					//lock.unlock();
					return null;
				}
				
			}
			finally
			{
				lock.unlock();
			}
		}
		
		return null;
	}

	public ContainerIntf move2(int pid, int direction)	
	{
		//System.out.println("Inside Move2");
		Container contObj = new Container();
		
	//System.out.println("No: of Treasures now is: "+gameDetailsObj.getNumOfAvailableTreasures());	
		if(isMoveReady==false)
		{
			
			contObj.setMessage(Messages.MOVE_NOT_ALLOWED.toString());
			return contObj;
			//return null;
		}
		
		clientRecentRequestTimeMap.put(pid, System.currentTimeMillis());
		//System.out.println("Came here");
		int locx,locy;
		int numOfTreasuresAtXY=0;
		Player player = (Player)getPlayer(pid);
		
		if(player==null) 
		{
			return null;
		}
		
		locx = player.getLocationX();
		locy = player.getLocationY();
		
		 
		switch(direction)
		{
			case KeyEvent.VK_LEFT:
				//System.out.println("Left");
				locx = locx - 1;
				if(locx<0 || locx >=N)
				{
					contObj.setMessage(Messages.INVALID_MOVE.toString());
					return contObj;
					//return null;
				}
				break;
			case KeyEvent.VK_RIGHT:
				//System.out.println("Right");
				locx = locx + 1;
				if(locx<0 || locx >=N)
				{
					contObj.setMessage(Messages.INVALID_MOVE.toString());
					return contObj;
					//return null;
				}
				break;
			case KeyEvent.VK_UP:
				//System.out.println("Up");
				locy = locy - 1;
				if(locy<0 || locy >=N)
				{
					contObj.setMessage(Messages.INVALID_MOVE.toString());
					return contObj;
					//return null;
				}
				break;
			case KeyEvent.VK_DOWN:
				//System.out.println("Down");
				locy = locy + 1;
				if(locy<0 || locy >=N)
				{
					contObj.setMessage(Messages.INVALID_MOVE.toString());
					return contObj;
					//return null;
				}
				break;
			case KeyEvent.VK_N:
				System.out.println("NoMove");
				contObj.setMessage(Messages.NO_MOVE.toString());
				contObj.setGameDetailsObj(gameDetailsObj);
				return contObj;
				//return gameDetailsObj;
			case KeyEvent.VK_Q:
				//lock.lock();
				try
				{
					System.out.println("Quit request from player: "+pid);
					clientLivenessMap.put(pid, false);
					clientRecentRequestTimeMap.remove(pid);
					clientObjMap.remove(pid);
				}
				finally
				{
				//	lock.unlock();
				}
				
				return null;
			default:
				System.out.println("Invalid Move");
				contObj.setMessage(Messages.INVALID_MOVE.toString());
				return contObj;
				//return null;
		}	
		//lock.lock();
		try
		{
			if(isMoveReady==false)
			{
				contObj.setMessage(Messages.MOVE_NOT_ALLOWED.toString());
				return contObj;
				//return null;
			}
			
			for(PlayerIntf p : gameDetailsObj.getPlayerList())
			{
				if(p.getLocationX() == locx && p.getLocationY() == locy)
				{
					contObj.setMessage(Messages.CELL_OCCUPIED.toString());
					contObj.setGameDetailsObj(gameDetailsObj);
					return contObj;
					//return gameDetailsObj;
				}
			}
			
			player.setLocationX(locx);
			player.setLocationY(locy);
			numOfTreasuresAtXY = gameDetailsObj.getTreasuresMap().get(locx+" "+locy);
			gameDetailsObj.setNumOfAvailableTreasures(gameDetailsObj.getNumOfAvailableTreasures() - numOfTreasuresAtXY);
			
			if(gameDetailsObj.getNumOfAvailableTreasures()==0)
			{
				isMoveReady = false;
				isGameOver = true;
				timerThread.interrupt();
			}	
		}
		catch(Exception e){}
		finally
		{
		//	lock.unlock();
		}
		player.setNumOfTreasures(player.getNumOfTreasures()+ numOfTreasuresAtXY);
		gameDetailsObj.getTreasuresMap().put(locx+" "+locy, 0);
		
		contObj.setMessage(Messages.VALID_MOVE.toString());
		contObj.setGameDetailsObj(gameDetailsObj);
		return contObj;
		//return gameDetailsObj;
	}
	
	public void waitingOver()
	{
		//synchronized (this)
		lock.lock();
		try
		{
			isGameStarted = true;
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public void initializeGameDetails()
	{
		lock.lock();
		try
		{	
			gameDetailsObj = new GameDetailsImpl(N,M,clientList);
			clientList = null; //this is not used anymore
					
			System.out.println("Starting player notifications for game start");
			//for(int pid : clientObjMap.keySet())
			Iterator<Map.Entry<Integer,PeerPlayerIntf>> iter = clientObjMap.entrySet().iterator();
			while(iter.hasNext())
			{
				Map.Entry<Integer,PeerPlayerIntf> entry = iter.next();
				int pid = entry.getKey();
			
				try 
				{
					PeerPlayerIntf clientObj= entry.getValue();
					clientObj.startGameNotify(pid,gameDetailsObj,anotherServerStub);
					System.out.println("Notified: "+pid);
				}
				catch(RemoteException rexcep)
				{
					System.out.println("Player " + pid + " is not alive anymore");
					//lock.lock();
					try
					{
						clientLivenessMap.put(pid, false);
						clientRecentRequestTimeMap.remove(pid);
						iter.remove();
					}
					finally
					{
						//lock.unlock();
					}
				}
				catch (Exception e) {}	
			}
			System.out.println("Notified all live players for game start");
			
			isMoveReady = true;
			
			try {
				anotherServerStub.UpdateGameStateToB(deepCopy(this),true);
			} catch (RemoteException e) {
				//e.printStackTrace();
			}
			
			timerThread = new Thread(new TimerThread(this),"Timer Thread");
			timerThread.start();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	private int createPlayer() 
	{
		int locX,locY;
		boolean stop;
		pIdCounter++;
		
		do
		{
			stop = true;
			locX = randomObj1.nextInt(N);
			locY = randomObj2.nextInt(N);
			for(PlayerIntf playerObj : clientList)
			{
				if( locX == playerObj.getLocationX() && locY == playerObj.getLocationY() )
				{
					stop = false;
					break;
				}
			}
			
		}while(stop==false);
		
		Player playerObj = new Player(pIdCounter,0,locX,locY);
		clientList.add(playerObj);
		clientLivenessMap.put(pIdCounter, true);
		clientRecentRequestTimeMap.put(pIdCounter, System.currentTimeMillis());
		return pIdCounter;
		
	}
	/*
	//this function is always called only after acquiring lock
	public void removeClient()
	{
		/..put for loop
		clientObjMap.remove(pid);
		clientRecentRequestTimeMap.remove(pid);
	}
	*/
	private void notifyGameOver()
	{
//isMoveReady = false.
	}
}
