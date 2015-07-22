package util.control.compiler;

import java.util.List;

import util.control.compiler.Parser.ExpressionBlockNode;
import util.control.compiler.Parser.Node;


public class Analyser {

	List<Node> nodes;
	//Analyser class
	Analyser(){
	}
	void evaluateExpressionBlockNode(ExpressionBlockNode node){
	    for (int i = 0, l = node.expressionNodeList.size(); i < l; i++){
	        Node expressionNode = node.expressionNodeList.get(i);
	        evaluateExpressionNode(expressionNode);
	    }
	}
	void evaluateExpressionNode(ExpressionNode node){
	    if (node instanceof VariableNode){
	        evaluateVariableNode(node);
	    }
	}
	void evaluateVariableNode(Node node){
	    if (nodes[node.varName]){
	        //this variable has been declared before
	        //since we can find it in our variable table
	        System.err.println("SEMANTIC_ERROR, The variable \"" + node.varName + "\" has been declared already, line: "+Integer.toString(node.line));
	    }else{
	    	nodes[node.varName] = node;
	        //if we do not use "else", this variable declaration will replace the previous one
	        //This may result in wrong data type checking later on
	    }
	    this.evaluateExpressionNode(node.initExpressionNode);
	}
}
