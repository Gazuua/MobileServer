package mobteam10.project;

import java.io.Serializable;

public class UserData implements Serializable {
    
	private static final long serialVersionUID = 8501L;
	
	private int index;
    private String name;

    public UserData(){ 
        name = "ABCDE";
        index = -1;
    }
    
    public UserData(int index, String name)
    {
        this.index = index;
        this.name = name;
    }

    public int getIndex() {
        return index;
    }
    public String getName() {
        return name;
    }
    
    public void setIndex(int index) {
        this.index = index;
    }
    public void setName(String name) {
        this.name = name;
    }
}
