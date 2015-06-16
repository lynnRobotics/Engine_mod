package util.control.compiler;

import java.util.List;

public class Parser {
	
	private Scanner scanner;
	private Token currentToken;
	private Token lookaheadToken;
	private boolean lookaheadTokenConsumed;
	
	Parser(Scanner scanner){
		this.scanner = scanner;
		currentToken = new Token();
		lookaheadToken = new Token();
		lookaheadTokenConsumed = true;
	}

	int nextToken(){
	    if (lookaheadTokenConsumed){
	        int token = scanner.nextToken();
	        //skip comments
	        while (token == Token.LINECOMMENT_TOKEN || token == Token.BLOCKCOMMENT_TOKEN){
	            token = scanner.nextToken();
	        }
	        currentToken.type = token;
	        currentToken.text = scanner.currentToken.text;
	        return token;
	    }else{
	        currentToken.type = lookaheadToken.type;
	        currentToken.text = lookaheadToken.text;
	        lookaheadTokenConsumed = true;
	        return currentToken.type;
	    }
	}
	
	int lookahead(){
	    if (lookaheadTokenConsumed){
	        int token = scanner.nextToken();
	        //skip comments
	        while (token == Token.LINECOMMENT_TOKEN || token == Token.BLOCKCOMMENT_TOKEN){
	            token = scanner.nextToken();
	        }
	        lookaheadToken.type = token;
	        lookaheadToken.text = scanner.currentToken.text;
	        lookaheadTokenConsumed = false;
	        return token;
	    }else{
	        return lookaheadToken.type;
	    }
	}
	
	Node parse(){
		ExpressionBlockNode rootBlock = new ExpressionBlockNode();
	    parseExpressions(rootBlock);
	    return rootBlock;
	}
	
	//to parse a list of expressions
	void parseExpressions(ExpressionBlockNode expressionBlockNode){
	    while (lookahead() != Token.RIGHTBRACE_TOKEN && lookahead() != Token.EOS_TOKEN){
	        Node expressionNode = parseExpression();
	        if (expressionNode.undefined){
	            expressionBlockNode.expressionNodeList.add(expressionNode);
	        }
	    }
	}
	
	//to parse an expression
	Node parseExpression(){
	    switch (lookahead()){
	        case Token.PRINT_TOKEN:
	            int printToken = nextToken();
	            Node expressionNode = parseExpression();
				if (expressionNode.undefined == true){
					System.err.println("SYNTAX_ERROR, Missing an expression after \"print\", line:"+Integer.toString(scanner.currLine));
				}
	            return new PrintNode(expressionNode);
	        case Token.INTLITERAL_TOKEN:
	            int intToken = nextToken();
	            return new IntNode(currentToken.text);
			case Token.VAR_TOKEN:
				return parseVarExpression();
			case Token.IF_TOKEN:
				return parseIfExpression();
			case Token.WHILE_TOKEN:
				return parseWhileExpression();	
	        default:
	            //unexpected, consume it
	            this.nextToken();
	            return new Node(true);
	    }
	}

	Node parseVarExpression(){
	    //consume "var"
	    nextToken();
	    //expecting an identifier
	    if (lookahead() == Token.IDENTIFIER_TOKEN){
	        nextToken();
	        String varName = this.currentToken.text;
	        //consume a colon
	        if (nextToken() != Token.COLON_TOKEN){
	            skipError();
	            return new Node(true);
	        }
	        //type token
	        if (lookahead() != Token.TYPE_TOKEN){
	            skipError();
	            return new Node(true);
	        }
	        nextToken();
	        String typeName = currentToken.text;
	        Node initNode = new Node();
	        //check if it has initialization expression
	        if (lookahead() == Token.ASSIGN_TOKEN){
	            initNode = parseSimpleAssignmentExpression();
	        }
	        return new VariableNode(varName, typeName, initNode);
	    }
	    skipError();
	    return new Node(true);
	}

	Node parseSimpleAssignmentExpression(){
	    //consume the "=" sign
	    this.nextToken();
	    Node expressionNode = parseExpression();
	    return expressionNode;
	}

	// skip error
	//a naive implementation for skipping error
	void skipError(){
	    scanner.skipNewLine = false;
	    while (this.lookahead() != Token.NEWLINE_TOKEN && lookahead() != Token.EOS_TOKEN){
	        nextToken();
	    }
	    scanner.skipNewLine = true;
	}

	// if, while statement
	void matchSemicolon(){
	    //consume the semicolon
	    if (this.lookahead() == Token.SEMICOLON_TOKEN){
	        this.nextToken();
	    }else{
	        //syntax error
	        System.err.println("SYNTAX_ERROR, Expecting a semicolon at the end of expression, line: "+Integer.toString(scanner.currLine));
	    }
	}

	Node parseIfExpression(){
	    //consume "if"
	    this.nextToken();
	    Node condition = parseParenExpression();
	    Node expressions = parseExpressionBlock();
	    Node elseExpressions = new Node(true);
	    if (this.lookahead() == Token.ELSE_TOKEN){
	        //consume "else"
	        this.nextToken();
	        elseExpressions = parseExpressionBlock();
	    }
	    return new IfNode(condition, expressions, elseExpressions);
	}

	// operand
	Node parseOperand(){
	    int token = nextToken();
	    Node operandNode;
	    switch (token){
	        case Token.INTLITERAL_TOKEN:
	            operandNode = new IntNode(currentToken.text);
	        break;
	        case Token.BOOLLITERAL_TOKEN:
	            operandNode = new BoolNode(currentToken.text);
	        break;
	        case Token.IDENTIFIER_TOKEN:
	            operandNode = new IdentifierNode(currentToken.text);
				if (this.lookahead() == Token.MINUSMINUS_TOKEN){
					//post decrement
					this.nextToken();
					operandNode = new PostDecrementNode(operandNode);
				}else if (this.lookahead() == Token.PLUSPLUS_TOKEN){
					//post increment
					this.nextToken();
					operandNode = new PostIncrementNode(operandNode);
				}
	        break;
	        case Token.LEFTPAREN_TOKEN:
	            operandNode = new ParenNode(parseCompoundExpression(0));
	            //consume the right paren )
	            if (this.lookahead() == Token.RIGHTPAREN_TOKEN){
	                this.nextToken();
	            }else{
	            	System.err.println("SYNTAX_ERROR, Missing right paren \")\" line: "+Integer.toString(scanner.currLine));
	            }
	        break;
	        case Token.MINUS_TOKEN:
	            operandNode = new NegateNode(parseOperand());
	        break;
			case Token.PLUSPLUS_TOKEN:
				if (this.lookahead() == Token.IDENTIFIER_TOKEN){
					this.nextToken();
					operandNode = new PreIncrementNode(new IdentifierNode(currentToken.text));
				}else{
					System.err.println("SYNTAX_ERROR, Expecting an identifier for pre-increment expression, line: "+Integer.toString(scanner.currLine));
					return null;
				}
			break;
	        default:
	            //not valid
	            System.err.println("SYNTAX_ERROR, Unexpected token, line: "+Integer.toString(scanner.currLine));
	            return null;
	    }
	    return operandNode;
	}

	int getBindingPower(int token){
	    switch (token){
	        case Token.PLUS_TOKEN:
	        case Token.MINUS_TOKEN:
	            return 120;
	        case Token.MULT_TOKEN:
	        case Token.DIV_TOKEN:
	            return 130;
	    }
	    return -1;
	}
	
	Node createOperatorNode(int operatorToken){
	    switch (operatorToken){
	        case Token.PLUS_TOKEN:
	            return new OperatorPlusNode();
	        case Token.MINUS_TOKEN:
	            return new OperatorMinusNode();
	        case Token.MULT_TOKEN:
	            return new OperatorMultNode();
	        case Token.DIV_TOKEN:
	            return new OperatorDivNode();
	    }
		return new Node(true);
	}
	
	Node parseCompoundExpression(int rightBindingPower){
	    Node operandNode = parseOperand();
	    if (operandNode == null){
	        return operandNode;
	    }
	    CompoundNode compoundExpressionNode = new CompoundNode();
	    compoundExpressionNode.compoundExpressionNodeList.add(operandNode);
	    int operator = lookahead();
	    int leftBindingPower = getBindingPower(operator);
	    if (leftBindingPower == -1){
	        //not an operator
	        return compoundExpressionNode;
	    }
	    while (rightBindingPower < leftBindingPower){
	        operator = nextToken();
	        compoundExpressionNode.compoundExpressionNodeList.add(createOperatorNode(operator));
	        Node node = parseCompoundExpression(leftBindingPower);
	        compoundExpressionNode.compoundExpressionNodeList.add(node);
	        int oper = lookahead();
	        leftBindingPower = this.getBindingPower(oper);
	        if (leftBindingPower == -1){
	            //not an operator
	            return compoundExpressionNode;
	        }
	    }
	    return compoundExpressionNode;
	}

	Node parseParenExpression(){
		return new Node(true);
	}
	
	Node parseExpressionBlock(){
		return new Node(true);
	}
	
	Node parseWhileExpression(){
		return new Node(true);
	}
	
	public class Node{
		Node(){
			setLine(scanner.currLine);
		}
		Node(boolean bool){
			undefined = bool;
			setLine(scanner.currLine);
		}
		
		Node(String text){
			setLine(scanner.currLine);
		}
		public boolean undefined = false;
		public int line = 0;
		
		public void setLine(int line){
			this.line = line;
		}
	}
	
	public class OperatorPlusNode extends Node{
		OperatorPlusNode(){
			setLine(scanner.currLine);
		}
	}
	
	public class ExpressionBlockNode extends Node{
		public List<Node> expressionNodeList;
		ExpressionBlockNode(){
			setLine(scanner.currLine);
		}
	}
	
	public class VariableNode extends Node{
		VariableNode(String varName,String typeName, Node initNode){
			setLine(scanner.currLine);
		}
	}
	
	public class PrintNode extends Node{
		PrintNode(Node node){
			setLine(scanner.currLine);
		}
		
	}
	
	public class IntNode extends Node{
		IntNode(String text){
			setLine(scanner.currLine);
		}
	}
	
	public class BoolNode extends Node{
		BoolNode(String text){
			setLine(scanner.currLine);
		}
	}
	
	public class IdentifierNode extends Node{
		IdentifierNode(String text){
			setLine(scanner.currLine);
		}
	}
	
	public class PostDecrementNode extends Node{
		PostDecrementNode(Node node){
			setLine(scanner.currLine);
		}
	}
	
	public class PostIncrementNode extends Node{
		PostIncrementNode(Node node){
			setLine(scanner.currLine);
		}
	}
	
	public class ParenNode extends Node{
		ParenNode(Node node){
			setLine(scanner.currLine);
		}
	}
	
	public class NegateNode extends Node{
		NegateNode(Node node){
			setLine(scanner.currLine);
		}
	}
	
	public class PreIncrementNode extends Node{
		PreIncrementNode(Node node){
			setLine(scanner.currLine);
		}
	}
	
	public class OperatorMinusNode extends Node{
		OperatorMinusNode(){
			setLine(scanner.currLine);
		}
	}
	
	public class OperatorMultNode extends Node{
		OperatorMultNode(){
			setLine(scanner.currLine);
		}
	}
	
	public class OperatorDivNode extends Node{
		OperatorDivNode(){
			setLine(scanner.currLine);
		}
	}
	
	public class CompoundNode extends Node{
		public List<Node> compoundExpressionNodeList;
		CompoundNode(){
			setLine(scanner.currLine);
		}
	}

	public class IfNode extends Node{
		IfNode(Node condition, Node expressions, Node elseExpressions){
			setLine(scanner.currLine);
		}
	}
}

