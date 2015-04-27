package IntelM2M.control.compiler;

import java.util.HashMap;
import java.util.Map;

public class Token {
	
	class tokens{
		static int EOS_TOKEN = 1; //end of stream
		// using + 1 allows adding a new token easily later
		static int COLON_TOKEN = EOS_TOKEN + 1;
		static int SEMICOLON_TOKEN = COLON_TOKEN + 1;
		static int LEFTPAREN_TOKEN = SEMICOLON_TOKEN + 1;
		static int RIGHTPAREN_TOKEN = LEFTPAREN_TOKEN + 1;
		static int LEFTBRACE_TOKEN = RIGHTPAREN_TOKEN + 1;
		static int RIGHTBRACE_TOKEN = LEFTBRACE_TOKEN + 1;
		static int MOD_TOKEN = RIGHTBRACE_TOKEN + 1;
		// multi-char tokens
		static int VAR_TOKEN = MOD_TOKEN + 1;
		static int TYPE_TOKEN = VAR_TOKEN + 1;
		static int BOOLLITERAL_TOKEN = TYPE_TOKEN + 1;
		static int IF_TOKEN = BOOLLITERAL_TOKEN + 1;
		static int ELSE_TOKEN = IF_TOKEN + 1;
		static int WHILE_TOKEN = ELSE_TOKEN + 1;
		static int PRINT_TOKEN = WHILE_TOKEN + 1;
		static int IDENTIFIER_TOKEN = PRINT_TOKEN + 1;
		// operator
		static int PLUS_TOKEN = IDENTIFIER_TOKEN + 1;
		static int PLUSPLUS_TOKEN = PLUS_TOKEN + 1;
		static int PLUSASSIGN_TOKEN = PLUSPLUS_TOKEN + 1;
		static int MINUS_TOKEN = PLUSASSIGN_TOKEN + 1;
		static int MINUSMINUS_TOKEN = MINUS_TOKEN + 1;
		static int MINUSASSIGN_TOKEN = MINUSMINUS_TOKEN + 1;
		static int MULT_TOKEN = MINUSASSIGN_TOKEN + 1;
		static int DIV_TOKEN = MULT_TOKEN + 1;
		static int ASSIGN_TOKEN = DIV_TOKEN + 1;
		static int EQUAL_TOKEN = ASSIGN_TOKEN + 1;
		static int NOTEQUAL_TOKEN = EQUAL_TOKEN + 1;
		static int GREATER_TOKEN = NOTEQUAL_TOKEN + 1;
		static int GREATEREQUAL_TOKEN = GREATER_TOKEN + 1;
		static int LESS_TOKEN = GREATEREQUAL_TOKEN + 1;
		static int LESSEQUAL_TOKEN = LESS_TOKEN + 1;
		static int AND_TOKEN = LESSEQUAL_TOKEN + 1;
		static int OR_TOKEN = AND_TOKEN + 1;
		static int NOT_TOKEN = OR_TOKEN + 1;
		static int LINECOMMENT_TOKEN = NOT_TOKEN + 1;
		static int BLOCKCOMMENT_TOKEN = LINECOMMENT_TOKEN + 1;
	}
	Token(){ 	}
	Token(int type,String text){
		this.type = type;
		this.text = text;
	}
	Token.backwardMap = {}; //for inverse look-up
	for (var x in Token.tokens){
		Token.backwardMap[Token.tokens[x]] = x;
	}
	
	public int type;
	public String text;		
}
