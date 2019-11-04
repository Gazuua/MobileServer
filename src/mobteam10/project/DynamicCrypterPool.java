package mobteam10.project;

import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

// ���� ��Ī Ű�� �̿��� ��ȣȭ�� ����ϴ� ������ Ŭ����. keyPool�̶�� Map ��ü�� �̿��� Ŭ���̾�Ʈ�� ���� Ű�� �����Ѵ�.
public class DynamicCrypterPool {

	private static final DynamicCrypterPool crypter = new DynamicCrypterPool(); // Singleton
	
	private Map<String, SecretKeySpec> keyPool;
	private byte[] keyBase = "Mobile10Project!".getBytes(); // Key = 16Bytes
	private Cipher cipher; // ��ȣȭ�� ���������� ����ϴ� �߿��� �ν��Ͻ�
	
	// �����ڿ��� �ʱ�ȭ�� �� �ִ� ���� �ʱ�ȭ
	private DynamicCrypterPool()
	{
		keyPool = new HashMap<>();
		try {
			cipher = Cipher.getInstance("AES"); // AES ��ȣȭ�� ����ϴ� �ν��Ͻ��� �ҷ��´�.
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// �̱��� ���� �ν��Ͻ� ��� �޼ҵ�.
	public static DynamicCrypterPool getInstance()
	{
		return crypter;
	}
	
	// Ű Ǯ�� ���𰡰� �ִٴ� ���� ����� Ŭ���̾�Ʈ�� �ִٴ� ���̹Ƿ� �װ��� �غ� ���·� ����. boolean ������ �װ��� ��ȯ�ϴ� �޼ҵ�.
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
	
	// ���� Ű�� ŰǮ�� �߰��ϴ� �޼ҵ�. ���⼭ value�� Ŭ���̾�Ʈ�� ���� ip �ּ��̴�.
	// �����⸦ ����ϴ� ���� ��Ʈ��ũ ���� ���� Ŭ���̾�Ʈ�� ���� ���� ������ �װ��� ���� ����ó���� ���� �ʾ�...
	public void addKeyByValue(String value)
	{		
		byte[] key = keyBase.clone();
		byte[] val = value.getBytes();
		
		for(int i=0; i<key.length; i++)
			for (int j=0; j<val.length; j++)
				key[i] ^= val[j];
		
		keyPool.put(value, new SecretKeySpec(key, "AES"));
	}
	
	// ŰǮ���� �ش��ϴ� �����͸� �����.
	public void deleteKeyByValue(String value)
	{
		keyPool.remove(value);
	}
	
	// ��ȣȭ �޼ҵ�
	public byte[] encrypt(byte[] data, String value)
	{
		try {		
			cipher.init(Cipher.ENCRYPT_MODE, keyPool.get(value)); // cipher�� ��ȣȭ ����, Ű�� �޾� ��ȣȭ�� �غ��Ѵ�.
			return cipher.doFinal(data); // ��ȣȭ�� ��ģ �����͸� ��´�.
		} 
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("ERR - (POOL)encrypt()");
			return null;
		}
	}
	
	// ��ȣȭ �޼ҵ�
	public byte[] decrypt(byte[] data, String value)
	{
		try {
			cipher.init(Cipher.DECRYPT_MODE, keyPool.get(value)); // cipher�� ��ȣȭ ����, Ű�� �޾� ��ȣȭ�� �غ��Ѵ�.
			return cipher.doFinal(data); // ��ȣȭ�� ��ģ �����͸� ��´�.
		} 
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("ERR - (POOL)decrypt()");
			return null;
		}
	}
}
