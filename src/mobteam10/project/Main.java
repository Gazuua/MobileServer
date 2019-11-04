package mobteam10.project;

public class Main {		
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		// Selector로 구동되는 서버를 메인 메소드에서 틀어주는 것으로 마무리가 되었으나
		// 추후 클래스 구조를 재설계하여 훌륭한 모듈로 탄생시킬 예정....입니다.
		SelectServer server = new SelectServer();
		Thread thread = new Thread(server);
		thread.start();
		
		// 아래 코드는 암,복호화가 잘 되는가와 시간을 측정하기 위한 코드.. E3-1231v3 CPU와 8G RAM, Windows 10 환경에서 14ms정도 측정됨.
		/*
		byte[] data = "HELLO".getBytes();
		byte[] base = "HELL".getBytes();
		byte[] b2 = "HEL".getBytes();
		DynamicCrypterPool dp = DynamicCrypterPool.getInstance();
		dp.addKeyByValue(base);
		
		long start = System.currentTimeMillis();
		byte[] enc1 = dp.encrypt(data, base);
		byte[] dec1 = dp.decrypt(enc1, base);
		long time = System.currentTimeMillis() - start;
		
		System.out.println(new String(dec1));
		System.out.println(time);*/
	}
}