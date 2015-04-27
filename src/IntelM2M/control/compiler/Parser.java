package IntelM2M.control.compiler;

public class Parser {
	
	Parser(Scanner scanner){
		this.scanner = scanner;
		currentToken = new Token();
		lookaheadToken = new Token();
		lookaheadTokenConsumed = true;
	}
	
	private Scanner scanner;
	private Token currentToken;
	private Token lookaheadToken;
	private boolean lookaheadTokenConsumed;

	int nextToken(){
	    if (lookaheadTokenConsumed){
	        int token = scanner.nextToken();
	        //skip comments
	        while (token == Token.tokens.LINECOMMENT_TOKEN || token == Token.tokens.BLOCKCOMMENT_TOKEN){
	            token = scanner.nextToken();
	        }
	        this.currentToken.type = token;
	        this.currentToken.text = scanner.currentToken.text;
	        return token;
	    }else{
	        this.currentToken.type = lookaheadToken.type;
	        this.currentToken.text = lookaheadToken.text;
	        this.lookaheadTokenConsumed = true;
	        return currentToken.type;
	    }
	}
	
	int lookahead(){
	    if (lookaheadTokenConsumed){
	        int token = scanner.nextToken();
	        //skip comments
	        while (token == Token.tokens.LINECOMMENT_TOKEN || token == Token.tokens.BLOCKCOMMENT_TOKEN){
	            token = this.scanner.nextToken();
	        }
	        this.lookaheadToken.type = token;
	        this.lookaheadToken.text = scanner.currentToken.text;
	        this.lookaheadTokenConsumed = false;
	        return token;
	    }else{
	        return lookaheadToken.type;
	    }
	}
	
	Node parse(){
	    Node rootBlock = new ExpressionBlockNode();
	    parseExpressions(rootBlock);
	    return rootBlock;
	}
	
	//to parse a list of expressions
	void parseExpressions(expressionBlockNode){
	    while (this.lookahead() != Token.tokens.RIGHTBRACE_TOKEN &&
	            this.lookahead() != Token.tokens.EOS_TOKEN){
	        var expressionNode = parseExpression();
	        if (expressionNode){
	            expressionBlockNode.push(expressionNode);
	        }
	    }
	}
	
	//to parse an expression
	Node parseExpression(){
	    switch (lookahead()){
	        case Token.tokens.PRINT_TOKEN:
	            int printToken = nextToken();
	            var expressionNode = parseExpression();
				if (expressionNode == undefined){
					Errors.push({
						type: Errors.SYNTAX_ERROR,
						msg: "Missing an expression after \"print\"",
						line: this.scanner.currLine
					});
				}
	            return new PrintNode(expressionNode);
	        break;
	        case Token.tokens.INTLITERAL_TOKEN:
	            int intToken = nextToken();
	            return new IntNode(this.currentToken.text);
	        break;
			case Token.tokens.VAR_TOKEN:
				return parseVarExpression();
			break;
			case Token.tokens.IF_TOKEN:
				return this.parseIfExpression();
			break;
			case Token.tokens.WHILE_TOKEN:
				return this.parseWhileExpression();
			break;				
	        default:
	            //unexpected, consume it
	            this.nextToken();
	    }
	}

	void parseVarExpression(){
	    //consume "var"
	    this.nextToken();
	    //expecting an identifier
	    if (this.lookahead() == Token.tokens.IDENTIFIER_TOKEN){
	        this.nextToken();
	        var varName = this.currentToken.text;
	        //consume a colon
	        if (this.nextToken() != Token.tokens.COLON_TOKEN){
	            this.skipError();
	            return;
	        }
	        //type token
	        if (this.lookahead() != Token.tokens.TYPE_TOKEN){
	            this.skipError();
	            return;
	        }
	        this.nextToken();
	        var typeName = this.currentToken.text;
	        var initNode;
	        //check if it has initialization expression
	        if (this.lookahead() == Token.tokens.ASSIGN_TOKEN){
	            initNode = this.parseSimpleAssignmentExpression();
	        }
	        return new VariableNode(varName, typeName, initNode);
	    }
	    this.skipError();
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
	    while (this.lookahead() != Token.tokens.NEWLINE_TOKEN && lookahead() != Token.tokens.EOS_TOKEN){
	        nextToken();
	    }
	    scanner.skipNewLine = true;
	}

	// if, while statement
	void matchSemicolon(){
	    //consume the semicolon
	    if (this.lookahead() == Token.tokens.SEMICOLON_TOKEN){
	        this.nextToken();
	    }else{
	        //syntax error
	        Errors.push({
	            type: Errors.SYNTAX_ERROR,
	            msg: "Expecting a semicolon at the end of expression",
	            line: this.scanner.currLine
	        });
	    }
	}

	Node parseIfExpression(){
	    //consume "if"
	    this.nextToken();
	    var condition = this.parseParenExpression();
	    var expressions = this.parseExpressionBlock();
	    var elseExpressions;
	    if (this.lookahead() == Token.tokens.ELSE_TOKEN){
	        //consume "else"
	        this.nextToken();
	        elseExpressions = this.parseExpressionBlock();
	    }
	    return new IfNode(condition, expressions, elseExpressions);
	}

	// operand
	Node parseOperand(){
	    int token = nextToken();
	    Node operandNode;
	    switch (token){
	        case Token.tokens.INTLITERAL_TOKEN:
	            operandNode = new IntNode(this.currentToken.text);
	        break;
	        case Token.tokens.BOOLLITERAL_TOKEN:
	            operandNode = new BoolNode(this.currentToken.text);
	        break;
	        case Token.tokens.IDENTIFIER_TOKEN:
	            operandNode = new IdentifierNode(this.currentToken.text);
				if (this.lookahead() == Token.tokens.MINUSMINUS_TOKEN){
					//post decrement
					this.nextToken();
					operandNode = new PostDecrementNode(operandNode);
				}else if (this.lookahead() == Token.tokens.PLUSPLUS_TOKEN){
					//post increment
					this.nextToken();
					operandNode = new PostIncrementNode(operandNode);
				}
	        break;
	        case Token.tokens.LEFTPAREN_TOKEN:
	            operandNode = new ParenNode(this.parseCompoundExpression(0));
	            //consume the right paren )
	            if (this.lookahead() == Token.tokens.RIGHTPAREN_TOKEN){
	                this.nextToken();
	            }else{
	                Errors.push({
	                    type: Errors.SYNTAX_ERROR,
	                    msg: "Missing right paren \")\"",
	                    line: this.scanner.currLine
	                });
	            }
	        break;
	        case Token.tokens.MINUS_TOKEN:
	            operandNode = new NegateNode(this.parseOperand());
	        break;
			case Token.tokens.PLUSPLUS_TOKEN:
				if (this.lookahead() == Token.tokens.IDENTIFIER_TOKEN){
					this.nextToken();
					operandNode = new PreIncrementNode(new IdentifierNode(this.currentToken.text));
				}else{
					Errors.push({
						type: Errors.SYNTAX_ERROR,
						msg: "Expecting an identifier for pre-increment expression",
						line: this.scanner.currLine
					});
					return null;
				}
			break;
	        default:
	            //not valid
	            Errors.push({
	                type: Errors.SYNTAX_ERROR,
	                msg: "Unexpected token",
	                line: this.scanner.currLine
	            });
	            return null;
	    }
	    return operandNode;
	}

	int getBindingPower = function (token){
	    switch (token){
	        case Token.tokens.PLUS_TOKEN:
	        case Token.tokens.MINUS_TOKEN:
	            return 120;
	        case Token.tokens.MULT_TOKEN:
	        case Token.tokens.DIV_TOKEN:
	            return 130;
	    }
	    return -1;
	}
	
	
	void createOperatorNode(operatorToken){
	    switch (operatorToken){
	        case Token.tokens.PLUS_TOKEN:
	            return new OperatorPlusNode();
	        break;
	        case Token.tokens.MINUS_TOKEN:
	            return new OperatorMinusNode();
	        break;
	        case Token.tokens.MULT_TOKEN:
	            return new OperatorMultNode();
	        break;
	        case Token.tokens.DIV_TOKEN:
	            return new OperatorDivNode();
	        break;
	    }
	}
	
	Node parseCompoundExpression(rightBindingPower){
	    var operandNode = this.parseOperand();
	    if (operandNode == null){
	        return operandNode;
	    }
	    var compoundExpressionNode = new CompoundNode();
	    compoundExpressionNode.push(operandNode);
	    var operator = this.lookahead();
	    var leftBindingPower = this.getBindingPower(operator);
	    if (leftBindingPower == -1){
	        //not an operator
	        return compoundExpressionNode;
	    }
	    while (rightBindingPower < leftBindingPower){
	        operator = this.nextToken();
	        compoundExpressionNode.push(this.createOperatorNode(operator));
	        var node = this.parseCompoundExpression(leftBindingPower);
	        compoundExpressionNode.push(node);
	        var oper = this.lookahead();
	        leftBindingPower = this.getBindingPower(oper);
	        if (leftBindingPower == -1){
	            //not an operator
	            return compoundExpressionNode;
	        }
	    }
	    return compoundExpressionNode;
	}

}

