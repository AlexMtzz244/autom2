package proyecto.lenguaje.parser;

import java.util.*;
import proyecto.lenguaje.lexer.Token;

// Nodo base
public abstract class AstNode {
	// imprime árbol con indentación
	public String toTreeString() {
		StringBuilder sb = new StringBuilder();
		buildTree(sb, 0);
		return sb.toString();
	}
	protected abstract void buildTree(StringBuilder sb, int indent);
	protected void indent(StringBuilder sb, int n) {
		for (int i = 0; i < n; i++) sb.append("  ");
	}
}

// Programa: lista de declaraciones/expresiones
class ProgramNode extends AstNode {
	public final List<AstNode> items;
	public ProgramNode(List<AstNode> items) { this.items = items; }
	@Override protected void buildTree(StringBuilder sb, int indent) {
		indent(sb, indent); sb.append("Program\n");
		for (AstNode it : items) it.buildTree(sb, indent + 1);
	}
}

// Declaración simple: name = expr
class DeclNode extends AstNode {
	public final String name;
	public final AstNode expr;
	public DeclNode(String name, AstNode expr) { this.name = name; this.expr = expr; }
	@Override protected void buildTree(StringBuilder sb, int indent) {
		indent(sb, indent); sb.append("Decl: ").append(name).append("\n");
		expr.buildTree(sb, indent + 1);
	}
}

// Identificador
class IdentifierNode extends AstNode {
	public final String name;
	public IdentifierNode(String name) { this.name = name; }
	@Override protected void buildTree(StringBuilder sb, int indent) {
		indent(sb, indent); sb.append("Ident: ").append(name).append("\n");
	}
}

// Literales (string, int, float, char, bool)
class LiteralNode extends AstNode {
	public final Token token;
	public LiteralNode(Token token) { this.token = token; }
	@Override protected void buildTree(StringBuilder sb, int indent) {
		indent(sb, indent); sb.append("Literal(").append(token.getType()).append("): ").append(token.getValue()).append("\n");
	}
}

// If expression
class IfNode extends AstNode {
	public final AstNode cond, thenBranch, elseBranch;
	public IfNode(AstNode c, AstNode t, AstNode e) { cond = c; thenBranch = t; elseBranch = e; }
	@Override protected void buildTree(StringBuilder sb, int indent) {
		indent(sb, indent); sb.append("If\n");
		cond.buildTree(sb, indent + 1);
		indent(sb, indent+1); sb.append("Then\n");
		thenBranch.buildTree(sb, indent + 2);
		indent(sb, indent+1); sb.append("Else\n");
		elseBranch.buildTree(sb, indent + 2);
	}
}

// Let expression: let name = bound in body
class LetNode extends AstNode {
	public final String name;
	public final AstNode bound, body;
	public LetNode(String name, AstNode bound, AstNode body) { this.name = name; this.bound = bound; this.body = body; }
	@Override protected void buildTree(StringBuilder sb, int indent) {
		indent(sb, indent); sb.append("Let ").append(name).append("\n");
		bound.buildTree(sb, indent + 1);
		indent(sb, indent+1); sb.append("In\n");
		body.buildTree(sb, indent + 2);
	}
}

// Aplicación (función aplicada a argumentos)
class ApplyNode extends AstNode {
	public final AstNode function;
	public final List<AstNode> args;
	public ApplyNode(AstNode fn, List<AstNode> args) { this.function = fn; this.args = args; }
	@Override protected void buildTree(StringBuilder sb, int indent) {
		indent(sb, indent); sb.append("Apply\n");
		function.buildTree(sb, indent + 1);
		for (AstNode a : args) a.buildTree(sb, indent + 1);
	}
}

// Binary operator: left op right
class BinaryOpNode extends AstNode {
	public final String op;
	public final AstNode left, right;
	public BinaryOpNode(String op, AstNode l, AstNode r) { this.op = op; left = l; right = r; }
	@Override protected void buildTree(StringBuilder sb, int indent) {
		indent(sb, indent); sb.append("BinaryOp(").append(op).append(")\n");
		left.buildTree(sb, indent + 1);
		right.buildTree(sb, indent + 1);
	}
}

// List node
class ListNode extends AstNode {
	public final List<AstNode> elements;
	public ListNode(List<AstNode> elements) { this.elements = elements; }
	@Override protected void buildTree(StringBuilder sb, int indent) {
		indent(sb, indent); sb.append("List\n");
		for (AstNode e : elements) e.buildTree(sb, indent + 1);
	}
}

// Tuple node
class TupleNode extends AstNode {
	public final List<AstNode> elements;
	public TupleNode(List<AstNode> elements) { this.elements = elements; }
	@Override protected void buildTree(StringBuilder sb, int indent) {
		indent(sb, indent); sb.append("Tuple\n");
		for (AstNode e : elements) e.buildTree(sb, indent + 1);
	}
}

// Unified CycleNode (WHILE, FOR, LOOP)
class CycleNode extends AstNode {
	public enum CycleType { WHILE, FOR, LOOP }

	public final CycleType type;
	public final AstNode init;       // for: initialization (may be null)
	public final AstNode condition;  // condition expression
	public final AstNode update;     // for: increment/update expression (may be null)
	public final List<AstNode> body; // statements inside block

	// WHILE/LOOP: init and update are null; FOR: may provide init/update
	public CycleNode(CycleType type, AstNode init, AstNode condition, AstNode update, List<AstNode> body) {
		this.type = type;
		this.init = init;
		this.condition = condition;
		this.update = update;
		this.body = body == null ? Collections.emptyList() : body;
	}

	@Override protected void buildTree(StringBuilder sb, int indent) {
		indent(sb, indent); sb.append("Cycle(").append(type).append(")\n");
		if (init != null) {
			indent(sb, indent + 1); sb.append("Init\n");
			init.buildTree(sb, indent + 2);
		}
		if (condition != null) {
			indent(sb, indent + 1); sb.append("Condition\n");
			condition.buildTree(sb, indent + 2);
		}
		if (update != null) {
			indent(sb, indent + 1); sb.append("Update\n");
			update.buildTree(sb, indent + 2);
		}
		indent(sb, indent + 1); sb.append("Body\n");
		for (AstNode stmt : body) stmt.buildTree(sb, indent + 2);
	}
}
