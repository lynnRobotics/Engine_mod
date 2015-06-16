package util.control.compiler;

import java.util.HashMap;
import java.util.Map;

public class Token {
	
	static final int EOS_TOKEN = 1; //end of stream
	// using + 1 allows adding a new token easily later
	static final int COLON_TOKEN = EOS_TOKEN + 1;
	static final int SEMICOLON_TOKEN = COLON_TOKEN + 1;
	static final int LEFTPAREN_TOKEN = SEMICOLON_TOKEN + 1;
	static final int RIGHTPAREN_TOKEN = LEFTPAREN_TOKEN + 1;
	static final int LEFTBRACE_TOKEN = RIGHTPAREN_TOKEN + 1;
	static final int RIGHTBRACE_TOKEN = LEFTBRACE_TOKEN + 1;
	static final int MOD_TOKEN = RIGHTBRACE_TOKEN + 1;
	// multi-char tokens
	static final int VAR_TOKEN = MOD_TOKEN + 1;
	static final int TYPE_TOKEN = VAR_TOKEN + 1;
	static final int BOOLLITERAL_TOKEN = TYPE_TOKEN + 1;
	static final int IF_TOKEN = BOOLLITERAL_TOKEN + 1;
	static final int ELSE_TOKEN = IF_TOKEN + 1;
	static final int WHILE_TOKEN = ELSE_TOKEN + 1;
	static final int PRINT_TOKEN = WHILE_TOKEN + 1;
	static final int IDENTIFIER_TOKEN = PRINT_TOKEN + 1;
	// operator
	static final int PLUS_TOKEN = IDENTIFIER_TOKEN + 1;
	static final int PLUSPLUS_TOKEN = PLUS_TOKEN + 1;
	static final int PLUSASSIGN_TOKEN = PLUSPLUS_TOKEN + 1;
	static final int MINUS_TOKEN = PLUSASSIGN_TOKEN + 1;
	static final int MINUSMINUS_TOKEN = MINUS_TOKEN + 1;
	static final int MINUSASSIGN_TOKEN = MINUSMINUS_TOKEN + 1;
	static final int MULT_TOKEN = MINUSASSIGN_TOKEN + 1;
	static final int DIV_TOKEN = MULT_TOKEN + 1;
	static final int ASSIGN_TOKEN = DIV_TOKEN + 1;
	static final int EQUAL_TOKEN = ASSIGN_TOKEN + 1;
	static final int NOTEQUAL_TOKEN = EQUAL_TOKEN + 1;
	static final int GREATER_TOKEN = NOTEQUAL_TOKEN + 1;
	static final int GREATEREQUAL_TOKEN = GREATER_TOKEN + 1;
	static final int LESS_TOKEN = GREATEREQUAL_TOKEN + 1;
	static final int LESSEQUAL_TOKEN = LESS_TOKEN + 1;
	static final int AND_TOKEN = LESSEQUAL_TOKEN + 1;
	static final int OR_TOKEN = AND_TOKEN + 1;
	static final int NOT_TOKEN = OR_TOKEN + 1;
	static final int LINECOMMENT_TOKEN = NOT_TOKEN + 1;
	static final int BLOCKCOMMENT_TOKEN = LINECOMMENT_TOKEN + 1;
	static final int NEWLINE_TOKEN = BLOCKCOMMENT_TOKEN;
	static final int INTLITERAL_TOKEN = NEWLINE_TOKEN;
		
	Token(){ 	}
	Token(int type,String text){
		this.type = type;
		this.text = text;
	}
	public int type;
	public String text;
}
