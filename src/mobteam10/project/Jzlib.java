package mobteam10.project;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZInputStream;
import com.jcraft.jzlib.ZOutputStream;

// Referenced Libraries�� ���Ե� �ܺ� ���̺귯���� �̿��Ͽ� ���� �۾��� �ô� Ŭ�����̴�.
public class Jzlib {
	
	// �����͸� �����ϴ� �޼ҵ�. ��Ʈ���� �̿��Ѵ�.
	public static byte[] compress(byte[] data)
	{
		System.out.println("compress1 : "+data.length);
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ZOutputStream zOut = new ZOutputStream(out, JZlib.Z_BEST_COMPRESSION);
			ObjectOutputStream objOut = new ObjectOutputStream(zOut);
			objOut.writeObject(data);
			objOut.close(); // JavaDoc�� ���ϸ� ��Ʈ���� ���� �ٱ��� �͸� ������ ���� ���� �ڵ����� �����ٰ� �Ǿ� �ִ�.
			// �׷��� �׷��� ������ �Ʒ� ������ �ϴ� ByteArrayOutputStream�� �޼ҵ� toByteArray�� �� �Ǵ����� �𸣰�����
			// JavaDoc���� �׷��ٰ� �ϴ�.... �ϴ� ������ �Ǵ� �ڵ�� ����!
			
			System.out.println(out.toByteArray().length);
			return out.toByteArray();
		}
		catch(Exception e)
		{
			System.out.println("ERR - Jzlib.compress");
			return null;
		}
	}
	
	// �������� ������ Ǫ�� �޼ҵ�. ��Ʈ���� �̿��Ѵ�.
	public static byte[] decompress(byte[] data)
	{
		try {
			ByteArrayInputStream in = new ByteArrayInputStream(data);
			ZInputStream zIn = new ZInputStream(in);
			ObjectInputStream objIn = new ObjectInputStream(zIn);
			byte[] ret = (byte[]) objIn.readObject();
			objIn.close();
			return ret;
		}
		catch(Exception e)
		{
			System.out.println("ERR - Jzlib.decompress");
			return null;
		}
	}
}
