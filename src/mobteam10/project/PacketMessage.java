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

// 통신을 할 때 쓰이는 PacketMessage 클래스
public class PacketMessage implements Serializable {

    private static final long serialVersionUID = 8501L;

    private short msgCode; // 이 메세지가 뭐하는 놈인가를 정의한 코드값
    private String chatMsg; // 채팅 메세지일 경우 대화내용
    private String answer; // 정답이 필요할 경우 채워넣을 스트링
    private UserData userData; // 누가 이 패킷을 보냈는지에 대한 정보
    private ArrayList<DrawData> drawDatas; // 그림 패킷일 경우 그림 데이터
    private ArrayList<UserData> userList; // JOIN 패킷일 경우 유저가 누구누구 있는지 담아줄 데이터

    private transient byte[] serializedBytes; // transient 키워드로 직렬화에서 제외된, 직렬화가 마무리된 바이트 스트림.

    public PacketMessage() { }

    // 채팅 패킷을 만드는 메소드.. 이하 make 메소드는 전부 동일.
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
        setChatMsg("게임이 시작되었습니다!");
        setAnswer(answer);
        serialize();
    }

    public void makeDrawerPacket(String answer) {
        setMsgCode(MessageCode.DRAWER);
        setChatMsg("이번 라운드는 그림을 그릴 차례입니다!");
        setAnswer(answer);
        serialize();
    }

    public void makeTimeoutPacket(String answer) {
        setMsgCode(MessageCode.TIMEOUT);
        setAnswer(answer);
        setChatMsg("시간이 초과되었습니다!\n정답은 " + answer + " 입니다.");
        serialize();
    }

    public void makeFinishPacket(String answer) {
        setMsgCode(MessageCode.FINISH);
        setAnswer(answer);
        setChatMsg("정답은 " + answer + " 입니다.");
        serialize();
    }

    public void makeCorrectPacket(UserData data, String msg) {
        setMsgCode(MessageCode.CORRECT);
        setUserData(data);
        setChatMsg(msg);
        serialize();
    }

    
    // 직렬화를 하는 메소드. Jzlib를 통한 압축도 여기서 한번에 진행됨.
    public void serialize() {
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;

        try {
            baos = new ByteArrayOutputStream();     // Byte 배열화하여 Stream
            oos = new ObjectOutputStream(baos);     // Object를 출력하는 Stream
            oos.writeObject(this);                  // 현재 객체를 Byte Stream으로 출력
            serializedBytes = Jzlib.compress(baos.toByteArray());
            // byte 배열로 직렬화된 객체 정보를 압축하여 저장

            oos.close();

        } catch (Exception e) {
        	System.out.println("ERR - PacketMessage.serialize");
        }
    }

    // 역직렬화 메소드. 압축 풀기도 동시에 진행됨.
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