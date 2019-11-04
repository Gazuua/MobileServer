package mobteam10.project;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZInputStream;
import com.jcraft.jzlib.ZOutputStream;

// Referenced Libraries에 포함된 외부 라이브러리를 이용하여 압축 작업을 맡는 클래스이다.
public class Jzlib {
	
	// 데이터를 압축하는 메소드. 스트림을 이용한다.
	public static byte[] compress(byte[] data)
	{
		System.out.println("compress1 : "+data.length);
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ZOutputStream zOut = new ZOutputStream(out, JZlib.Z_BEST_COMPRESSION);
			ObjectOutputStream objOut = new ObjectOutputStream(zOut);
			objOut.writeObject(data);
			objOut.close(); // JavaDoc에 의하면 스트림은 가장 바깥의 것만 닫으면 안의 것은 자동으로 닫힌다고 되어 있다.
			// 그러나 그렇게 따지면 아래 닫혀야 하는 ByteArrayOutputStream의 메소드 toByteArray가 왜 되는지는 모르겠지만
			// JavaDoc에서 그렇다고 하니.... 일단 동작이 되는 코드로 넣음!
			
			System.out.println(out.toByteArray().length);
			return out.toByteArray();
		}
		catch(Exception e)
		{
			System.out.println("ERR - Jzlib.compress");
			return null;
		}
	}
	
	// 데이터의 압축을 푸는 메소드. 스트림을 이용한다.
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
