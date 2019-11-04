package mobteam10.project;

import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

// 동적 대칭 키를 이용한 암호화를 담당하는 서버용 클래스. keyPool이라는 Map 객체를 이용해 클라이언트별 동적 키를 관리한다.
public class DynamicCrypterPool {

	private static final DynamicCrypterPool crypter = new DynamicCrypterPool(); // Singleton
	
	private Map<String, SecretKeySpec> keyPool;
	private byte[] keyBase = "Mobile10Project!".getBytes(); // Key = 16Bytes
	private Cipher cipher; // 암호화를 실질적으로 담당하는 중요한 인스턴스
	
	// 생성자에서 초기화할 수 있는 것을 초기화
	private DynamicCrypterPool()
	{
		keyPool = new HashMap<>();
		try {
			cipher = Cipher.getInstance("AES"); // AES 암호화를 담당하는 인스턴스를 불러온다.
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// 싱글톤 전용 인스턴스 얻는 메소드.
	public static DynamicCrypterPool getInstance()
	{
		return crypter;
	}
	
	// 키 풀에 무언가가 있다는 뜻은 통신할 클라이언트가 있다는 뜻이므로 그것을 준비 상태로 본다. boolean 값으로 그것을 반환하는 메소드.
	public boolean isReady() { 
		if ( !isEmpty() ) return true;
		else return false;
	}
	
	public boolean isEmpty() {
		return keyPool.isEmpty();
	}
	
	public int size() {
		return keyPool.size();
	}
	
	// 동적 키를 키풀에 추가하는 메소드. 여기서 value는 클라이언트의 공인 ip 주소이다.
	// 공유기를 사용하는 같은 네트워크 내의 여러 클라이언트가 오면 뻑이 나지만 그것을 막을 예외처리는 하지 않았...
	public void addKeyByValue(String value)
	{		
		byte[] key = keyBase.clone();
		byte[] val = value.getBytes();
		
		for(int i=0; i<key.length; i++)
			for (int j=0; j<val.length; j++)
				key[i] ^= val[j];
		
		keyPool.put(value, new SecretKeySpec(key, "AES"));
	}
	
	// 키풀에서 해당하는 데이터를 지운다.
	public void deleteKeyByValue(String value)
	{
		keyPool.remove(value);
	}
	
	// 암호화 메소드
	public byte[] encrypt(byte[] data, String value)
	{
		try {		
			cipher.init(Cipher.ENCRYPT_MODE, keyPool.get(value)); // cipher를 암호화 모드로, 키를 받아 암호화를 준비한다.
			return cipher.doFinal(data); // 암호화를 마친 데이터를 뱉는다.
		} 
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("ERR - (POOL)encrypt()");
			return null;
		}
	}
	
	// 복호화 메소드
	public byte[] decrypt(byte[] data, String value)
	{
		try {
			cipher.init(Cipher.DECRYPT_MODE, keyPool.get(value)); // cipher를 복호화 모드로, 키를 받아 복호화를 준비한다.
			return cipher.doFinal(data); // 복호화를 마친 데이터를 뱉는다.
		} 
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("ERR - (POOL)decrypt()");
			return null;
		}
	}
}
