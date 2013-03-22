/*
 * Copyright 1999-2006 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/*
 * Code derived from com.sun.tools.javac.tree.Pretty, from the langtools project.
 * A version can be found at, for example, http://hg.openjdk.java.net/jdk7/build/langtools
 */
package lombok.delombok;

import static com.sun.tools.javac.code.Flags.ANNOTATION;
import static com.sun.tools.javac.code.Flags.ENUM;
import static com.sun.tools.javac.code.Flags.INTERFACE;
import static com.sun.tools.javac.code.Flags.SYNTHETIC;
import static com.sun.tools.javac.code.Flags.StandardFlags;
import static com.sun.tools.javac.code.Flags.VARARGS;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import lombok.javac.CommentInfo;
import lombok.javac.Javac;
import lombok.javac.CommentInfo.EndConnection;
import lombok.javac.CommentInfo.StartConnection;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCArrayTypeTree;
import com.sun.tools.javac.tree.JCTree.JCAssert;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCContinue;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCErroneous;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCInstanceOf;
import com.sun.tools.javac.tree.JCTree.JCLabeledStatement;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCParens;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCSkip;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.JCTree.JCSynchronized;
import com.sun.tools.javac.tree.JCTree.JCThrow;
import com.sun.tools.javac.tree.JCTree.JCTry;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCWildcard;
import com.sun.tools.javac.tree.JCTree.LetExpr;
import com.sun.tools.javac.tree.JCTree.TypeBoundKind;
import com.sun.tools.javac.util.Convert;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Position;

/** Prints out a tree as an indented Java source program.
 *
 *  <p><b>This is NOT part of any API supported by Sun Microsystems.  If
 *  you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
@SuppressWarnings("all") // Mainly sun code that has other warning settings
public class PrettyCommentsPrinter extends JCTree.Visitor {

    private static final Method GET_TAG_METHOD;
    private static final Field TAG_FIELD; 
    
    private static final int PARENS = Javac.getCtcInt(JCTree.class, "PARENS");
    private static final int IMPORT = Javac.getCtcInt(JCTree.class, "IMPORT");
    private static final int VARDEF = Javac.getCtcInt(JCTree.class, "VARDEF");
    private static final int SELECT = Javac.getCtcInt(JCTree.class, "SELECT");

    private static final Map<Integer, String> OPERATORS;
    
    static {
    	Method m = null;
    	Field f = null;
    	try {
    		m = JCTree.class.getDeclaredMethod("getTag");
		} 
		catch (NoSuchMethodException e) {
			try {
				f = JCTree.class.getDeclaredField("tag");
			} 
			catch (NoSuchFieldException e1) {
				e1.printStackTrace();
			}
		}
		GET_TAG_METHOD = m;
		TAG_FIELD = f;
		
		Map<Integer, String> map = new HashMap<Integer, String>();
		
        map.put(Javac.getCtcInt(JCTree.class, "POS"), "+");
        map.put(Javac.getCtcInt(JCTree.class, "NEG"), "-");
        map.put(Javac.getCtcInt(JCTree.class, "NOT"), "!");
        map.put(Javac.getCtcInt(JCTree.class, "COMPL"), "~");
        map.put(Javac.getCtcInt(JCTree.class, "PREINC"), "++");
        map.put(Javac.getCtcInt(JCTree.class, "PREDEC"), "--");
        map.put(Javac.getCtcInt(JCTree.class, "POSTINC"), "++");
        map.put(Javac.getCtcInt(JCTree.class, "POSTDEC"), "--");
        map.put(Javac.getCtcInt(JCTree.class, "NULLCHK"), "<*nullchk*>");
        map.put(Javac.getCtcInt(JCTree.class, "OR"), "||");
        map.put(Javac.getCtcInt(JCTree.class, "AND"), "&&");
        map.put(Javac.getCtcInt(JCTree.class, "EQ"), "==");
        map.put(Javac.getCtcInt(JCTree.class, "NE"), "!=");
        map.put(Javac.getCtcInt(JCTree.class, "LT"), "<");
        map.put(Javac.getCtcInt(JCTree.class, "GT"), ">");
        map.put(Javac.getCtcInt(JCTree.class, "LE"), "<=");
        map.put(Javac.getCtcInt(JCTree.class, "GE"), ">=");
        map.put(Javac.getCtcInt(JCTree.class, "BITOR"), "|");
        map.put(Javac.getCtcInt(JCTree.class, "BITXOR"), "^");
        map.put(Javac.getCtcInt(JCTree.class, "BITAND"), "&");
        map.put(Javac.getCtcInt(JCTree.class, "SL"), "<<");
        map.put(Javac.getCtcInt(JCTree.class, "SR"), ">>");
        map.put(Javac.getCtcInt(JCTree.class, "USR"), ">>>");
        map.put(Javac.getCtcInt(JCTree.class, "PLUS"), "+");
        map.put(Javac.getCtcInt(JCTree.class, "MINUS"), "-");
        map.put(Javac.getCtcInt(JCTree.class, "MUL"), "*");
        map.put(Javac.getCtcInt(JCTree.class, "DIV"), "/");
        map.put(Javac.getCtcInt(JCTree.class, "MOD"), "%");
		
		OPERATORS = map;
    }
    
    static int getTag(JCTree tree) {
    	if (GET_TAG_METHOD != null) {
    		try {
				return (Integer)GET_TAG_METHOD.invoke(tree);
			} 
    		catch (IllegalAccessException e) {
    			throw new RuntimeException(e);
			} 
    		catch (InvocationTargetException e) {
    			throw new RuntimeException(e.getCause());
			} 
    	}
    	try {
			return TAG_FIELD.getInt(tree);
		} 
    	catch (IllegalAccessException e) {
    		throw new RuntimeException(e);
		}
    }
	
	private List<CommentInfo> comments;
	private final JCCompilationUnit cu;
	private boolean onNewLine = true;
	private boolean aligned = false;
	private boolean inParams = false;
	
	private boolean needsSpace = false;
	private boolean needsNewLine = false;
	private boolean needsAlign = false;
	
    public PrettyCommentsPrinter(Writer out, JCCompilationUnit cu, List<CommentInfo> comments) {
        this.out = out;
		this.comments = comments;
		this.cu = cu;
    }

	private int endPos(JCTree tree) {
		return tree.getEndPosition(cu.endPositions);
	}
    
    private void consumeComments(int till) throws IOException {
    	boolean prevNewLine = onNewLine;
    	boolean found = false;
    	CommentInfo head = comments.head;
		while (comments.nonEmpty() && head.pos < till) {
			printComment(head);
			comments = comments.tail;
			head = comments.head;
		}
		if (!onNewLine && prevNewLine) {
			println();
		}
	}

    private void consumeTrailingComments(int from) throws IOException {
    	boolean prevNewLine = onNewLine;
		CommentInfo head = comments.head;
		boolean stop = false;
		while (comments.nonEmpty() && head.prevEndPos == from && !stop && !(head.start == StartConnection.ON_NEXT_LINE || head.start == StartConnection.START_OF_LINE)) {
			from = head.endPos;
			printComment(head);
			stop = (head.end == EndConnection.ON_NEXT_LINE);
			comments = comments.tail;
			head = comments.head;
		}
		if (!onNewLine && prevNewLine) {
			println();
		}
	}

	private void printComment(CommentInfo comment) throws IOException {
    	prepareComment(comment.start);
    	print(comment.content);
    	switch (comment.end) {
		case ON_NEXT_LINE:
			if (!aligned) {
				needsNewLine = true;
				needsAlign = true;
			}
			break;
		case AFTER_COMMENT:
			needsSpace = true;
			break;
		case DIRECT_AFTER_COMMENT:
			// do nothing
			break;
		}
    }

	private void prepareComment(StartConnection start) throws IOException {
		switch (start) {
			case DIRECT_AFTER_PREVIOUS:
				needsSpace = false;
				break;
			case AFTER_PREVIOUS:
				needsSpace = true;
				break;
			case START_OF_LINE:
				needsNewLine = true;
				needsAlign = false;
				break;
			case ON_NEXT_LINE:
				if (!aligned) {
					needsNewLine = true;
					needsAlign = true;
				}
				break;
		}
	}
    
    /** The output stream on which trees are printed.
     */
    Writer out;

    /** The current left margin.
     */
    int lmargin = 0;

    /** The enclosing class name.
     */
    Name enclClassName;

    /** A hashtable mapping trees to their documentation comments
     *  (can be null)
     */
    Map<JCTree, String> docComments = null;
    
    /** Align code to be indented to left margin.
     */
    void align() throws IOException {
    	onNewLine = false;
    	aligned = true;
    	needsAlign = false;
        for (int i = 0; i < lmargin; i++) out.write("\t");
    }

    /** Increase left margin by indentation width.
     */
    void indent() {
        lmargin++;
    }

    /** Decrease left margin by indentation width.
     */
    void undent() {
        lmargin--;
    }

    /** Enter a new precedence level. Emit a `(' if new precedence level
     *  is less than precedence level so far.
     *  @param contextPrec    The precedence level in force so far.
     *  @param ownPrec        The new precedence level.
     */
    void open(int contextPrec, int ownPrec) throws IOException {
        if (ownPrec < contextPrec) out.write("(");
    }

    /** Leave precedence level. Emit a `(' if inner precedence level
     *  is less than precedence level we revert to.
     *  @param contextPrec    The precedence level we revert to.
     *  @param ownPrec        The inner precedence level.
     */
    void close(int contextPrec, int ownPrec) throws IOException {
        if (ownPrec < contextPrec) out.write(")");
    }

    /** Print string, replacing all non-ascii character with unicode escapes.
     */
    public void print(Object s) throws IOException {
    	boolean align = needsAlign;
    	if (needsNewLine && !onNewLine) {
    		println();
    	}
    	if (align && !aligned) {
    		align();
    	}
    	if (needsSpace && !onNewLine && !aligned) {
    		out.write(' ');
    	}
    	needsSpace = false;
        
    	out.write(Convert.escapeUnicode(s.toString()));
       
        onNewLine = false;
        aligned = false;
    }

    /** Print new line.
     */
    public void println() throws IOException {
    	onNewLine = true;
    	aligned = false;
    	needsNewLine = false;
        out.write(lineSep);
    }

    String lineSep = System.getProperty("line.separator");

    /**************************************************************************
     * Traversal methods
     *************************************************************************/

    /** Exception to propagate IOException through visitXXX methods */
    private static class UncheckedIOException extends Error {
        static final long serialVersionUID = -4032692679158424751L;
        UncheckedIOException(IOException e) {
            super(e.getMessage(), e);
        }
    }

    /** Visitor argument: the current precedence level.
     */
    int prec;

    /** Visitor method: print expression tree.
     *  @param prec  The current precedence level.
     */
    public void printExpr(JCTree tree, int prec) throws IOException {

        int prevPrec = this.prec;
        try {
            this.prec = prec;
            if (tree == null) print("/*missing*/");
            else {
            	consumeComments(tree.pos);
               	tree.accept(this);
               	int endPos = endPos(tree);
				consumeTrailingComments(endPos);
            }
        } catch (UncheckedIOException ex) {
            IOException e = new IOException(ex.getMessage());
            e.initCause(ex);
            throw e;
        } finally {
        	this.prec = prevPrec;
        }
    }

    /** Derived visitor method: print expression tree at minimum precedence level
     *  for expression.
     */
    public void printExpr(JCTree tree) throws IOException {
        printExpr(tree, TreeInfo.noPrec);
    }

    /** Derived visitor method: print statement tree.
     */
    public void printStat(JCTree tree) throws IOException {
        if (isEmptyStat(tree)) {
            // printEmptyStat(); // -- starting in java 7, these get lost, so to be consistent, we never print them.
        } else {
            printExpr(tree, TreeInfo.notExpression);
        }
    }
    
    public void printEmptyStat() throws IOException {
        print(";");
    }

    public boolean isEmptyStat(JCTree tree) {
        if (!(tree instanceof JCBlock)) return false;
        JCBlock block = (JCBlock) tree;
        return (Position.NOPOS == block.pos) && block.stats.isEmpty();
    }

    /** Derived visitor method: print list of expression trees, separated by given string.
     *  @param sep the separator string
     */
    public <T extends JCTree> void printExprs(List<T> trees, String sep) throws IOException {
        if (trees.nonEmpty()) {
            printExpr(trees.head);
            for (List<T> l = trees.tail; l.nonEmpty(); l = l.tail) {
                print(sep);
                printExpr(l.head);
            }
        }
    }

    /** Derived visitor method: print list of expression trees, separated by commas.
     */
    public <T extends JCTree> void printExprs(List<T> trees) throws IOException {
        printExprs(trees, ", ");
    }

    /** Derived visitor method: print list of statements, each on a separate line.
     */
    public void printStats(List<? extends JCTree> trees) throws IOException {
        for (List<? extends JCTree> l = trees; l.nonEmpty(); l = l.tail) {
            align();
            printStat(l.head);
            println();
        }
    }

    /** Print a set of modifiers.
     */
    public void printFlags(long flags) throws IOException {
        if ((flags & SYNTHETIC) != 0) print("/*synthetic*/ ");
        print(TreeInfo.flagNames(flags));
        if ((flags & StandardFlags) != 0) print(" ");
        if ((flags & ANNOTATION) != 0) print("@");
    }

    public void printAnnotations(List<JCAnnotation> trees) throws IOException {
        for (List<JCAnnotation> l = trees; l.nonEmpty(); l = l.tail) {
            printStat(l.head);
            if (inParams) { 
            	print(" ");
            }
            else {
            	println();
            	align();
            }
        }
    }

    /** Print documentation comment, if it exists
     *  @param tree    The tree for which a documentation comment should be printed.
     */
    public void printDocComment(JCTree tree) throws IOException {
        if (docComments != null) {
            String dc = docComments.get(tree);
            if (dc != null) {
                print("/**"); println();
                int pos = 0;
                int endpos = lineEndPos(dc, pos);
                while (pos < dc.length()) {
                    align();
                    print(" *");
                    if (pos < dc.length() && dc.charAt(pos) > ' ') print(" ");
                    print(dc.substring(pos, endpos)); println();
                    pos = endpos + 1;
                    endpos = lineEndPos(dc, pos);
                }
                align(); print(" */"); println();
                align();
            }
        }
    }
//where
    static int lineEndPos(String s, int start) {
        int pos = s.indexOf('\n', start);
        if (pos < 0) pos = s.length();
        return pos;
    }

    /** If type parameter list is non-empty, print it enclosed in "<...>" brackets.
     */
    public void printTypeParameters(List<JCTypeParameter> trees) throws IOException {
        if (trees.nonEmpty()) {
            print("<");
            printExprs(trees);
            print(">");
        }
    }

    /** Print a block.
     */
    public void printBlock(List<? extends JCTree> stats, JCTree container) throws IOException {
        print("{");
        println();
        indent();
        printStats(stats);
        consumeComments(endPos(container));
        undent();
        align();
        print("}");
    }

    /** Print a block.
     */
    public void printEnumBody(List<JCTree> stats) throws IOException {
        print("{");
        println();
        indent();
        boolean first = true;
        for (List<JCTree> l = stats; l.nonEmpty(); l = l.tail) {
            if (isEnumerator(l.head)) {
                if (!first) {
                    print(",");
                    println();
                }
                align();
                printStat(l.head);
                first = false;
            }
        }
        print(";");
        println();
        int x = 0;
        for (List<JCTree> l = stats; l.nonEmpty(); l = l.tail) {
        	x++;
            if (!isEnumerator(l.head)) {
                align();
                printStat(l.head);
                println();
            }
        }
        undent();
        align();
        print("}");
    }

    public void printEnumMember(JCVariableDecl tree) throws IOException {
        printAnnotations(tree.mods.annotations);
        print(tree.name);
        if (tree.init instanceof JCNewClass) {
            JCNewClass constructor = (JCNewClass) tree.init;
            if (constructor.args != null && constructor.args.nonEmpty()) {
                print("(");
                printExprs(constructor.args);
                print(")");
            }
            if (constructor.def != null && constructor.def.defs != null) {
                print(" ");
                printBlock(constructor.def.defs, constructor.def);
            }
        }
    }

    /** Is the given tree an enumerator definition? */
    boolean isEnumerator(JCTree t) {
        return getTag(t) == VARDEF && (((JCVariableDecl) t).mods.flags & ENUM) != 0;
    }

    /** Print unit consisting of package clause and import statements in toplevel,
     *  followed by class definition. if class definition == null,
     *  print all definitions in toplevel.
     *  @param tree     The toplevel tree
     *  @param cdef     The class definition, which is assumed to be part of the
     *                  toplevel tree.
     */
    public void printUnit(JCCompilationUnit tree, JCClassDecl cdef) throws IOException {
        docComments = tree.docComments;
        printDocComment(tree);
        if (tree.pid != null) {
            consumeComments(tree.pos);
            print("package ");
            printExpr(tree.pid);
            print(";");
            println();
        }
        boolean firstImport = true;
        for (List<JCTree> l = tree.defs;
        l.nonEmpty() && (cdef == null || getTag(l.head) == IMPORT);
        l = l.tail) {
            if (getTag(l.head) == IMPORT) {
                JCImport imp = (JCImport)l.head;
                Name name = TreeInfo.name(imp.qualid);
                if (name == name.table.fromChars(new char[] {'*'}, 0, 1) ||
                        cdef == null ||
                        isUsed(TreeInfo.symbol(imp.qualid), cdef)) {
                    if (firstImport) {
                        firstImport = false;
                        println();
                    }
                    printStat(imp);
                }
            } else {
                printStat(l.head);
            }
        }
        if (cdef != null) {
            printStat(cdef);
            println();
        }
    }
    // where
    boolean isUsed(final Symbol t, JCTree cdef) {
        class UsedVisitor extends TreeScanner {
            public void scan(JCTree tree) {
                if (tree!=null && !result) tree.accept(this);
            }
            boolean result = false;
            public void visitIdent(JCIdent tree) {
                if (tree.sym == t) result = true;
            }
        }
        UsedVisitor v = new UsedVisitor();
        v.scan(cdef);
        return v.result;
    }

    /**************************************************************************
     * Visitor methods
     *************************************************************************/

    public void visitTopLevel(JCCompilationUnit tree) {
        try {
            printUnit(tree, null);
            consumeComments(Integer.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitImport(JCImport tree) {
        try {
            print("import ");
            if (tree.staticImport) print("static ");
            printExpr(tree.qualid);
            print(";");
            println();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitClassDef(JCClassDecl tree) {
        try {
        	consumeComments(tree.pos);
            println(); align();
            printDocComment(tree);
            printAnnotations(tree.mods.annotations);
            printFlags(tree.mods.flags & ~INTERFACE);
            Name enclClassNamePrev = enclClassName;
            enclClassName = tree.name;
            if ((tree.mods.flags & INTERFACE) != 0) {
                print("interface " + tree.name);
                printTypeParameters(tree.typarams);
                if (tree.implementing.nonEmpty()) {
                    print(" extends ");
                    printExprs(tree.implementing);
                }
            } else {
                if ((tree.mods.flags & ENUM) != 0)
                    print("enum " + tree.name);
                else
                    print("class " + tree.name);
                printTypeParameters(tree.typarams);
                if (tree.getExtendsClause() != null) {
                    print(" extends ");
                    printExpr(tree.getExtendsClause());
                }
                if (tree.implementing.nonEmpty()) {
                    print(" implements ");
                    printExprs(tree.implementing);
                }
            }
            print(" ");
            // <Added for delombok by Reinier Zwitserloot>
            if ((tree.mods.flags & INTERFACE) != 0) {
                removeImplicitModifiersForInterfaceMembers(tree.defs);
            }
            // </Added for delombok by Reinier Zwitserloot>
            if ((tree.mods.flags & ENUM) != 0) {
                printEnumBody(tree.defs);
            } else {
                printBlock(tree.defs, tree);
            }
            enclClassName = enclClassNamePrev;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Added for delombok by Reinier Zwitserloot
    private void removeImplicitModifiersForInterfaceMembers(List<JCTree> defs) {
        for (JCTree def :defs) {
            if (def instanceof JCVariableDecl) {
                ((JCVariableDecl) def).mods.flags &= ~(Flags.PUBLIC | Flags.STATIC | Flags.FINAL);
            }
            if (def instanceof JCMethodDecl) {
                ((JCMethodDecl) def).mods.flags &= ~(Flags.PUBLIC | Flags.ABSTRACT);
            }
            if (def instanceof JCClassDecl) {
                ((JCClassDecl) def).mods.flags &= ~(Flags.PUBLIC | Flags.STATIC);
            }
        }
    }
    
    public void visitMethodDef(JCMethodDecl tree) {
        try {
            boolean isConstructor = tree.name == tree.name.table.fromChars("<init>".toCharArray(), 0, 6);
            // when producing source output, omit anonymous constructors
            if (isConstructor && enclClassName == null) return;
            boolean isGeneratedConstructor = isConstructor && ((tree.mods.flags & Flags.GENERATEDCONSTR) != 0);
            if (isGeneratedConstructor) return;
            println(); align();
            printDocComment(tree);
            printExpr(tree.mods);
            printTypeParameters(tree.typarams);
            if (tree.typarams != null && tree.typarams.length() > 0) print(" ");
            if (tree.name == tree.name.table.fromChars("<init>".toCharArray(), 0, 6)) {
                print(enclClassName != null ? enclClassName : tree.name);
            } else {
                printExpr(tree.restype);
                print(" " + tree.name);
            }
            print("(");
            inParams = true;
            printExprs(tree.params);
            inParams = false;
            print(")");
            if (tree.thrown.nonEmpty()) {
                print(" throws ");
                printExprs(tree.thrown);
            }
            if (tree.defaultValue != null) {
              print(" default ");
              print(tree.defaultValue);
            }
            if (tree.body != null) {
                print(" ");
                printBlock(tree.body.stats, tree.body);
            } else {
                print(";");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitVarDef(JCVariableDecl tree) {
        try {
            if (docComments != null && docComments.get(tree) != null) {
                println(); align();
            }
            printDocComment(tree);
            if ((tree.mods.flags & ENUM) != 0) {
                printEnumMember(tree);
            } else {
                printExpr(tree.mods);
                if ((tree.mods.flags & VARARGS) != 0) {
                    printExpr(((JCArrayTypeTree) tree.vartype).elemtype);
                    print("... " + tree.name);
                } else {
                    printExpr(tree.vartype);
                    print(" " + tree.name);
                }
                if (tree.init != null) {
                    print(" = ");
                    printExpr(tree.init);
                }
                if (prec == TreeInfo.notExpression) print(";");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitSkip(JCSkip tree) {
        try {
            print(";");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitBlock(JCBlock tree) {
        try {
        	consumeComments(tree.pos);
            printFlags(tree.flags);
            printBlock(tree.stats, tree);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitDoLoop(JCDoWhileLoop tree) {
        try {
            print("do ");
            printStat(tree.body);
            align();
            print(" while ");
            if (getTag(tree.cond) == PARENS) {
                printExpr(tree.cond);
            } else {
                print("(");
                printExpr(tree.cond);
                print(")");
            }
            print(";");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitWhileLoop(JCWhileLoop tree) {
        try {
            print("while ");
            if (getTag(tree.cond) == PARENS) {
                printExpr(tree.cond);
            } else {
                print("(");
                printExpr(tree.cond);
                print(")");
            }
            print(" ");
            printStat(tree.body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitForLoop(JCForLoop tree) {
        try {
            print("for (");
            if (tree.init.nonEmpty()) {
                if (getTag(tree.init.head) == VARDEF) {
                    printExpr(tree.init.head);
                    for (List<JCStatement> l = tree.init.tail; l.nonEmpty(); l = l.tail) {
                        JCVariableDecl vdef = (JCVariableDecl)l.head;
                        print(", " + vdef.name + " = ");
                        printExpr(vdef.init);
                    }
                } else {
                    printExprs(tree.init);
                }
            }
            print("; ");
            if (tree.cond != null) printExpr(tree.cond);
            print("; ");
            printExprs(tree.step);
            print(") ");
            printStat(tree.body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitForeachLoop(JCEnhancedForLoop tree) {
        try {
            print("for (");
            printExpr(tree.var);
            print(" : ");
            printExpr(tree.expr);
            print(") ");
            printStat(tree.body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitLabelled(JCLabeledStatement tree) {
        try {
            print(tree.label + ": ");
            printStat(tree.body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitSwitch(JCSwitch tree) {
        try {
            print("switch ");
            if (getTag(tree.selector) == PARENS) {
                printExpr(tree.selector);
            } else {
                print("(");
                printExpr(tree.selector);
                print(")");
            }
            print(" {");
            println();
            printStats(tree.cases);
            align();
            print("}");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitCase(JCCase tree) {
        try {
            if (tree.pat == null) {
                print("default");
            } else {
                print("case ");
                printExpr(tree.pat);
            }
            print(": ");
            println();
            indent();
            printStats(tree.stats);
            undent();
            align();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitSynchronized(JCSynchronized tree) {
        try {
            print("synchronized ");
            if (getTag(tree.lock) == PARENS) {
                printExpr(tree.lock);
            } else {
                print("(");
                printExpr(tree.lock);
                print(")");
            }
            print(" ");
            printStat(tree.body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitTry(JCTry tree) {
        try {
            print("try ");
            List<?> resources = null;
            try {
                Field f = JCTry.class.getField("resources");
                resources = (List<?>) f.get(tree);
            } catch (Exception ignore) {
                // In JDK6 and down this field does not exist; resources will retain its initializer value which is what we want.
            }
            
            if (resources != null && resources.nonEmpty()) {
                boolean first = true;
                print("(");
                for (Object var0 : resources) {
                    JCTree var = (JCTree) var0;
                    if (!first) {
                        println();
                        indent();
                    }
                    printStat(var);
                    first = false;
                }
                print(") ");
            }
            
            printStat(tree.body);
            for (List<JCCatch> l = tree.catchers; l.nonEmpty(); l = l.tail) {
                printStat(l.head);
            }
            if (tree.finalizer != null) {
                print(" finally ");
                printStat(tree.finalizer);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitCatch(JCCatch tree) {
        try {
            print(" catch (");
            printExpr(tree.param);
            print(") ");
            printStat(tree.body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitConditional(JCConditional tree) {
        try {
            open(prec, TreeInfo.condPrec);
            printExpr(tree.cond, TreeInfo.condPrec);
            print(" ? ");
            printExpr(tree.truepart, TreeInfo.condPrec);
            print(" : ");
            printExpr(tree.falsepart, TreeInfo.condPrec);
            close(prec, TreeInfo.condPrec);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitIf(JCIf tree) {
        try {
            print("if ");
            if (getTag(tree.cond) == PARENS) {
                printExpr(tree.cond);
            } else {
                print("(");
                printExpr(tree.cond);
                print(")");
            }
            print(" ");
            printStat(tree.thenpart);
            if (tree.elsepart != null) {
                print(" else ");
                printStat(tree.elsepart);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    private boolean isNoArgsSuperCall(JCExpression expr) {
        if (!(expr instanceof JCMethodInvocation)) return false;
        JCMethodInvocation tree = (JCMethodInvocation) expr;
        if (!tree.typeargs.isEmpty() || !tree.args.isEmpty()) return false;
        if (!(tree.meth instanceof JCIdent)) return false;
        return ((JCIdent) tree.meth).name.toString().equals("super");
    }
    
    public void visitExec(JCExpressionStatement tree) {
        if (isNoArgsSuperCall(tree.expr)) return;
        try {
            printExpr(tree.expr);
            if (prec == TreeInfo.notExpression) print(";");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitBreak(JCBreak tree) {
        try {
            print("break");
            if (tree.label != null) print(" " + tree.label);
            print(";");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitContinue(JCContinue tree) {
        try {
            print("continue");
            if (tree.label != null) print(" " + tree.label);
            print(";");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitReturn(JCReturn tree) {
        try {
            print("return");
            if (tree.expr != null) {
                print(" ");
                printExpr(tree.expr);
            }
            print(";");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitThrow(JCThrow tree) {
        try {
            print("throw ");
            printExpr(tree.expr);
            print(";");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitAssert(JCAssert tree) {
        try {
            print("assert ");
            printExpr(tree.cond);
            if (tree.detail != null) {
                print(" : ");
                printExpr(tree.detail);
            }
            print(";");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitApply(JCMethodInvocation tree) {
        try {
            if (!tree.typeargs.isEmpty()) {
                if (getTag(tree.meth) == SELECT) {
                    JCFieldAccess left = (JCFieldAccess)tree.meth;
                    printExpr(left.selected);
                    print(".<");
                    printExprs(tree.typeargs);
                    print(">" + left.name);
                } else {
                    print("<");
                    printExprs(tree.typeargs);
                    print(">");
                    printExpr(tree.meth);
                }
            } else {
                printExpr(tree.meth);
            }
            print("(");
            printExprs(tree.args);
            print(")");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitNewClass(JCNewClass tree) {
        try {
            if (tree.encl != null) {
                printExpr(tree.encl);
                print(".");
            }
            print("new ");
            if (!tree.typeargs.isEmpty()) {
                print("<");
                printExprs(tree.typeargs);
                print(">");
            }
            printExpr(tree.clazz);
            print("(");
            printExprs(tree.args);
            print(")");
            if (tree.def != null) {
                Name enclClassNamePrev = enclClassName;
                enclClassName =
                        tree.def.name != null ? tree.def.name :
                            tree.type != null && tree.type.tsym.name != tree.type.tsym.name.table.fromChars(new char[0], 0, 0) ? tree.type.tsym.name :
                                null;
                if ((tree.def.mods.flags & Flags.ENUM) != 0) print("/*enum*/");
                printBlock(tree.def.defs, tree.def);
                enclClassName = enclClassNamePrev;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitNewArray(JCNewArray tree) {
        try {
            if (tree.elemtype != null) {
                print("new ");
                JCTree elem = tree.elemtype;
                if (elem instanceof JCArrayTypeTree)
                    printBaseElementType((JCArrayTypeTree) elem);
                else
                    printExpr(elem);
                for (List<JCExpression> l = tree.dims; l.nonEmpty(); l = l.tail) {
                    print("[");
                    printExpr(l.head);
                    print("]");
                }
                if (elem instanceof JCArrayTypeTree)
                    printBrackets((JCArrayTypeTree) elem);
            }
            if (tree.elems != null) {
                if (tree.elemtype != null) print("[]");
                print("{");
                printExprs(tree.elems);
                print("}");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitParens(JCParens tree) {
        try {
            print("(");
            printExpr(tree.expr);
            print(")");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitAssign(JCAssign tree) {
        try {
            open(prec, TreeInfo.assignPrec);
            printExpr(tree.lhs, TreeInfo.assignPrec + 1);
            print(" = ");
            printExpr(tree.rhs, TreeInfo.assignPrec);
            close(prec, TreeInfo.assignPrec);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String operatorName(int tag) {
        String result = OPERATORS.get(tag);
        if (result == null) throw new Error();
        return result;
    }

    public void visitAssignop(JCAssignOp tree) {
        try {
            open(prec, TreeInfo.assignopPrec);
            printExpr(tree.lhs, TreeInfo.assignopPrec + 1);
            print(" " + operatorName(getTag(tree) - JCTree.ASGOffset) + "= ");
            printExpr(tree.rhs, TreeInfo.assignopPrec);
            close(prec, TreeInfo.assignopPrec);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitUnary(JCUnary tree) {
        try {
            int ownprec = TreeInfo.opPrec(getTag(tree));
            String opname = operatorName(getTag(tree));
            open(prec, ownprec);
            if (getTag(tree) <= Javac.getCtcInt(JCTree.class, "PREDEC")) {
                print(opname);
                printExpr(tree.arg, ownprec);
            } else {
                printExpr(tree.arg, ownprec);
                print(opname);
            }
            close(prec, ownprec);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitBinary(JCBinary tree) {
        try {
            int ownprec = TreeInfo.opPrec(getTag(tree));
            String opname = operatorName(getTag(tree));
            open(prec, ownprec);
            printExpr(tree.lhs, ownprec);
            print(" " + opname + " ");
            printExpr(tree.rhs, ownprec + 1);
            close(prec, ownprec);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitTypeCast(JCTypeCast tree) {
        try {
            open(prec, TreeInfo.prefixPrec);
            print("(");
            printExpr(tree.clazz);
            print(")");
            printExpr(tree.expr, TreeInfo.prefixPrec);
            close(prec, TreeInfo.prefixPrec);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitTypeTest(JCInstanceOf tree) {
        try {
            open(prec, TreeInfo.ordPrec);
            printExpr(tree.expr, TreeInfo.ordPrec);
            print(" instanceof ");
            printExpr(tree.clazz, TreeInfo.ordPrec + 1);
            close(prec, TreeInfo.ordPrec);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitIndexed(JCArrayAccess tree) {
        try {
            printExpr(tree.indexed, TreeInfo.postfixPrec);
            print("[");
            printExpr(tree.index);
            print("]");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitSelect(JCFieldAccess tree) {
        try {
            printExpr(tree.selected, TreeInfo.postfixPrec);
            print("." + tree.name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitIdent(JCIdent tree) {
        try {
            print(tree.name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitLiteral(JCLiteral tree) {
        try {
            switch (tree.typetag) {
                case TypeTags.INT:
                    print(tree.value.toString());
                    break;
                case TypeTags.LONG:
                    print(tree.value + "L");
                    break;
                case TypeTags.FLOAT:
                    print(tree.value + "F");
                    break;
                case TypeTags.DOUBLE:
                    print(tree.value.toString());
                    break;
                case TypeTags.CHAR:
                    print("\'" +
                            Convert.quote(
                            String.valueOf((char)((Number)tree.value).intValue())) +
                            "\'");
                    break;
                case TypeTags.BOOLEAN:
                    print(((Number)tree.value).intValue() == 1 ? "true" : "false");
                    break;
                case TypeTags.BOT:
                    print("null");
                    break;
                default:
                    print("\"" + Convert.quote(tree.value.toString()) + "\"");
                    break;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitTypeIdent(JCPrimitiveTypeTree tree) {
        try {
            switch(tree.typetag) {
                case TypeTags.BYTE:
                    print("byte");
                    break;
                case TypeTags.CHAR:
                    print("char");
                    break;
                case TypeTags.SHORT:
                    print("short");
                    break;
                case TypeTags.INT:
                    print("int");
                    break;
                case TypeTags.LONG:
                    print("long");
                    break;
                case TypeTags.FLOAT:
                    print("float");
                    break;
                case TypeTags.DOUBLE:
                    print("double");
                    break;
                case TypeTags.BOOLEAN:
                    print("boolean");
                    break;
                case TypeTags.VOID:
                    print("void");
                    break;
                default:
                    print("error");
                    break;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitTypeArray(JCArrayTypeTree tree) {
        try {
            printBaseElementType(tree);
            printBrackets(tree);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Prints the inner element type of a nested array
    private void printBaseElementType(JCArrayTypeTree tree) throws IOException {
        JCTree elem = tree.elemtype;
        while (elem instanceof JCWildcard)
            elem = ((JCWildcard) elem).inner;
        if (elem instanceof JCArrayTypeTree)
            printBaseElementType((JCArrayTypeTree) elem);
        else
            printExpr(elem);
    }

    // prints the brackets of a nested array in reverse order
    private void printBrackets(JCArrayTypeTree tree) throws IOException {
        JCTree elem;
        while (true) {
            elem = tree.elemtype;
            print("[]");
            if (!(elem instanceof JCArrayTypeTree)) break;
            tree = (JCArrayTypeTree) elem;
        }
    }

    public void visitTypeApply(JCTypeApply tree) {
        try {
            printExpr(tree.clazz);
            print("<");
            printExprs(tree.arguments);
            print(">");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitTypeParameter(JCTypeParameter tree) {
        try {
            print(tree.name);
            if (tree.bounds.nonEmpty()) {
                print(" extends ");
                printExprs(tree.bounds, " & ");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void visitWildcard(JCWildcard tree) {
        try {
            Object kind = tree.getClass().getField("kind").get(tree);
            print(kind);
            if (kind != null && kind.getClass().getSimpleName().equals("TypeBoundKind")) {
                kind = kind.getClass().getField("kind").get(kind);
            }
            
            if (tree.getKind() != Tree.Kind.UNBOUNDED_WILDCARD)
                printExpr(tree.inner);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void visitTypeBoundKind(TypeBoundKind tree) {
        try {
            print(String.valueOf(tree.kind));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitErroneous(JCErroneous tree) {
        try {
            print("(ERROR)");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitLetExpr(LetExpr tree) {
        try {
            print("(let " + tree.defs + " in " + tree.expr + ")");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitModifiers(JCModifiers mods) {
        try {
            printAnnotations(mods.annotations);
            printFlags(mods.flags);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitAnnotation(JCAnnotation tree) {
        try {
            print("@");
            printExpr(tree.annotationType);
            if (tree.args.nonEmpty()) {
                print("(");
                if (tree.args.length() == 1 && tree.args.get(0) instanceof JCAssign) {
                     JCExpression lhs = ((JCAssign)tree.args.get(0)).lhs;
                     if (lhs instanceof JCIdent && ((JCIdent)lhs).name.toString().equals("value")) tree.args = List.of(((JCAssign)tree.args.get(0)).rhs);
                }
                printExprs(tree.args);
                print(")");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void visitTree(JCTree tree) {
        try {
            if ("JCTypeUnion".equals(tree.getClass().getSimpleName())) {
                print(tree.toString());
                return;
            } else {
                print("(UNKNOWN: " + tree + ")");
                println();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
