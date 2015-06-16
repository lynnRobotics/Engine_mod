package util.control.compiler;

import java.io.*;

public class Scanner {
	private Reader reader;
	public Token currentToken;
	public int currLine;
	private int state;
	private String bufferStr;
	public boolean skipNewLine;
	static final int START_STATE = 1; //every FSM should have a start state
	static final int IDENTIFIER_STATE = START_STATE + 1;
	static final int SLASH_STATE = IDENTIFIER_STATE + 1;

	Scanner(Reader reader){
		this.reader = reader;
		currentToken = new Token(); //storing the current analysed token
		currLine = 0; //the line number of the current line being read
		state = 1;
	}
	
	public int makeToken(int type, String text){
	    currentToken.type = type;
	    currentToken.text = text;
	    return type;
	}
	public int makeToken(int type){
		currentToken.type = type;
		return type;
	}
	
	public int nextToken(){
	    while (true){
	        switch (state){
	            case START_STATE:
	            	if (reader.nextChar()==-1)
	            		return makeToken(Token.EOS_TOKEN);
	                char c1 = (char)reader.nextChar();
					if ( (c1>='a' && c1<='z' ) || (c1>='A' && c1<='Z') ){
						this.state = IDENTIFIER_STATE;
						//we need to remember what the token's text is
						bufferStr = Character.toString(c1);
					}else{
						switch (c1){
							case ':':
								return makeToken(Token.COLON_TOKEN);
							case ';':
								return makeToken(Token.SEMICOLON_TOKEN);
							case '(':
								return makeToken(Token.LEFTPAREN_TOKEN);
							case ')':
								return makeToken(Token.RIGHTPAREN_TOKEN);
							case '{':
								return makeToken(Token.LEFTBRACE_TOKEN);
							case '}':
								return makeToken(Token.RIGHTBRACE_TOKEN);
							case '%':
								return makeToken(Token.MOD_TOKEN);
							case '\r': case '\n':
								this.currLine++;
							case '!':
								if (reader.nextChar() == '='){
									return makeToken(Token.NOTEQUAL_TOKEN);
								}else{
									reader.retract();
									return makeToken(Token.NOT_TOKEN);
								}
							case '+':
								char d = (char)reader.nextChar();
								if (d == '='){
									return makeToken(Token.PLUSASSIGN_TOKEN);
								}else if (d == '+'){
									return makeToken(Token.PLUSPLUS_TOKEN);
								}else{
									reader.retract();
									return makeToken(Token.PLUS_TOKEN);
								}
							case '-':
								char d1 = (char)reader.nextChar();
								if (d1 == '='){
									return makeToken(Token.MINUSASSIGN_TOKEN);
								}else if (d1 == '-'){
									return makeToken(Token.MINUSMINUS_TOKEN);
								}else{
									reader.retract();
									return makeToken(Token.MINUS_TOKEN);
								}
							case '*':
								return makeToken(Token.MULT_TOKEN);
							case '=':
								if (reader.nextChar() == '='){
									return makeToken(Token.EQUAL_TOKEN);
								}else{
									this.reader.retract();
									return makeToken(Token.ASSIGN_TOKEN);
								}
							case '>':
								if (reader.nextChar() == '='){
									return makeToken(Token.GREATEREQUAL_TOKEN);
								}else{
									reader.retract();
									return makeToken(Token.GREATER_TOKEN);
								}
							case '<':
								if (reader.nextChar() == '='){
									return makeToken(Token.LESSEQUAL_TOKEN);
								}else{
									reader.retract();
									return makeToken(Token.LESS_TOKEN);
								}
							case '/':
								state = Scanner.SLASH_STATE;
							break;
							case '&':
								if (reader.nextChar() == '&'){
									return makeToken(Token.AND_TOKEN);
								}else{
									reader.retract();
									System.err.println("SYNTAX_ERROR, You have only one &, Line number:"+ Integer.toString(currLine));
								}
							break;
							case '|':
								if (reader.nextChar() == '|'){
									return makeToken(Token.OR_TOKEN);
								}else{
									reader.retract();
									System.err.println("SYNTAX_ERROR, You have only one |, Line number:"+ Integer.toString(currLine));
								}
							break;						
							default:
								//ignore them
						}
					}
	            break;
				case IDENTIFIER_STATE:
					char c2 = (char)reader.nextChar();
					if ((c2 >= 'a' && c2 <= 'z') || (c2 >= 'A' && c2 <= 'Z')){
						bufferStr += Character.toString(c2);
					}else{
						//stop reading it since it is not a letter anymore
						//retract the last character we read because it does not belong to this identfier
						reader.retract();
						//change back the state to read the next token
						state = Scanner.START_STATE;
						if (bufferStr.equals("var"))
							return makeToken(Token.VAR_TOKEN);
						else if (bufferStr.equals("int")||bufferStr.equals("bool"))//need to pass bufferStr as well to distinguish which type it is
							return makeToken(Token.TYPE_TOKEN, bufferStr);
						else if(bufferStr.equals("true")||bufferStr.equals("false")||bufferStr.equals("TRUE")||bufferStr.equals("FALSE"))
							return makeToken(Token.BOOLLITERAL_TOKEN, bufferStr);
						else if(bufferStr.equals("if"))
							return makeToken(Token.IF_TOKEN);
						else if(bufferStr.equals("else"))
							return makeToken(Token.ELSE_TOKEN);
						else if(bufferStr.equals("while"))
							return makeToken(Token.WHILE_TOKEN);
						else if(bufferStr.equals("print"))
							return makeToken(Token.PRINT_TOKEN);
						else
							return makeToken(Token.IDENTIFIER_TOKEN, bufferStr);
					}
				break;
				case SLASH_STATE:
					char d = (char)reader.nextChar();
					if (d == '/'){
						//line comment
						bufferStr = "";
						//reading 1 more char here can prevent the case that a // is followed by a line break char immediately
						d = (char)reader.nextChar();
						if (d != '\r' && d != '\n'){
							while (d != '\r' && d != '\n'){
								bufferStr += d;
								d = (char)reader.nextChar();
							}
							//to retract the line break char
							reader.retract();
						}
						state = Scanner.START_STATE;
						return makeToken(Token.LINECOMMENT_TOKEN, bufferStr);
					}else if (d == '*'){
						//block comment
						bufferStr = "";
						boolean end = false;
						while (!end){
							if (reader.nextChar() != -1){
								d = (char)reader.nextChar();
								if (d == '\r' || d == '\n'){
									currLine++;
								}
								if (d == '*'){
									char e = (char)reader.nextChar();
									if (e == '/'){
										//meet */
										end = true;
									}else{
										bufferStr += "*" + e;
									}
								}else{
									bufferStr += d;
								}
							}else{
								end = true;
							}
						}
						state = Scanner.START_STATE;
						return makeToken(Token.BLOCKCOMMENT_TOKEN, bufferStr);
					}else{
						state = Scanner.START_STATE;
						reader.retract();
						return makeToken(Token.DIV_TOKEN);
					}		
	        }
	    }
	}
	
	
	
}
