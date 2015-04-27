package IntelM2M.control.compiler;

public class Reader {
	
	//str is the data to be read
	Reader(char[] str){
		data = str;
		currPos = 0;
		dataLength = str.length;
	}

	private char[] data;
	private int currPos;
	private int dataLength;
	
	public char nextChar(){
		if (this.currPos >= this.dataLength){
	        return -1; //end of stream
	    }
	    return data[currPos++];
	}
	
	//n is the number of characters to be retracted
	public void retract(){
		currPos -=1;
		if (currPos < 0)
			currPos = 0;
	}
	public void retract(int n){
	    currPos -= n;
	    if (currPos < 0){
	        currPos = 0;
	    }
	}
}
