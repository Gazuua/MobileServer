package mobteam10.project;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


// Selector�� ���ư��� ���� ��ü�� ������ Ŭ�����̴�. Runnable �������̽��� �����Ͽ���.
public class SelectServer implements Runnable {
	
	private Selector selector;	// �ͺ��ŷ ���� ����� �����ϴ� �� �ʿ��� Selector
	private ServerSocketChannel serverChannel;	// Ŭ���̾�Ʈ���� ������ ������ ���� ���� ä��
	private InetSocketAddress serverAddress;	// ������ �ּҸ� ������ ��ü
	private Queue<PacketMessage> outPacketQueue; // ��Ŷ ���� ��⿭�� �̿��� ť
	private DynamicCrypterPool crypter;			// ��ȣȭ�� ���� keyPool�� �޼ҵ尡 �����Ǵ� �̱��� Ŭ����
	
	private Map<SocketChannel, Integer> room;	// �� �ȿ� �������� �ִ��� ������ Map
	private Map<Integer, String> namelist;		// �� �ȿ� �ִ� ���� �̸��� ������ Map
	private Queue<Integer> drawerQueue;			// �׸� �׸��� ������ ������ ť
	private boolean[] indexMark;				// �ε��� ���� ������ �����ϰ� �ִ����� ǥ���� boolean �迭
	private boolean isStarted;					// startTimer�� �۵� ������ Ȯ���� ����
	private boolean isGaming;					// ������ ���۵Ǿ� ���� ������ Ȯ���� ����
	private int nowDrawer;						// ���� ���忡�� �׸� �׸��� ������ ���� ����� �ε���. ��ÿ��� -1
	private String nowAnswer;					// ���� ������ ������ �����ϴ� String
	private Random random;						// ���� ������ ���� ��ü
	private TimerClass timerClass;				// Ÿ�̸� ��ü�� ���� ���ǵ� ���� ��ü
	
	private Thread thread; // �̰Ŵ� �ҽ��� �ʹ� �������� ¥�� �߰��� Ÿ�̸� �θ��°Ͷ�����..
	
	// ������
	public SelectServer()
	{
		serverAddress = new InetSocketAddress(55248);	// ���� ��Ʈ��ȣ�� 55248������ ����
		outPacketQueue = new LinkedList<>();			// LinkedList �Ҵ�
		crypter = DynamicCrypterPool.getInstance();		// ��ȣȭ ��ü �ν��Ͻ� �ޱ�
		room = new HashMap<SocketChannel, Integer>();	// SocketChannel�� key��, �������� Value�� ������ �ؽ��� �Ҵ�
		namelist = new HashMap<Integer, String>();		// �������� key��, ���ڿ��� Value�� ������ �ؽ��� �Ҵ�
		drawerQueue = new LinkedList<>();				// �׸� ���� ���ϴ� ť �Ҵ�
		indexMark = new boolean[4];
		for(int i=0; i<4; i++) indexMark[i] = false;	// indexMark �迭 �ʱ�ȭ
		isStarted = false;								// ���� ī��Ʈ �������� false �ʱ�ȭ
		isGaming = false;								// ���� �������� false �ʱ�ȭ
		nowDrawer = -1;									// �׸��� ��� ������ -1
		nowAnswer = new String();
		random = new Random();							// ���� ������ ������ �Ʒ����� ��� �ʱ�ȭ������ �ϴ� �ʱ�ȭ
		timerClass = new TimerClass();					// Ÿ�̸� �̳� Ŭ������ �ʱ�ȭ
	}
	
	// ���� ����� ���õ� Ÿ�̸Ӹ� ������ �̳� Ŭ����
	class TimerClass {

		Timer startTimer; 
		TimerTask startTimerTask;
		Timer gameTimer;
		TimerTask gameTimerTask;

		// 7�� �� ������ �����ϴ� Ÿ�̸� ����
		public void startTimerWork() {
			if( startTimer != null ) this.stopStartTimer();
			setNewStartTimer();
			setNewStartTimerTask();
			startTimer.schedule(timerClass.startTimerTask, 7000);
		}
		
		// 2�� ���� ������ �����ϴ� Ÿ�̸� ����
		public void gameTimerWork() {
			if( gameTimer != null ) this.stopStartTimer();
			setNewGameTimer();
			setNewGameTimerTask();
			gameTimer.schedule(gameTimerTask, 120000);
		}
		
		public void setNewStartTimer() {
			this.startTimer = new Timer();
		}

		public void setNewStartTimerTask() {
			this.startTimerTask = new TimerTask() {
				@Override
				public void run() { // �濡 4���� ���� ���� 7�ʰ� ������ �� Ÿ�̸Ӱ� �ߵ��Ǿ� ������ ���۵ȴ�.
					random = new Random();
					nowAnswer = AnswerString.answerStrings[random.nextInt(AnswerString.answerStrings.length)];
					nowDrawer = drawerQueue.poll();
					int temp = nowDrawer;
					drawerQueue.offer(temp);
					
					PacketMessage startMsg = new PacketMessage();
					startMsg.makeStartPacket(nowAnswer);
					sendPacket(room.keySet().iterator().next(), startMsg);
					
					isStarted = false;
					isGaming = true;
					
					PacketMessage drawerMsg = new PacketMessage();
					drawerMsg.makeDrawerPacket(nowAnswer);
					sendPacket(room.keySet().iterator().next(), drawerMsg);
					
					if (thread != null) 
					{
						thread.interrupt();
						thread = null;
					}
					gameTimerWork();
				}
			};
		}

		public void setNewGameTimer() {
			this.gameTimer = new Timer();
		}

		public void setNewGameTimerTask() {
			this.gameTimerTask = new TimerTask() {
				@Override
				public void run() { // ���� Ÿ�̸Ӵ� 2�� �ȿ� ���� �� ������ ������ �����ϸ�, �� �ܿ��� cancel�ȴ�.
					// Ÿ�Ӿƿ� �� Ÿ�Ӿƿ� ��Ŷ�� ���� ������ ���������� �˸���.
					PacketMessage timeoutMsg = new PacketMessage();
					timeoutMsg.makeTimeoutPacket(nowAnswer);
					sendPacket(room.keySet().iterator().next(), timeoutMsg);
					
					isStarted = false;
					isGaming = false;
					
					if ( room.size() == 2 ) // ������ ������ ������ 4���� ���������� �ٽ� ������ �����ϵ��� �����带 �����Ѵ�.
					{
						thread = new Thread(new Runnable() {
							@Override
							public void run() {
								try {
									Thread.sleep(3000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									return;
								}
								
								PacketMessage msg = new PacketMessage();
								msg.setMsgCode(MessageCode.DEFAULT);
								msg.setChatMsg("��� �� ������ ���۵˴ϴ�.");
								msg.serialize();
								sendPacket(room.keySet().iterator().next(), msg);
								isStarted = true;
								isGaming = false;
								timerClass.startTimerWork();
							}
						});
						thread.start();
					}
					else { // 4�� �����̸� �׳� �ƹ��͵� ���Ѵ�
						isStarted = false;
						isGaming = false;
					}
				}
			};
		}
		
		public void stopStartTimer() {
			startTimer.cancel();
			startTimer = null;
		}
		
		public void stopGameTimer() {
			gameTimer.cancel();
			gameTimer = null;
		}
	}
	
	
	// Runnable�� run �޼ҵ� �������̵�
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(true) 
		{
            try 
            {
            	selector = Selector.open(); // Selector�� open
            	
            	serverChannel = ServerSocketChannel.open();	// ���� ���� ä�� ����
            	serverChannel.configureBlocking(false);		// �ͺ��ŷ ���� ����
            	serverChannel.socket().bind(serverAddress);	// ���� ������ bind�Ѵ�
            	serverChannel.register(selector, SelectionKey.OP_ACCEPT); // selector�� ������ accept�� ���� �����ϵ��� ���
            	
            	serverChannel.socket().getInetAddress();
				Log("���� ���� ���� !! -> ���� IP : " + InetAddress.getLocalHost().getHostAddress().toString() 
            			+ " // "+ LocalDateTime.now()); // ���� ���� �ð� ���
            	
            }
            catch (Exception e) // ������ ����� �����ϱ⵵ ���� ���ư��ø� ���⼭ �����
            {
            	e.printStackTrace();
            	Log("���� ���α׷� ���ۿ� ������ �־� ���α׷��� �����մϴ� !! ");
            	return;
            }
            
            while(true) // ������ ���Ƿ� ������ �������� select ������ ���ѷ����� ������
        	{
        		try {
					selector.select(); // ���� Ǯ �ȿ��� ���°� ��ȭ�� Ű�� �����Ѵ�
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log("select ������ �߸��Ǿ����ϴ� !! ");
					return;
				}
        		
        		// Iterator�� ���� ���õ� Ű�鿡 ���� �۾��� �����Ѵ�
        		Iterator<?> keys = selector.selectedKeys().iterator();
        		
        		while(keys.hasNext()) // iterator�� ��� Ű�� ��ȸ�� ������ �ݺ����� �����Ѵ�
        		{
        			SelectionKey key = (SelectionKey) keys.next();
        			keys.remove();
        			if(!key.isValid()) 
        				continue;
        			
        			if (key.isAcceptable())
        				Accept(key);
        			else if (key.isReadable())
        				Read(key);
        			else if (key.isWritable())
        				Write(key);
        		}
        	}	
        } 
	}
	
	// ���� ä�ο��� Ŭ���̾�Ʈ ä���� connect�� accept �ϴ� �޼ҵ�
	private void Accept(SelectionKey key)
	{
		serverChannel = (ServerSocketChannel) key.channel();
		SocketChannel channel = null;
		
		try {
			channel = serverChannel.accept(); // connect�� accept�� Ŭ���̾�Ʈ ���� ä���� ��´�
			channel.configureBlocking(false); // �񵿱� ����� �ϵ��� �����Ѵ�
			channel.register(selector, SelectionKey.OP_READ); // ���� Ű ���¸� read�� �ξ� �б� �����ϵ��� �Ѵ�
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log("Ŭ���̾�Ʈ�� ������ ��û�Ͽ����� �����Ͽ����ϴ� !! " + LocalDateTime.now()); // �Ƹ� �Ͼ �� ������ �ϴ� �ص� ����ó��
			return;
		}
		
		String address = channel.socket().getInetAddress().getHostAddress();
		
		if (room.size() >= 4) // �濡 �̹� 4�� ������ �ο� �ʰ��̹Ƿ� �����Ѵ�.
		{
			Log("�ο� �ʰ��� Ŭ���̾�Ʈ�� ���� �ź� !! -> IP �ּ� : " + address + " / " + LocalDateTime.now());
			
			try {
				channel.close();
				return;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		}
		
		int i;
		for(i=0; i<4; i++) // ����ִ� �ε����� �����ϱ� ���� ��ȸ
			if ( !indexMark[i] ) break;
		
		room.put(channel, i); // �ε����� ������ ä�� �濡 ����
		crypter.addKeyByValue(channel.socket().getInetAddress().getHostAddress()); // ���� ��Ī Ű�� �����Ͽ� �����Ѵ�
		drawerQueue.offer(i); // �׸� �׸��� ������ �����ϴ� ť�� �ִ´�
		indexMark[i] = true; // �ε����� ������ �����ߴٰ� ǥ�����ش�
		
		Log("Ŭ���̾�Ʈ ���� !! -> IP �ּ� : " + address + " / " + LocalDateTime.now());
	}
	
	// readŰ�� �б⸦ ������ �� ���̴� �޼ҵ�
	private void Read(SelectionKey key)
	{
		SocketChannel channel = (SocketChannel)key.channel();
		ByteBuffer buf = null;
		String address = channel.socket().getInetAddress().getHostAddress().toString();
		
		Log("\n================== ���� ���� !! ==================");
		
		int recvbytes; // ���� ����Ʈ ��
		short packetSize; // ��Ŷ ������
		ByteBuffer bSize = ByteBuffer.allocateDirect(2); // ����� ���� bytebuffer
		byte[] size = new byte[2]; // ��Ŷ ����� ����� ���̱� ���� ����Ʈ �迭
		
		try {
			recvbytes = channel.read(bSize); // bSize�� ũ�⸸ŭ ���� ���ۿ��� �����͸� ������ �� ũ�⸦ recvbytes�� �޴´�
			
			if (recvbytes == -1) // ���� ũ�Ⱑ -1�̶�� ���� ������ �����ٴ� �ǹ��̹Ƿ� ó�����ش�.
			{
				try {
					clientDisconnected(address, channel);
					channel.close();
					return;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log("Ŭ���̾�Ʈ�� ���������� ������� ���߽��ϴ� !! " + address);
					return;
				}
			}
			
			bSize.flip(); // ByteBuffer�� ���� ���¿� ���� position���� limit���� �����Ѵ�. (ByteBuffer ��ü ����)
			bSize.get(size); // ����Ʈ �迭�� ByteBuffer�� ���� ���´�.
			packetSize = this.byteArrayToShort(size); // ����Ʈ �迭�� short�� �ٲ۴�
			buf = ByteBuffer.allocateDirect(packetSize); // �������� ���ŵ� ����Ʈ�� �� ����Ʈ���� �˾����Ƿ� �� ũ�⸸ŭ ���ο� bytebuffer�� �Ҵ��Ѵ�.
			// allocateDirect�� JVM�� �ƴ� �ý��� �޸𸮸� ���� �Ҵ�ޱ� ������ �����Ͱ� �̵��ϴ� �ӵ��� ��ô ������.
			recvbytes = 0;
			
			while(packetSize != recvbytes) // ��Ŷ�� ������ �� ������ ���� ���� �ƴϹǷ�, �ش� ũ�⸦ ���� ������ ���ѷ����� ������.
			{
				recvbytes += channel.read(buf);
			}
			
		} catch (Exception e) { // Ŭ���̾�Ʈ�� �̻��� ������ ���ư����� �� ���� �� �ִ� ����ó��
			// TODO Auto-generated catch block
			clientDisconnected(address, channel);
			e.printStackTrace();
			return;
		}
		
		buf.flip();
		byte[] bytes = new byte[buf.limit()]; // ����Ʈ ���ۿ� ���� ���� ������ ����Ʈ �迭 ����
		buf.get(bytes); // ����Ʈ �迭�� ������ ����
		bytes = crypter.decrypt(bytes, 
				channel.socket().getInetAddress().getHostAddress()); // Ŭ���̾�Ʈ�� ���� ��ĪŰ�� �̿��� ��ȣȭ�Ѵ�.
		
		PacketMessage msg = PacketMessage.deserialize(bytes); // �޼��� �м��� ���� ������ȭ
		if (msg == null) return; // null�� �� ������ ���α׷� ��� ����
		Log("���� !! -> �۽��� IP �ּ� : " + address + " / �޼��� ũ�� : " + recvbytes);
			
		// ť�� ���� �޼����� �߰��Ѵ�
		outPacketQueue.offer(msg);
		
		try {
			channel.register(selector, SelectionKey.OP_WRITE); // �����Ϳ� ���� ä���� ���� ���� ���·� ���
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}	
		
		if (buf != null)
		{
			buf.clear();
			buf = null;
		}
		
		bytes = null;
	}
	
	// write Ű�� ���� ä���� �̿��� ���� ���� ��ü�� ��ε�ĳ���� ���ִ� �޼ҵ�. �׷��� ���������� ��Ŷ�� ������ �޼ҵ�� �ƴϴ�.
	private void Write(SelectionKey key)
	{
		SocketChannel channel = (SocketChannel)key.channel();
		String address = channel.socket().getInetAddress().getHostAddress().toString();
		
		while(true)
		{
			PacketMessage msg = outPacketQueue.poll(); // ť���� ���� ���� ��Ŷ�� ���´�
			
			// ť�� ����־ null���� ���� ��� �޼��� ������ �����Ѵ�
			if (msg == null) break;
			
			Log("�۽� !! -> �۽��� IP �ּ� : " + address + " / �޼��� �ڵ� : " + msg.getMsgCode());
			Log("================== �۽� ���� !! ==================");
			
			// ����ȭ�ϱ� �� Ư�� �ڵ忡 ���� ������ ��ó�� ������ ��ģ��
			switch(msg.getMsgCode())
			{
			case MessageCode.JOIN: // JOIN ��Ŷ�� �޾��� ��� ���� ��ü���� ��ε�ĳ������ LIST ��Ŷ�� �����
				int index = room.get(channel);
				String name = msg.getUserData().getName();
				namelist.put(index, name); // �̸� ����Ʈ�� ���� �̸��� �����Ѵ�
				if (room.size() == 2 && !isStarted && thread == null) { // ���� ������ ������ �Ǹ� startTimer�� ������Ų��.
					msg.makeListPacket(makeListData(), new UserData(index, name), 
							"[" + msg.getUserData().getName() + "] ���� �����ϼ̽��ϴ�.\n"
							+"��� �� ������ ���۵˴ϴ�.");
					
					isStarted = true;
					isGaming = false;
					timerClass.startTimerWork();
					Log("4�� ����! 7�� �� ������ ���۵˴ϴ�.");
				}
				else msg.makeListPacket(makeListData(), new UserData(index, name), 
						"[" + msg.getUserData().getName() + "] ���� �����ϼ̽��ϴ�.");
				break;
			case MessageCode.CHAT: // ���� �߿� �������� ä���� ���� ��� �켱 �װ��� �������� Ȯ���Ͽ� �´� ������ �Ѵ�
				int cindex = room.get(channel);
				String cname = namelist.get(room.get(channel));
				if ( isGaming ) { // ���� ���ε�..
					if (nowAnswer.contains(msg.getChatMsg())) { // ������ �´´ٸ�?
						
						// ������ ���� ���� �׸� �׸��� ����� ��� ��Ŷ�� �׳� �����ϰ�, Key�� �ٽ� READ���·� ������.
						if(cindex == nowDrawer) {
							try {
								channel.register(selector, SelectionKey.OP_READ);
								return;
							} catch (ClosedChannelException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								return;
							}
						}
						
						// ������ �������� �ùٸ� ���� �Ͽ� ����ٸ� �� �Ʒ��� �ڵ���� �����Ѵ�.
						timerClass.stopGameTimer();
						isStarted = false;
						isGaming = false;
						
						PacketMessage correctMsg = new PacketMessage();
						correctMsg.makeCorrectPacket(new UserData(cindex, cname), 
								"["+cname+"] ���� ������ ���߾� ������ ȹ���ϼ̽��ϴ�!");
						sendPacket(channel, correctMsg);
						
						
						PacketMessage finishMsg = new PacketMessage();
						finishMsg.makeFinishPacket(nowAnswer);
						sendPacket(channel, finishMsg); // ������ �������� �˸��� finish��Ŷ�� ������.
						
						if ( room.size() == 2 ) // �����µ� 4���� �״�� �����ִٸ� 3�� �� ������ϵ��� ������ �����Ѵ�.
						{
							thread = new Thread(new Runnable() {
								@Override
								public void run() {
									try {
										Thread.sleep(3000);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										return;
									}
									
									PacketMessage msg = new PacketMessage();
									msg.setMsgCode(MessageCode.DEFAULT);
									msg.setChatMsg("��� �� ������ ���۵˴ϴ�.");
									msg.serialize();
									sendPacket(room.keySet().iterator().next(), msg);
									isStarted = true;
									isGaming = false;
									timerClass.startTimerWork();
								}
							});
							thread.start();
						}
					}
				}
				msg.setChatMsg(cname + " : " + msg.getChatMsg());
				msg.setUserData(new UserData(cindex, cname));
				msg.serialize();
				break;
			}
			
			sendPacket(channel, msg); // ��ó���� �Ϸ�� msg ��Ŷ�� �ڵ忡 ���� ���ǿ� �°� �����Ѵ�			
		}
		
		try {
			channel.register(selector, SelectionKey.OP_READ); // �� �ѷ����� �ٽ� �б� ���� ������.
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
	}
	
	// ���������� ��Ŷ�� ������ �޼ҵ�. ���� �������� �ܵ����� �� �޼ҵ带 ����� ���� �ִ�..(Ŭ���� ������ ����� �� ¥��..)
	public synchronized void sendPacket(SocketChannel channel, PacketMessage msg)
	{
		ByteBuffer buf = null;
		Iterator<?> iterator = room.keySet().iterator();
		
		// null�� �ƴϸ� ������ ���� ���̹Ƿ� ����ȭ�Ͽ� ����Ʈ ���ۿ� ��Ƶд�
		if( msg.getSerializedBytes() == null ) msg.serialize();
		
		// �� �ȿ� �ִ� ����鿡�� Write ���ش�
		while(iterator.hasNext())
		{
			SocketChannel sendChannel = (SocketChannel)iterator.next();
			String sendAddress = sendChannel.socket().getInetAddress().getHostAddress();
			
			// ������ ���� ��ȣȭ�� �ϰ� �տ� ũ�� ������ ���� 2����Ʈ ����� ���δ�.
			byte[] encrypted = crypter.encrypt
					(msg.getSerializedBytes(), sendChannel.socket().getInetAddress().getHostAddress());
			byte[] size = shortToByteArray((short)encrypted.length);
			byte[] alldata = this.combineByteArrays(size, encrypted);
			buf = ByteBuffer.allocateDirect(alldata.length);
			buf.put(alldata);
			buf.flip();
			
			switch(msg.getMsgCode())
			{
			// �׸��� �׸� ������ DrawData�� �޾ƾ� �� �ʿ䰡 �����Ƿ�, ����Ȯ�� �� ������ �� ������ ��ŵ�Ѵ�
			case MessageCode.DRAW:
				if (sendChannel.equals(channel)) continue;
				Log("�۽� �� !! -> ��� ȣ��Ʈ IP �ּ� : " + sendAddress + " / �޼��� ���� : DRAW");
				break;
			// Drawer�� ���θ� �޼����� �޾ƾ� �ϹǷ� ������ �ƴ� ��� ��ŵ
			case MessageCode.DRAWER:
				if (room.get(sendChannel) != nowDrawer) continue; 
				Log("�۽� �� !! -> ��� ȣ��Ʈ IP �ּ� : " + sendAddress + " / �޼��� ���� : DRAWER");
				break;
			// �� �� ��ε�ĳ��Ʈ�ؾ� �� �޼��� �ڵ��� ��� �׿� �´� �α׸� ����Ѵ�		
			case MessageCode.CHAT:
				Log("�۽� �� !! -> ��� ȣ��Ʈ IP �ּ� : " + sendAddress + " / �޼��� ���� : " + msg.getChatMsg());
				break;
			case MessageCode.JOIN:
				Log("�۽� �� !! -> ��� ȣ��Ʈ IP �ּ� : " + sendAddress + " / �޼��� ���� : JOIN" );
				break;	
			case MessageCode.EXIT:
				Log("�۽� �� !! -> ��� ȣ��Ʈ IP �ּ� : " + sendAddress + " / �޼��� ���� : EXIT");
				break;
			case MessageCode.START:
				Log("�۽� �� !! -> ��� ȣ��Ʈ IP �ּ� : " + sendAddress + " / �޼��� ���� : START");
				break;
			case MessageCode.CORRECT:
				Log("�۽� �� !! -> ��� ȣ��Ʈ IP �ּ� : " + sendAddress + " / �޼��� ���� : CORRECT");
				break;
			// default(�����κ����� �˸� �޼���)
			default:
				Log("�۽� �� !! -> ��� ȣ��Ʈ IP �ּ� : " + sendAddress + " / �޼��� ���� : DEFAULT");
				break;
			}
			
			// �Ѹ���
			try {
				sendChannel.write(buf);
				buf.rewind(); // �̰��� �� �� ��� bytebuffer�� ������ �״�� �� ä position�� �ʱ�ȭ�Ͽ� �ٽ� ���� �� �ֵ��� �ϴ� ��
			} catch(Exception e)
			{
				e.printStackTrace();
			}
			
			buf.clear();
		}
		
		Log("================== �۽� �Ϸ� !! ==================");
		
		if (buf != null)
		{
			buf.clear();
			buf = null;
		}
	}
	
	// ���� ����Ʈ�� ������ִ� �޼ҵ�
	public ArrayList<UserData> makeListData()
	{
		ArrayList<UserData> userlist = new ArrayList<UserData>();

		Iterator<?> iterator = room.keySet().iterator();
		while(iterator.hasNext())
		{
			SocketChannel channel = (SocketChannel)iterator.next();
			UserData data = new UserData(room.get(channel), namelist.get(room.get(channel)));
			userlist.add(data);
		}
		
		return userlist;
	}
	
	// byte[2]->short�� ĳ�����ϴ� �޼ҵ�
	public short byteArrayToShort(byte[] arr)
    {
        short ret = 0;
        ret = (short) (((arr[0] & 0xFF) << 8) + (arr[1] & 0xFF));

        return ret;
    }

	// short->byte[2]�� ĳ�����ϴ� �޼ҵ�
	public byte[] shortToByteArray(short n)
    {
        byte[] ret = new byte[2];
        
        ret[0] = (byte)((n>>8) & 0xFF);
        ret[1] = (byte)((n>>0) & 0xFF);
        
        return ret;
    }

	// �� ���� ����Ʈ �迭�� A + B ������� ��ġ�� �޼ҵ�
	public byte[] combineByteArrays(byte[] a, byte[] b)
	{
		byte[] ret = new byte[a.length + b.length];
		
		int j = 0;
		
		for(byte k : a)
		{
			ret[j] = k;
			j++;
		}
		
		for(byte k : b)
		{
			ret[j] = k;
			j++;
		}
		
		return ret;
	}
	
	public static void Log(byte[] src)
	{
		System.out.println(new String(src));
	}
	
	public static void Log(String src)
	{
		System.out.println(src);
	}
	
	// Ŭ���̾�Ʈ���� ������ ������ �����ϴ� ���� ����� ���� �޼ҵ�
	private void clientDisconnected(String address, SocketChannel channel)
	{
		int index = room.get(channel);
		String name = namelist.get(index);
		
		PacketMessage msg = new PacketMessage(); // ������ ������ ������ �������� �˸��� ��Ŷ�� �����
		
		// ���� ������ ���缭, startTimer�� ����� �����尡 3�ʰ� ���� �ڰ� �ִٸ� �װ� �O�����ش�.
		if(thread != null)
		{
			thread.interrupt();
			thread = null;
		}
			
		
		if (isStarted) { 
			// ���� ī��Ʈ ���� ��� �ߴ��ϰ� �˸���
			timerClass.stopStartTimer();
			isStarted = false;
			nowDrawer = -1;
			msg.makeExitPacket(new UserData(index, name), 
					"[" + name + "] ���� ���ӿ��� �����̽��ϴ�.\n���� �� �� �� ��� ��ٸ��ø� ������ ���۵˴ϴ�.");
		}
		else if ( (isGaming && room.size() <= 2) || index == nowDrawer) { 
			// ���� ���̾��µ� ȥ�� ���ų�, �׸� �׸��� ����� ���� ��� ���� ���̴� ������ �ߴ��Ѵ�.
			timerClass.stopGameTimer();
			isGaming = false;
			nowDrawer = -1;
			msg.makeExitStopPacket(new UserData(index, name), 
					"[" + name + "] ���� ���ӿ��� �����̽��ϴ�.\n���� ���� ���̴� ������ �ߴܵ˴ϴ�.");
		}
		else 
			// �� ���� ��Ȳ�� �׳� �����ٰ� �˸��� �ش�
			msg.makeExitPacket(new UserData(index, name), "[" + name + "] ���� ���ӿ��� �����̽��ϴ�.");
		
		namelist.remove(index); 	// ��Ŷ�� ������ ��, ���� ���� �濡�� �ڸ��� ����
		drawerQueue.remove(index);	// �׸� �׸� ���������� ��������
		room.remove(channel);		// �̸� ����Ʈ������ ��������
		crypter.deleteKeyByValue(channel.socket().getInetAddress().getHostAddress());
		indexMark[index] = false;	// �ε��� ���̺� ���� �����Ͽ� �ڸ��� ����
		sendPacket(channel, msg);	// ���� �� ������ ��Ŷ�� ������
		
		Log("Ŭ���̾�Ʈ ���� ���� !! " + address + " / Index : " + index + " / " + LocalDateTime.now());
	}
	
}


