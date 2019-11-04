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


// Selector로 돌아가는 서버 전체를 구현한 클래스이다. Runnable 인터페이스를 구현하였음.
public class SelectServer implements Runnable {
	
	private Selector selector;	// 넌블로킹 소켓 통신을 구현하는 데 필요한 Selector
	private ServerSocketChannel serverChannel;	// 클라이언트들의 접속을 승인할 서버 소켓 채널
	private InetSocketAddress serverAddress;	// 서버의 주소를 가지는 객체
	private Queue<PacketMessage> outPacketQueue; // 패킷 전송 대기열로 이용할 큐
	private DynamicCrypterPool crypter;			// 암호화를 위한 keyPool과 메소드가 지원되는 싱글톤 클래스
	
	private Map<SocketChannel, Integer> room;	// 방 안에 누구누구 있는지 저장할 Map
	private Map<Integer, String> namelist;		// 방 안에 있는 유저 이름을 저장할 Map
	private Queue<Integer> drawerQueue;			// 그림 그리는 순서를 지정할 큐
	private boolean[] indexMark;				// 인덱스 값을 누군가 점유하고 있는지를 표시할 boolean 배열
	private boolean isStarted;					// startTimer가 작동 중인지 확인할 변수
	private boolean isGaming;					// 게임이 시작되어 진행 중인지 확인할 변수
	private int nowDrawer;						// 현재 라운드에서 그림 그리는 권한을 가진 사람의 인덱스. 평시에는 -1
	private String nowAnswer;					// 현재 라운드의 정답을 저장하는 String
	private Random random;						// 랜덤 변수를 만들 객체
	private TimerClass timerClass;				// 타이머 객체가 전부 정의된 모음 객체
	
	private Thread thread; // 이거는 소스를 너무 병맛같이 짜서 중간에 타이머 부르는것때문에..
	
	// 생성자
	public SelectServer()
	{
		serverAddress = new InetSocketAddress(55248);	// 서버 포트번호를 55248번으로 설정
		outPacketQueue = new LinkedList<>();			// LinkedList 할당
		crypter = DynamicCrypterPool.getInstance();		// 암호화 객체 인스턴스 받기
		room = new HashMap<SocketChannel, Integer>();	// SocketChannel을 key로, 정수값을 Value로 가지는 해쉬맵 할당
		namelist = new HashMap<Integer, String>();		// 정수값을 key로, 문자열을 Value로 가지는 해쉬맵 할당
		drawerQueue = new LinkedList<>();				// 그림 순서 정하는 큐 할당
		indexMark = new boolean[4];
		for(int i=0; i<4; i++) indexMark[i] = false;	// indexMark 배열 초기화
		isStarted = false;								// 시작 카운트 안했으니 false 초기화
		isGaming = false;								// 시작 안했으니 false 초기화
		nowDrawer = -1;									// 그리는 사람 없을땐 -1
		nowAnswer = new String();
		random = new Random();							// 랜덤 변수는 어차피 아래에서 계속 초기화하지만 일단 초기화
		timerClass = new TimerClass();					// 타이머 이너 클래스도 초기화
	}
	
	// 게임 진행과 관련된 타이머를 정의한 이너 클래스
	class TimerClass {

		Timer startTimer; 
		TimerTask startTimerTask;
		Timer gameTimer;
		TimerTask gameTimerTask;

		// 7초 뒤 게임을 시작하는 타이머 가동
		public void startTimerWork() {
			if( startTimer != null ) this.stopStartTimer();
			setNewStartTimer();
			setNewStartTimerTask();
			startTimer.schedule(timerClass.startTimerTask, 7000);
		}
		
		// 2분 동안 게임을 진행하는 타이머 가동
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
				public void run() { // 방에 4명이 가득 차고서 7초가 지나면 이 타이머가 발동되어 게임이 시작된다.
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
				public void run() { // 게임 타이머는 2분 안에 답을 못 맞췄을 때에만 동작하며, 그 외에는 cancel된다.
					// 타임아웃 시 타임아웃 패킷을 보내 게임이 실패했음을 알린다.
					PacketMessage timeoutMsg = new PacketMessage();
					timeoutMsg.makeTimeoutPacket(nowAnswer);
					sendPacket(room.keySet().iterator().next(), timeoutMsg);
					
					isStarted = false;
					isGaming = false;
					
					if ( room.size() == 2 ) // 게임이 끝나고 나서도 4명이 남아있으면 다시 게임을 시작하도록 스레드를 설정한다.
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
								msg.setChatMsg("잠시 후 게임이 시작됩니다.");
								msg.serialize();
								sendPacket(room.keySet().iterator().next(), msg);
								isStarted = true;
								isGaming = false;
								timerClass.startTimerWork();
							}
						});
						thread.start();
					}
					else { // 4명 이하이면 그냥 아무것도 안한다
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
	
	
	// Runnable의 run 메소드 오버라이드
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(true) 
		{
            try 
            {
            	selector = Selector.open(); // Selector를 open
            	
            	serverChannel = ServerSocketChannel.open();	// 서버 소켓 채널 열기
            	serverChannel.configureBlocking(false);		// 넌블로킹 모드로 설정
            	serverChannel.socket().bind(serverAddress);	// 서버 정보를 bind한다
            	serverChannel.register(selector, SelectionKey.OP_ACCEPT); // selector에 서버가 accept를 수행 가능하도록 등록
            	
            	serverChannel.socket().getInetAddress();
				Log("서버 가동 시작 !! -> 서버 IP : " + InetAddress.getLocalHost().getHostAddress().toString() 
            			+ " // "+ LocalDateTime.now()); // 서버 가동 시간 기록
            	
            }
            catch (Exception e) // 서버가 제대로 동작하기도 전에 돌아가시면 여기서 종료됨
            {
            	e.printStackTrace();
            	Log("서버 프로그램 동작에 문제가 있어 프로그램을 종료합니다 !! ");
            	return;
            }
            
            while(true) // 서버를 임의로 종료할 때까지는 select 동작을 무한루프로 돌린다
        	{
        		try {
					selector.select(); // 소켓 풀 안에서 상태가 변화한 키를 선택한다
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log("select 동작이 잘못되었습니다 !! ");
					return;
				}
        		
        		// Iterator를 통해 선택된 키들에 대한 작업을 수행한다
        		Iterator<?> keys = selector.selectedKeys().iterator();
        		
        		while(keys.hasNext()) // iterator로 모든 키를 순회할 때까지 반복문을 수행한다
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
	
	// 서버 채널에서 클라이언트 채널의 connect를 accept 하는 메소드
	private void Accept(SelectionKey key)
	{
		serverChannel = (ServerSocketChannel) key.channel();
		SocketChannel channel = null;
		
		try {
			channel = serverChannel.accept(); // connect를 accept한 클라이언트 소켓 채널을 얻는다
			channel.configureBlocking(false); // 비동기 통신을 하도록 설정한다
			channel.register(selector, SelectionKey.OP_READ); // 현재 키 상태를 read로 두어 읽기 가능하도록 한다
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log("클라이언트가 접속을 요청하였으나 실패하였습니다 !! " + LocalDateTime.now()); // 아마 일어날 일 없지만 일단 해둔 예외처리
			return;
		}
		
		String address = channel.socket().getInetAddress().getHostAddress();
		
		if (room.size() >= 4) // 방에 이미 4명 있으면 인원 초과이므로 차단한다.
		{
			Log("인원 초과로 클라이언트의 접속 거부 !! -> IP 주소 : " + address + " / " + LocalDateTime.now());
			
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
		for(i=0; i<4; i++) // 비어있는 인덱스를 점유하기 위해 순회
			if ( !indexMark[i] ) break;
		
		room.put(channel, i); // 인덱스를 점유한 채로 방에 들어간다
		crypter.addKeyByValue(channel.socket().getInetAddress().getHostAddress()); // 동적 대칭 키를 생성하여 보유한다
		drawerQueue.offer(i); // 그림 그리는 순서를 지정하는 큐에 넣는다
		indexMark[i] = true; // 인덱스를 실제로 점유했다고 표시해준다
		
		Log("클라이언트 접속 !! -> IP 주소 : " + address + " / " + LocalDateTime.now());
	}
	
	// read키로 읽기를 진행할 때 쓰이는 메소드
	private void Read(SelectionKey key)
	{
		SocketChannel channel = (SocketChannel)key.channel();
		ByteBuffer buf = null;
		String address = channel.socket().getInetAddress().getHostAddress().toString();
		
		Log("\n================== 수신 감지 !! ==================");
		
		int recvbytes; // 받은 바이트 수
		short packetSize; // 패킷 사이즈
		ByteBuffer bSize = ByteBuffer.allocateDirect(2); // 통신을 위한 bytebuffer
		byte[] size = new byte[2]; // 패킷 사이즈를 헤더로 붙이기 위한 바이트 배열
		
		try {
			recvbytes = channel.read(bSize); // bSize의 크기만큼 소켓 버퍼에서 데이터를 읽으며 이 크기를 recvbytes에 받는다
			
			if (recvbytes == -1) // 받은 크기가 -1이라는 것은 접속을 끊었다는 의미이므로 처리해준다.
			{
				try {
					clientDisconnected(address, channel);
					channel.close();
					return;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log("클라이언트가 정상적으로 종료되지 못했습니다 !! " + address);
					return;
				}
			}
			
			bSize.flip(); // ByteBuffer의 현재 상태에 따라 position값과 limit값을 수정한다. (ByteBuffer 객체 참조)
			bSize.get(size); // 바이트 배열에 ByteBuffer의 값을 얻어온다.
			packetSize = this.byteArrayToShort(size); // 바이트 배열을 short로 바꾼다
			buf = ByteBuffer.allocateDirect(packetSize); // 이제부터 수신될 바이트가 몇 바이트인지 알았으므로 그 크기만큼 새로운 bytebuffer를 할당한다.
			// allocateDirect는 JVM이 아닌 시스템 메모리를 직접 할당받기 때문에 데이터가 이동하는 속도가 무척 빠르다.
			recvbytes = 0;
			
			while(packetSize != recvbytes) // 패킷이 무조건 한 번에만 오는 것이 아니므로, 해당 크기를 받을 때까지 무한루프를 돌린다.
			{
				recvbytes += channel.read(buf);
			}
			
		} catch (Exception e) { // 클라이언트가 미상의 이유로 돌아가셨을 때 해줄 수 있는 예외처리
			// TODO Auto-generated catch block
			clientDisconnected(address, channel);
			e.printStackTrace();
			return;
		}
		
		buf.flip();
		byte[] bytes = new byte[buf.limit()]; // 바이트 버퍼에 받은 것을 저장할 바이트 배열 선언
		buf.get(bytes); // 바이트 배열에 데이터 저장
		bytes = crypter.decrypt(bytes, 
				channel.socket().getInetAddress().getHostAddress()); // 클라이언트별 동적 대칭키를 이용해 복호화한다.
		
		PacketMessage msg = PacketMessage.deserialize(bytes); // 메세지 분석을 위한 역직렬화
		if (msg == null) return; // null일 시 오류로 프로그램 사망 방지
		Log("수신 !! -> 송신자 IP 주소 : " + address + " / 메세지 크기 : " + recvbytes);
			
		// 큐에 보낼 메세지를 추가한다
		outPacketQueue.offer(msg);
		
		try {
			channel.register(selector, SelectionKey.OP_WRITE); // 셀렉터에 현재 채널을 쓰기 가능 상태로 등록
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
	
	// write 키를 가진 채널을 이용해 서버 내부 전체에 브로드캐스팅 해주는 메소드. 그러나 실질적으로 패킷을 보내는 메소드는 아니다.
	private void Write(SelectionKey key)
	{
		SocketChannel channel = (SocketChannel)key.channel();
		String address = channel.socket().getInetAddress().getHostAddress().toString();
		
		while(true)
		{
			PacketMessage msg = outPacketQueue.poll(); // 큐에서 지금 보낼 패킷을 빼온다
			
			// 큐가 비어있어서 null값이 왔을 경우 메세지 전송을 종료한다
			if (msg == null) break;
			
			Log("송신 !! -> 송신자 IP 주소 : " + address + " / 메세지 코드 : " + msg.getMsgCode());
			Log("================== 송신 시작 !! ==================");
			
			// 직렬화하기 전 특정 코드에 대한 데이터 전처리 과정을 거친다
			switch(msg.getMsgCode())
			{
			case MessageCode.JOIN: // JOIN 패킷을 받았을 경우 유저 전체에게 브로드캐스팅할 LIST 패킷을 만든다
				int index = room.get(channel);
				String name = msg.getUserData().getName();
				namelist.put(index, name); // 이름 리스트에 유저 이름을 저장한다
				if (room.size() == 2 && !isStarted && thread == null) { // 게임 시작의 조건이 되면 startTimer를 가동시킨다.
					msg.makeListPacket(makeListData(), new UserData(index, name), 
							"[" + msg.getUserData().getName() + "] 님이 입장하셨습니다.\n"
							+"잠시 후 게임이 시작됩니다.");
					
					isStarted = true;
					isGaming = false;
					timerClass.startTimerWork();
					Log("4명 입장! 7초 뒤 게임이 시작됩니다.");
				}
				else msg.makeListPacket(makeListData(), new UserData(index, name), 
						"[" + msg.getUserData().getName() + "] 님이 입장하셨습니다.");
				break;
			case MessageCode.CHAT: // 게임 중에 누군가가 채팅을 쳤을 경우 우선 그것이 정답인지 확인하여 맞는 동작을 한다
				int cindex = room.get(channel);
				String cname = namelist.get(room.get(channel));
				if ( isGaming ) { // 게임 중인데..
					if (nowAnswer.contains(msg.getChatMsg())) { // 정답이 맞는다면?
						
						// 정답을 맞춘 놈이 그림 그리는 사람일 경우 패킷을 그냥 무시하고, Key를 다시 READ상태로 돌린다.
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
						
						// 정답을 누군가가 올바른 조건 하에 맞춘다면 이 아래의 코드들을 실행한다.
						timerClass.stopGameTimer();
						isStarted = false;
						isGaming = false;
						
						PacketMessage correctMsg = new PacketMessage();
						correctMsg.makeCorrectPacket(new UserData(cindex, cname), 
								"["+cname+"] 님이 정답을 맞추어 점수를 획득하셨습니다!");
						sendPacket(channel, correctMsg);
						
						
						PacketMessage finishMsg = new PacketMessage();
						finishMsg.makeFinishPacket(nowAnswer);
						sendPacket(channel, finishMsg); // 게임이 끝났음을 알리는 finish패킷을 보낸다.
						
						if ( room.size() == 2 ) // 끝났는데 4명이 그대로 남아있다면 3초 뒤 재시작하도록 스레드 가동한다.
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
									msg.setChatMsg("잠시 후 게임이 시작됩니다.");
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
			
			sendPacket(channel, msg); // 전처리가 완료된 msg 패킷을 코드에 따른 조건에 맞게 전송한다			
		}
		
		try {
			channel.register(selector, SelectionKey.OP_READ); // 다 뿌렸으면 다시 읽기 모드로 돌린다.
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
	}
	
	// 실질적으로 패킷을 보내는 메소드. 가끔 서버에서 단독으로 이 메소드를 사용할 때도 있다..(클래스 구조를 제대로 못 짜서..)
	public synchronized void sendPacket(SocketChannel channel, PacketMessage msg)
	{
		ByteBuffer buf = null;
		Iterator<?> iterator = room.keySet().iterator();
		
		// null이 아니면 무조건 보낼 것이므로 직렬화하여 바이트 버퍼에 담아둔다
		if( msg.getSerializedBytes() == null ) msg.serialize();
		
		// 방 안에 있는 사람들에게 Write 해준다
		while(iterator.hasNext())
		{
			SocketChannel sendChannel = (SocketChannel)iterator.next();
			String sendAddress = sendChannel.socket().getInetAddress().getHostAddress();
			
			// 보내기 전에 암호화를 하고 앞에 크기 정보를 담은 2바이트 헤더를 붙인다.
			byte[] encrypted = crypter.encrypt
					(msg.getSerializedBytes(), sendChannel.socket().getInetAddress().getHostAddress());
			byte[] size = shortToByteArray((short)encrypted.length);
			byte[] alldata = this.combineByteArrays(size, encrypted);
			buf = ByteBuffer.allocateDirect(alldata.length);
			buf.put(alldata);
			buf.flip();
			
			switch(msg.getMsgCode())
			{
			// 그림을 그린 본인은 DrawData를 받아야 할 필요가 없으므로, 본인확인 후 본인일 시 루프를 스킵한다
			case MessageCode.DRAW:
				if (sendChannel.equals(channel)) continue;
				Log("송신 중 !! -> 대상 호스트 IP 주소 : " + sendAddress + " / 메세지 내용 : DRAW");
				break;
			// Drawer는 본인만 메세지를 받아야 하므로 본인이 아닐 경우 스킵
			case MessageCode.DRAWER:
				if (room.get(sendChannel) != nowDrawer) continue; 
				Log("송신 중 !! -> 대상 호스트 IP 주소 : " + sendAddress + " / 메세지 내용 : DRAWER");
				break;
			// 그 외 브로드캐스트해야 할 메세지 코드의 경우 그에 맞는 로그를 기록한다		
			case MessageCode.CHAT:
				Log("송신 중 !! -> 대상 호스트 IP 주소 : " + sendAddress + " / 메세지 내용 : " + msg.getChatMsg());
				break;
			case MessageCode.JOIN:
				Log("송신 중 !! -> 대상 호스트 IP 주소 : " + sendAddress + " / 메세지 내용 : JOIN" );
				break;	
			case MessageCode.EXIT:
				Log("송신 중 !! -> 대상 호스트 IP 주소 : " + sendAddress + " / 메세지 내용 : EXIT");
				break;
			case MessageCode.START:
				Log("송신 중 !! -> 대상 호스트 IP 주소 : " + sendAddress + " / 메세지 내용 : START");
				break;
			case MessageCode.CORRECT:
				Log("송신 중 !! -> 대상 호스트 IP 주소 : " + sendAddress + " / 메세지 내용 : CORRECT");
				break;
			// default(서버로부터의 알림 메세지)
			default:
				Log("송신 중 !! -> 대상 호스트 IP 주소 : " + sendAddress + " / 메세지 내용 : DEFAULT");
				break;
			}
			
			// 뿌린다
			try {
				sendChannel.write(buf);
				buf.rewind(); // 이것은 한 번 썼던 bytebuffer의 정보를 그대로 둔 채 position만 초기화하여 다시 읽을 수 있도록 하는 것
			} catch(Exception e)
			{
				e.printStackTrace();
			}
			
			buf.clear();
		}
		
		Log("================== 송신 완료 !! ==================");
		
		if (buf != null)
		{
			buf.clear();
			buf = null;
		}
	}
	
	// 유저 리스트를 만들어주는 메소드
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
	
	// byte[2]->short로 캐스팅하는 메소드
	public short byteArrayToShort(byte[] arr)
    {
        short ret = 0;
        ret = (short) (((arr[0] & 0xFF) << 8) + (arr[1] & 0xFF));

        return ret;
    }

	// short->byte[2]로 캐스팅하는 메소드
	public byte[] shortToByteArray(short n)
    {
        byte[] ret = new byte[2];
        
        ret[0] = (byte)((n>>8) & 0xFF);
        ret[1] = (byte)((n>>0) & 0xFF);
        
        return ret;
    }

	// 두 개의 바이트 배열을 A + B 순서대로 합치는 메소드
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
	
	// 클라이언트에서 접속을 끊으면 실행하는 연결 종료용 통합 메소드
	private void clientDisconnected(String address, SocketChannel channel)
	{
		int index = room.get(channel);
		String name = namelist.get(index);
		
		PacketMessage msg = new PacketMessage(); // 누군가 나갔기 때문에 나갔음을 알리는 패킷을 만든다
		
		// 누가 정답을 맞춰서, startTimer를 깨우는 스레드가 3초간 잠을 자고 있다면 그걸 즁지해준다.
		if(thread != null)
		{
			thread.interrupt();
			thread = null;
		}
			
		
		if (isStarted) { 
			// 시작 카운트 중일 경우 중단하고 알린다
			timerClass.stopStartTimer();
			isStarted = false;
			nowDrawer = -1;
			msg.makeExitPacket(new UserData(index, name), 
					"[" + name + "] 님이 게임에서 나가셨습니다.\n방이 꽉 찬 뒤 잠시 기다리시면 게임이 시작됩니다.");
		}
		else if ( (isGaming && room.size() <= 2) || index == nowDrawer) { 
			// 게임 중이었는데 혼자 남거나, 그림 그리는 사람이 나간 경우 진행 중이던 게임을 중단한다.
			timerClass.stopGameTimer();
			isGaming = false;
			nowDrawer = -1;
			msg.makeExitStopPacket(new UserData(index, name), 
					"[" + name + "] 님이 게임에서 나가셨습니다.\n현재 진행 중이던 게임이 중단됩니다.");
		}
		else 
			// 그 외의 상황엔 그냥 나갔다고 알림만 준다
			msg.makeExitPacket(new UserData(index, name), "[" + name + "] 님이 게임에서 나가셨습니다.");
		
		namelist.remove(index); 	// 패킷을 보내기 전, 나간 놈은 방에서 자리를 뺀다
		drawerQueue.remove(index);	// 그림 그릴 순서에서도 뺴버린다
		room.remove(channel);		// 이름 리스트에서도 마찬가지
		crypter.deleteKeyByValue(channel.socket().getInetAddress().getHostAddress());
		indexMark[index] = false;	// 인덱스 테이블 값도 변경하여 자리를 비운다
		sendPacket(channel, msg);	// 방을 다 뺐으면 패킷을 보낸다
		
		Log("클라이언트 연결 종료 !! " + address + " / Index : " + index + " / " + LocalDateTime.now());
	}
	
}


