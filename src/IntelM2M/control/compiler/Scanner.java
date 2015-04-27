package IntelM2M.control.compiler;

public class Scanner {

	Scanner(Reader reader){
		this.reader = reader;
		currentToken = new Token(); //storing the current analysed token
		currLine = 0; //the line number of the current line being read
		state = Scanner.START_STATE;
	}
	
	private Reader reader;
	public Token currentToken;
	private int currLine;
	private int state;
	private String bufferStr;
	static int START_STATE = 1; //every FSM should have a start state
	static int IDENTIFIER_STATE = Scanner.START_STATE + 1;
	static int SLASH_STATE = Scanner.IDENTIFIER_STATE + 1;

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
	                char c1 = reader.nextChar();
					if ( (c1>='a' && c1<='z' ) || (c1>='A' && c1<='Z') ){
						this.state = Scanner.IDENTIFIER_STATE;
						//we need to remember what the token's text is
						bufferStr = Character.toString(c1);
					}else{
						switch (c1){
							case ':':
								return makeToken(Token.tokens.COLON_TOKEN);
							break;
							case ';':
								return makeToken(Token.tokens.SEMICOLON_TOKEN);
							break;
							case '(':
								return makeToken(Token.tokens.LEFTPAREN_TOKEN);
							break;
							case ')':
								return makeToken(Token.tokens.RIGHTPAREN_TOKEN);
							break;
							case '{':
								return makeToken(Token.tokens.LEFTBRACE_TOKEN);
							break;
							case '}':
								return makeToken(Token.tokens.RIGHTBRACE_TOKEN);
							break;
							case '%':
								return makeToken(Token.tokens.MOD_TOKEN);
							break;
							case (char)-1:
								return makeToken(Token.tokens.EOS_TOKEN);
							break;
							case '\r': case '\n':
								this.currLine++;
							case '!':
								if (this.reader.nextChar() == '='){
									return makeToken(Token.tokens.NOTEQUAL_TOKEN);
								}else{
									this.reader.retract();
									return makeToken(Token.tokens.NOT_TOKEN);
								}
							break;
							case '+':
								char d = this.reader.nextChar();
								if (d == '='){
									return makeToken(Token.tokens.PLUSASSIGN_TOKEN);
								}else if (d == '+'){
									return makeToken(Token.tokens.PLUSPLUS_TOKEN);
								}else{
									this.reader.retract();
									return makeToken(Token.tokens.PLUS_TOKEN);
								}
							break;
							case '-':
								char d1 = this.reader.nextChar();
								if (d1 == '='){
									return makeToken(Token.tokens.MINUSASSIGN_TOKEN);
								}else if (d1 == '-'){
									return makeToken(Token.tokens.MINUSMINUS_TOKEN);
								}else{
									this.reader.retract();
									return makeToken(Token.tokens.MINUS_TOKEN);
								}
							break;
							case '*':
								return makeToken(Token.tokens.MULT_TOKEN);
							break;
							case '=':
								if (this.reader.nextChar() == '='){
									return makeToken(Token.tokens.EQUAL_TOKEN);
								}else{
									this.reader.retract();
									return makeToken(Token.tokens.ASSIGN_TOKEN);
								}
							break;
							case '>':
								if (this.reader.nextChar() == '='){
									return makeToken(Token.tokens.GREATEREQUAL_TOKEN);
								}else{
									this.reader.retract();
									return makeToken(Token.tokens.GREATER_TOKEN);
								}
							break;
							case '<':
								if (this.reader.nextChar() == '='){
									return makeToken(Token.tokens.LESSEQUAL_TOKEN);
								}else{
									this.reader.retract();
									return makeToken(Token.tokens.LESS_TOKEN);
								}
							break;
							case '/':
								this.state = Scanner.SLASH_STATE;
							break;
							case '&':
								if (this.reader.nextChar() == '&'){
									return makeToken(Token.tokens.AND_TOKEN);
								}else{
									this.reader.retract();
									Errors.push({
										type: Errors.SYNTAX_ERROR,
										msg: "You have only one &",
										line: this.currLine
									});
								}
							break;
							case '|':
								if (this.reader.nextChar() == '|'){
									return makeToken(Token.tokens.OR_TOKEN);
								}else{
									this.reader.retract();
									Errors.push({
										type: Errors.SYNTAX_ERROR,
										msg: "You have only one |",
										line: this.currLine
									});
								}
							break;						
							default:
								//ignore them
						}
					}
	            break;
				case IDENTIFIER_STATE:
					char c2 = reader.nextChar();
					if ((c2 >= 'a' && c2 <= 'z') || (c2 >= 'A' && c2 <= 'Z')){
						bufferStr += Character.toString(c2);
					}else{
						//stop reading it since it is not a letter anymore
						//retract the last character we read because it does not belong to this identfier
						reader.retract();
						//change back the state to read the next token
						state = Scanner.START_STATE;
						if (bufferStr.equals("var"))
							return makeToken(Token.tokens.VAR_TOKEN);
						else if (bufferStr.equals("int")||bufferStr.equals("bool"))//need to pass bufferStr as well to distinguish which type it is
							return makeToken(Token.tokens.TYPE_TOKEN, bufferStr);
						else if(bufferStr.equals("true")||bufferStr.equals("false")||bufferStr.equals("TRUE")||bufferStr.equals("FALSE"))
							return makeToken(Token.tokens.BOOLLITERAL_TOKEN, bufferStr);
						else if(bufferStr.equals("if"))
							return makeToken(Token.tokens.IF_TOKEN);
						else if(bufferStr.equals("else"))
							return makeToken(Token.tokens.ELSE_TOKEN);
						else if(bufferStr.equals("while"))
							return makeToken(Token.tokens.WHILE_TOKEN);
						else if(bufferStr.equals("print"))
							return makeToken(Token.tokens.PRINT_TOKEN);
						else
							return makeToken(Token.tokens.IDENTIFIER_TOKEN, bufferStr);
					}
				break;
				case SLASH_STATE:
					char d = reader.nextChar();
					if (d == '/'){
						//line comment
						bufferStr = "";
						//reading 1 more char here can prevent the case that a // is followed by a line break char immediately
						d = this.reader.nextChar();
						if (d != '\r' && d != '\n'){
							while (d != '\r' && d != '\n'){
								bufferStr += d;
								d = this.reader.nextChar();
							}
							//to retract the line break char
							this.reader.retract();
						}
						this.state = Scanner.START_STATE;
						return makeToken(Token.tokens.LINECOMMENT_TOKEN, bufferStr);
					}else if (d == '*'){
						//block comment
						bufferStr = "";
						boolean end = false;
						while (! end){
							d = this.reader.nextChar();
							if (d != -1){
								if (d == '\r' || d == '\n'){
									this.currLine++;
								}
								if (d == '*'){
									char e = reader.nextChar();
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
						this.state = Scanner.START_STATE;
						return makeToken(Token.tokens.BLOCKCOMMENT_TOKEN, bufferStr);
					}else{
						this.state = Scanner.START_STATE;
						this.reader.retract();
						return makeToken(Token.tokens.DIV_TOKEN);
					}
				break;			
	        }
	    }
	}
	
	
	
}
