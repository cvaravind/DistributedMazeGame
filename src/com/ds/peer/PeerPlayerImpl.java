package com.ds.peer;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import com.ds.peer.Commons.Messages;
import com.ds.peer.PeerPlayerIntf;
import com.ds.peer.GameDetailsIntf;
import com.ds.peer.GameIntf;
import com.ds.peer.PlayerIntf;
import com.ds.peer.Commons.ServerType;

public class PeerPlayerImpl implements PeerPlayerIntf 
{
	JFrame frame;
	JButton hiddenButton;
	JPanel basePanel,gamePanel;
	JTextArea infoText = new JTextArea();
	GameDetailsIntf gameDetailsObj;
	GameIntf stub1;
	GameIntf stub2;
	Thread t1,t2;
	Boolean isStub1Died,isStub2Died;
	boolean gameOver;
	
	MyKeyListener myKeyListenerObj;
	
	ReentrantLock lock = new ReentrantLock(true);

	int myPid;
	int N=0;
	int M=0;
	
	//int numOfAvailableTreasures;
	private boolean isGameGUIInitialized=false;

	public PeerPlayerImpl(GameIntf stub) 
	{
		this.stub1 = stub;
		isGameGUIInitialized=false;
	}

	public void startGameNotify(int pId,GameDetailsIntf gameDetailsObj, GameIntf backupServerStub) throws RemoteException 
	{
		System.out.println("Notifying Client - Your PID is: "+pId);
		myPid = pId;
		this.stub2 = backupServerStub; 
		this.gameDetailsObj = gameDetailsObj;
		isStub1Died = false;
		isStub2Died = false;
		gameOver = false;
		
		setGameGUIInitialized(false);
		(new SwingWorker<String,String>()
		{
			@Override
		    protected String doInBackground() {
				hiddenButton.doClick();
				return "Initialized GUI";
		       }

			@Override
			protected void done() {
				//System.out.println("Came to done");
				setGameGUIInitialized(true);
				
				
				
				/*
				
				Action action = new AbstractAction() {
					
					public void actionPerformed(ActionEvent e) {
						System.out.println("Action Performed");
						System.out.println(((KeyEvent)e).getKeyCode());
						//infoText.setText(e.);
						
					}
				};
				//basePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('a'), "a");
				String[] allowedMoves  = {"UP","DOWN","LEFT","RIGHT"};
				infoText.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(allowedMoves[0]), "action");
				basePanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(allowedMoves[0]), "action");
				//basePanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke('a'), "a");
				basePanel.getActionMap().put("action", action);
				
				*/
			}
			
		}).execute();
		//System.out.println("came here startgame");
	}
	
	public GameIntf informPeerB(GameImpl gameObj)
	{
		System.out.println("I am selected as Back up server");
		PeerServer newserv = new PeerServer(gameObj,myPid,ServerType.BACKUP,false);
		GameIntf generatedStub = newserv.getStub();
		return generatedStub;
		
	}
	
	public void broadcastDiedStub(GameIntf st)
	{
		if(stub1.equals(st))
		{
			lock.lock();
			try
			{
				isStub1Died = true;
			}
			finally
			{
				lock.unlock();
			}
		}
		else if(stub2.equals(st))
		{
			lock.lock();
			try
			{
				isStub2Died = true;
			}
			finally
			{
				lock.unlock();
			}
		}
	}
	
	public void gameOverNotify(int winnerPId)
	{
		if(myPid==winnerPId)
		{
			infoText.setText("You are the winner!!");
			System.out.println("You are the winner!!");
		}
		else
		{
			infoText.setText("Winner is Player: "+ winnerPId);
			System.out.println("Winner is Player: "+ winnerPId);
		}
		frame.removeKeyListener(myKeyListenerObj);
		gameOver = true;
		
	}
	
	public void broadcastNewB(GameIntf st)
	{
		if(isStub1Died)
		{
			this.stub1 = st;
			isStub1Died = false;
		}
		else if(isStub2Died)
		{
			this.stub2 = st;
			isStub2Died = false;
		}
	}
	
	public void ping()
	{
		//System.out.println("I'am pinged for liveness");
	}
	
	public void prepareGUI()
	{
		frame = new JFrame();
		hiddenButton = new JButton("Hidden");
		basePanel = new JPanel();
		gamePanel = new JPanel();
		//panel.setLayout(new GridLayout(1,1));
		hiddenButton.setVisible(false);
		basePanel.setLayout(new GridLayout(1,2));
		basePanel.add(gamePanel);
		basePanel.add(hiddenButton);
		frame.add(basePanel);
		frame.setSize(350,350);
		frame.setVisible(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		hiddenButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Game started....");
			/*	
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			*/
				initializeGUI();
				frame.setVisible(true);
		//System.out.println("GUI Inituializd");
			}
		});
	}
	
	public void initializeGUI()
	{
		N = gameDetailsObj.getN();
		M = gameDetailsObj.getM();
		basePanel.remove(hiddenButton);
		infoText.setText("Test Text");
		infoText.setEditable(false);
		basePanel.add(infoText);
		gamePanel.setLayout(new GridLayout(N,N));
		
		 myKeyListenerObj = new MyKeyListener(PeerPlayerImpl.this);
		frame.addKeyListener(myKeyListenerObj);
		basePanel.addKeyListener(myKeyListenerObj);
		gamePanel.addKeyListener(myKeyListenerObj);
		infoText.addKeyListener(myKeyListenerObj);
		
		for(int i=0; i<N; i++)
			for(int j=0; j<N; j++)
			{
				JButton button = new JButton((i+","+j));
				button.addKeyListener(myKeyListenerObj);
				gamePanel.add(button);
			}
		fillMaze();
		gamePanel.repaint();
		basePanel.repaint();
		//panel.revalidate();
	}
	
	public void fillMaze()
	{
		Map<String,Integer> treasuresMap = gameDetailsObj.getTreasuresMap(); 
		for(String locationStr : treasuresMap.keySet())
		{
			int locx = Integer.parseInt(locationStr.split(" ")[0]);
			int locy = Integer.parseInt(locationStr.split(" ")[1]);
			((JButton)gamePanel.getComponent( locy*N+locx )).setText("<html>"
					+ "("+locx+","+locy+") <br/> ["+treasuresMap.get(locationStr) + " T]"
							+ "</html>");
				
			//System.out.println(locationStr+"-"+treasuresMap.get(locationStr));
		}
		//int numOfGatheredTreasures = 0;
		List<PlayerIntf> playerList = gameDetailsObj.getPlayerList();
		for(PlayerIntf player : playerList)
		{
			int pid = player.getPId();
			int locx = player.getLocationX();
			int locy = player.getLocationY();
			int pNumOfTreasures = player.getNumOfTreasures();
			
			//numOfGatheredTreasures = numOfGatheredTreasures + pNumOfTreasures;
			
			((JButton)gamePanel.getComponent( locy*N+locx )).setText("<html>"
					+ "("+locx+","+locy+") <br/> P "+ pid + " [" +pNumOfTreasures+ "T]"
							+ "</html>");
			
			if(myPid == pid)
			{
				((JButton)gamePanel.getComponent( locy*N+locx )).setBackground(Color.CYAN);
			}
		}
		//numOfAvailableTreasures = M - numOfGatheredTreasures;
		infoText.setText("Player "+ myPid 
			+ "\n" +"Total Number of Treasures: " + M
			+ "\n" +"Total Number of Available Treasures: " + gameDetailsObj.getNumOfAvailableTreasures());
		
		gamePanel.repaint();
		basePanel.repaint();
	}

	boolean isGameGUIInitialized() {
		return isGameGUIInitialized;
	}

	void setGameGUIInitialized(boolean isGameGUIInitialized) {
		this.isGameGUIInitialized = isGameGUIInitialized;
	}
	
}


class MyKeyListener implements KeyListener
{
	PeerPlayerImpl clientBoardObj;
	ContainerIntf response1=null;
	ContainerIntf response2=null;
	public MyKeyListener(PeerPlayerImpl callerobject)
	{
		clientBoardObj = callerobject;
	}
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	
	public void keyPressed(KeyEvent e) 
	{
		if(!clientBoardObj.isGameGUIInitialized())
			return;
		if(clientBoardObj.gameOver)
			return;
	
		response1=null;
		response2=null;
		
		System.out.print(KeyEvent.getKeyText(e.getKeyCode())+": ");
		//clientBoardObj.infoText.setText(KeyEvent.getKeyText(e.getKeyCode()));
		final int keyCode = e.getKeyCode();
		
		ContainerIntf resp=null;
		try 
		{
			//do
			//{
				if(clientBoardObj.isStub1Died==false)
				{
					clientBoardObj.t1 = new Thread() {
						@Override
						public void run()
						{
							try {
								response1=clientBoardObj.stub1.move(clientBoardObj.myPid, keyCode);
							} 
							catch (RemoteException e) 
							{
								clientBoardObj.lock.lock();
								try
								{
									//System.out.println("Request made by t1 has failed");
									clientBoardObj.isStub1Died = true;
									//e.printStackTrace();
								}
								finally
								{
									clientBoardObj.lock.unlock();
								}
								
							}
						}
					};
					clientBoardObj.t1.start();
					//System.out.println("Client: t1 starting");
				}
				
				if(clientBoardObj.isStub2Died==false)
				{
					clientBoardObj.t2 = new Thread() {
						@Override
						public void run()
						{
							try {
								response2=clientBoardObj.stub2.move(clientBoardObj.myPid, keyCode);
							} 
							catch (RemoteException e) 
							{
								clientBoardObj.lock.lock();
								try
								{
									//System.out.println("Request made by t2 has failed");
									clientBoardObj.isStub2Died = true;
									//e.printStackTrace();
								}
								finally
								{
									clientBoardObj.lock.unlock();
								}
							}
						}
					};
					clientBoardObj.t2.start();
				}
				
				try 
				{
					clientBoardObj.t1.join();
					//System.out.println("Client: t1 ****ending");
					clientBoardObj.t2.join();
					//System.out.println("Client: t2 ****ending");
				} catch (InterruptedException e1) {}
				/*
				try {
					clientBoardObj.t2.join();
				} catch (InterruptedException e1) {
					System.out.println("Inside Interrupted catch Exception of t2.join()");
					//e1.printStackTrace();
				}
				*/
			//System.out.println("Inside do while");	
			//}while(response1==null && response2==null);
			
			if(e.getKeyCode()==KeyEvent.VK_Q)
			{
				System.out.println("Client quitting...");
				System.exit(0);
			}
			
			resp = (response1 != null) ? response1 : response2;
			if(resp==null) return;
			clientBoardObj.gameDetailsObj = resp.getGameDetailsObj();
			
			System.out.println(resp.getMessage());
			
			if(resp.getMessage().contains(Messages.INVALID_MOVE.toString()) 
					|| resp.getMessage().contains(Messages.MOVE_NOT_ALLOWED.toString()))
			{

			}
			else
			{
				clientBoardObj.fillMaze();
			}
		} catch (Exception e1) {
			//System.out.println("Exception at keyPressed");
			//e1.printStackTrace();
		}
		
	}
}
