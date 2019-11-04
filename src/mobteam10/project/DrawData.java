package mobteam10.project;

import java.io.Serializable;

// 그림 좌표와 색을 short값으로 저장한 클래스.
// color 값의 경우 패킷에서 하나만 두었어야 하는데 조금 잘못 설계한 감이 있...
// 덕분에 중복 데이터가 많아 압축률이 올라갔다는 웃지 못할 일도 있........
// 시간 관계상 수정은 방학 시작 이후로..
public class DrawData implements Serializable {
	
	private static final long serialVersionUID = 8501L;
	
    private short x;
    private short y;
    private short color;
    
    public DrawData(short x, short y, short color)
    {
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public short getX() {
        return x;
    }

    public void setX(short x) {
        this.x = x;
    }

    public short getY() {
        return y;
    }

    public void setY(short y) {
        this.y = y;
    }

    public short getColor() {
        return color;
    }

    public void setColor(short color) {
        this.color = color;
    }
}
