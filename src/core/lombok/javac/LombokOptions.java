/*
 * Copyright (C) 2010 The Project Lombok Authors.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok.javac;

import java.util.HashSet;
import java.util.Set;

import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;

public class LombokOptions extends Options {
	private boolean deleteLombokAnnotations = true;
	private final Set<JCCompilationUnit> changed = new HashSet<JCCompilationUnit>();
	
	public static LombokOptions replaceWithDelombokOptions(Context context) {
		Options options = Options.instance(context);
		context.put(optionsKey, (Options)null);
		LombokOptions result = new LombokOptions(context);
		result.putAll(options);
		return result;
	}
	
	public boolean isChanged(JCCompilationUnit ast) {
		return changed.contains(ast);
	}
	
	public static void markChanged(Context context, JCCompilationUnit ast) {
		Options options = context.get(Options.optionsKey);
		if (options instanceof LombokOptions) ((LombokOptions) options).changed.add(ast);
	}
	
	public static boolean shouldDeleteLombokAnnotations(Context context) {
		Options options = context.get(Options.optionsKey);
		return (options instanceof LombokOptions) && ((LombokOptions) options).deleteLombokAnnotations;
	}
	
	private LombokOptions(Context context) {
		super(context);
	}
}
