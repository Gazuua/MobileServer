package mobteam10.project;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by JHG on 2017-10-05.
 */

// ����� �� �� ���̴� PacketMessage Ŭ����
public class PacketMessage implements Serializable {

    private static final long serialVersionUID = 8501L;

    private short msgCode; // �� �޼����� ���ϴ� ���ΰ��� ������ �ڵ尪
    private String chatMsg; // ä�� �޼����� ��� ��ȭ����
    private String answer; // ������ �ʿ��� ��� ä������ ��Ʈ��
    private UserData userData; // ���� �� ��Ŷ�� ���´����� ���� ����
    private ArrayList<DrawData> drawDatas; // �׸� ��Ŷ�� ��� �׸� ������
    private ArrayList<UserData> userList; // JOIN ��Ŷ�� ��� ������ �������� �ִ��� ����� ������

    private transient byte[] serializedBytes; // transient Ű����� ����ȭ���� ���ܵ�, ����ȭ�� �������� ����Ʈ ��Ʈ��.

    public PacketMessage() { }

    // ä�� ��Ŷ�� ����� �޼ҵ�.. ���� make �޼ҵ�� ���� ����.
    public void makeChatPacket(String msg) {
        setMsgCode(MessageCode.CHAT);
        setChatMsg(msg);
        serialize();
    }

    public void makeJoinPacket(UserData data) {
        setMsgCode(MessageCode.JOIN);
        setUserData(data);
        serialize();
    }
    
    public void makeDrawDataPacket(ArrayList<DrawData> drawData)
    {
        setMsgCode(MessageCode.DRAW);
        setDrawDatas(drawData);
        serialize();
    }

    public void makeExitPacket(UserData data, String msg) {
        setMsgCode(MessageCode.EXIT);
        setChatMsg(msg);
        setUserData(data);
        serialize();
    }

    public void makeExitStopPacket(UserData data, String msg) {
        setMsgCode(MessageCode.EXITSTOP);
        setChatMsg(msg);
        setUserData(data);
        serialize();
    }

    public void makeListPacket(ArrayList<UserData> list, UserData data, String msg) {
        setMsgCode(MessageCode.JOIN);
        setChatMsg(msg);
        setUserData(data);
        setUserList(list);
        serialize();
    }

    public void makeStartPacket(String answer) {
        setMsgCode(MessageCode.START);
        setChatMsg("������ ���۵Ǿ����ϴ�!");
        setAnswer(answer);
        serialize();
    }

    public void makeDrawerPacket(String answer) {
        setMsgCode(MessageCode.DRAWER);
        setChatMsg("�̹� ����� �׸��� �׸� �����Դϴ�!");
        setAnswer(answer);
        serialize();
    }

    public void makeTimeoutPacket(String answer) {
        setMsgCode(MessageCode.TIMEOUT);
        setAnswer(answer);
        setChatMsg("�ð��� �ʰ��Ǿ����ϴ�!\n������ " + answer + " �Դϴ�.");
        serialize();
    }

    public void makeFinishPacket(String answer) {
        setMsgCode(MessageCode.FINISH);
        setAnswer(answer);
        setChatMsg("������ " + answer + " �Դϴ�.");
        serialize();
    }

    public void makeCorrectPacket(UserData data, String msg) {
        setMsgCode(MessageCode.CORRECT);
        setUserData(data);
        setChatMsg(msg);
        serialize();
    }

    
    // ����ȭ�� �ϴ� �޼ҵ�. Jzlib�� ���� ���൵ ���⼭ �ѹ��� �����.
    public void serialize() {
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;

        try {
            baos = new ByteArrayOutputStream();     // Byte �迭ȭ�Ͽ� Stream
            oos = new ObjectOutputStream(baos);     // Object�� ����ϴ� Stream
            oos.writeObject(this);                  // ���� ��ü�� Byte Stream���� ���
            serializedBytes = Jzlib.compress(baos.toByteArray());
            // byte �迭�� ����ȭ�� ��ü ������ �����Ͽ� ����

            oos.close();

        } catch (Exception e) {
        	System.out.println("ERR - PacketMessage.serialize");
        }
    }

    // ������ȭ �޼ҵ�. ���� Ǯ�⵵ ���ÿ� �����.
    public static PacketMessage deserialize(byte[] bytes)
    {
        ByteArrayInputStream bais = null;
        ObjectInputStream ois = null;

        try {
        	bais = new ByteArrayInputStream(Jzlib.decompress(bytes));
            ois = new ObjectInputStream(bais);
            
            PacketMessage ret = (PacketMessage) ois.readObject();
            
            ois.close();
            
            return ret;
        } catch (Exception e) {
        	System.out.println("ERR - PacketMessage.deserialize");
            return null;
        }
    }

    // Getters
    public short getMsgCode() {
        return msgCode;
    }
    public String getChatMsg() {
        return chatMsg;
    }
    public UserData getUserData() {
        return userData;
    }
    public ArrayList<UserData> getUserList() {
        return userList;
    }
    public ArrayList<DrawData> getDrawDatas() {
        return drawDatas;
    }
    public String getAnswer() { return answer; }
    public byte[] getSerializedBytes() {
        return serializedBytes;
    }


    // Setters
    public void setMsgCode(short msgCode) {
        this.msgCode = msgCode;
    }
    public void setChatMsg(String str) {
        this.chatMsg = str;
    }
    public void setUserData(UserData data) {
        this.userData = data;
    }
    public void setUserList(ArrayList<UserData> userList) {
        this.userList = userList;
    }
    public void setDrawDatas(ArrayList<DrawData> drawDatas) {
        this.drawDatas = drawDatas;
    }
    public void setAnswer(String answer) { this.answer = answer; }
    
}