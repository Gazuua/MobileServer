package mobteam10.project;

public class Main {		
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		// Selector�� �����Ǵ� ������ ���� �޼ҵ忡�� Ʋ���ִ� ������ �������� �Ǿ�����
		// ���� Ŭ���� ������ �缳���Ͽ� �Ǹ��� ���� ź����ų ����....�Դϴ�.
		SelectServer server = new SelectServer();
		Thread thread = new Thread(server);
		thread.start();
		
		// �Ʒ� �ڵ�� ��,��ȣȭ�� �� �Ǵ°��� �ð��� �����ϱ� ���� �ڵ�.. E3-1231v3 CPU�� 8G RAM, Windows 10 ȯ�濡�� 14ms���� ������.
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