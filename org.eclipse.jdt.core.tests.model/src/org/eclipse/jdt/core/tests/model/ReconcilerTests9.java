/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *		IBM Corporation - initial API and implementation
 *		Stephan Herrmann - Copy-adjusted structure from ReconcilerTests, filled with new content
 *******************************************************************************/
package org.eclipse.jdt.core.tests.model;


import java.io.IOException;
import java.util.Hashtable;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.dom.AST;

import junit.framework.Test;

public class ReconcilerTests9 extends ModifyingResourceTests {

	protected ICompilationUnit workingCopy;
	protected ProblemRequestor problemRequestor;
	
	/*package*/ static final int JLS_LATEST = AST.JLS11;

/**
 */
public ReconcilerTests9(String name) {
	super(name);
}
// Use this static initializer to specify subset for tests
// All specified tests which do not belong to the class are skipped...
static {
//	JavaModelManager.VERBOSE = true;
//	TESTS_PREFIX = "testAnnotations";
//	TESTS_NAMES = new String[] { "testAnnotations2" };
//	TESTS_NUMBERS = new int[] { 118823 };
//	TESTS_RANGE = new int[] { 16, -1 };
}
public static Test suite() {
	return buildModelTestSuite(ReconcilerTests9.class);
}
protected void assertProblems(String message, String expected) {
	assertProblems(message, expected, this.problemRequestor);
}
/**
 * Setup for the next test.
 */
public void setUp() throws Exception {
	super.setUp();
	this.problemRequestor =  new ProblemRequestor();
	this.wcOwner = new WorkingCopyOwner() {
		public IProblemRequestor getProblemRequestor(ICompilationUnit unit) {
			return ReconcilerTests9.this.problemRequestor;
		}
	};
	this.workingCopy = getCompilationUnit("Reconciler9/src/module-info.java").getWorkingCopy(this.wcOwner, null);
	this.problemRequestor.initialize(this.workingCopy.getSource().toCharArray());
	startDeltas();
}
public void setUpSuite() throws Exception {
	super.setUpSuite();

	// Create project with 9 compliance
	IJavaProject project9 = createJava9Project("Reconciler9");
	createFile(
		"/Reconciler9/src/module-info.java",
		"/**\n" +
		"  * @category before" +
		"  */\n" +
		"@Deprecated\n" +
		"module mod.one {\n" +
		"}"
	);
	project9.setOption(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_9);
	project9.setOption(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.IGNORE);
	project9.setOption(JavaCore.COMPILER_PB_INVALID_JAVADOC, JavaCore.WARNING);


}
protected void setUpWorkingCopy(String path, String contents) throws JavaModelException {
	setUpWorkingCopy(path, contents, this.wcOwner);
}
private void setUpWorkingCopy(String path, String contents, WorkingCopyOwner owner) throws JavaModelException {
	this.workingCopy.discardWorkingCopy();
	this.workingCopy = getCompilationUnit(path).getWorkingCopy(owner, null);
	assertEquals("Invalid problem requestor!", this.problemRequestor, this.wcOwner.getProblemRequestor(this.workingCopy));
	setWorkingCopyContents(contents);
	this.workingCopy.makeConsistent(null);
}
void setWorkingCopyContents(String contents) throws JavaModelException {
	this.workingCopy.getBuffer().setContents(contents);
	this.problemRequestor.initialize(contents.toCharArray());
}
/**
 * Cleanup after the previous test.
 */
public void tearDown() throws Exception {
	TestCompilationParticipant.PARTICIPANT = null;
	if (this.workingCopy != null) {
		this.workingCopy.discardWorkingCopy();
	}
	stopDeltas();
	super.tearDown();
}
public void tearDownSuite() throws Exception {
	deleteProject("Reconciler9");
	super.tearDownSuite();
}
private String deprecatedForRemoval(String element) {
	if (isJRE9)
		return element + " has been deprecated and marked for removal\n";
	else
		return element + " is deprecated\n";
}
/*
 * Ensures that the delta is correct when adding an annotation
 */
public void testAnnotations1() throws JavaModelException {
	setWorkingCopyContents(
		"/**\n" +
		"  * @category before\n" +
		"  * @category after\n" +
		"  */\n" +
		"@Deprecated\n" +
		"@MyAnnot\n" +
		"module mod.one {\n" +
		"}"
	);
	this.workingCopy.reconcile(ICompilationUnit.NO_AST, false, null, null);
	assertWorkingCopyDeltas(
		"Unexpected delta",
		"mod.one[*]: {CATEGORIES | ANNOTATIONS}\n" +
		"	@MyAnnot[+]: {}"
	);
}
/*
 * Ensures that the delta is correct when removing an annotation
 */
public void testAnnotations2deprecated() throws JavaModelException {
	setWorkingCopyContents(
		"/**\n" +
		"  * @category before\n" +
		"  */\n" +
		"module mod.one {\n" +
		"}"
	);
	this.workingCopy.reconcile(ICompilationUnit.NO_AST, false, null, null);
	assertWorkingCopyDeltas(
		"Unexpected delta",
		"mod.one[*]: {MODIFIERS CHANGED | ANNOTATIONS}"
	);
}
/*
 * Ensures that the delta is correct when removing an annotation
 */
public void testAnnotations2() throws JavaModelException {
	setWorkingCopyContents(
		"/**\n" +
		"  * @category before\n" +
		"  */\n" +
		"  @MyAnnot(x=1)\n" +
		"module mod.one {\n" +
		"}"
	);
	this.workingCopy.makeConsistent(null);	setWorkingCopyContents(
		"/**\n" +
		"  * @category before\n" +
		"  */\n" +
		"module mod.one {\n" +
		"}"
	);
	this.workingCopy.reconcile(ICompilationUnit.NO_AST, false, null, null);
	assertWorkingCopyDeltas(
		"Unexpected delta",
		"mod.one[*]: {ANNOTATIONS}\n" +
		"	@MyAnnot[-]: {}"
	);
}
/*
 * Ensures that the delta is correct when changing an annotation
 */
public void testAnnotations3() throws JavaModelException {
	setWorkingCopyContents(
		"/**\n" +
		"  * @category before\n" +
		"  */\n" +
		"  @MyAnnot(x=1)\n" +
		"module mod.one {\n" +
		"}"
	);
	this.workingCopy.makeConsistent(null);

	setWorkingCopyContents(
		"/**\n" +
		"  * @category before\n" +
		"  * @category after\n" +
		"  */\n" +
		"  @MyAnnot(y=1)\n" +
		"module mod.one {\n" +
		"}"
	);
	this.workingCopy.reconcile(ICompilationUnit.NO_AST, false, null, null);
	assertWorkingCopyDeltas(
		"Unexpected delta",
		"mod.one[*]: {CATEGORIES | ANNOTATIONS}\n" +
		"	@MyAnnot[*]: {CONTENT}"
	);
}
/**
 * Ensures that the delta is correct when adding a category
 */
public void testCategories1() throws JavaModelException {
	setWorkingCopyContents(
		"/**\n" +
		"  * @category before\n" +
		"  * @category after\n" +
		"  */\n" +
		"@Deprecated\n" +
		"module mod.one {\n" +
		"}"
	);
	this.workingCopy.reconcile(ICompilationUnit.NO_AST, false, null, null);
	assertWorkingCopyDeltas(
		"Unexpected delta",
		"mod.one[*]: {CATEGORIES}"
	);
}
/**
 * Ensures that the delta is correct when removing a category
 */
public void testCategories2() throws JavaModelException {
	setWorkingCopyContents(
		"/**\n" +
		"  */\n" +
		"@Deprecated\n" +
		"module mod.one {\n" +
		"}"
	);
	this.workingCopy.reconcile(ICompilationUnit.NO_AST, false, null, null);
	assertWorkingCopyDeltas(
		"Unexpected delta",
		"mod.one[*]: {CATEGORIES}"
	);
}
/**
 * Ensures that the delta is correct when changing a category
 */
public void testCategories3() throws JavaModelException {
	setWorkingCopyContents(
		"/**\n" +
		"  * @category never\n" +
		"  */\n" +
		"@Deprecated\n" +
		"module mod.one {\n" +
		"}"
	);
	this.workingCopy.reconcile(ICompilationUnit.NO_AST, false, null, null);
	assertWorkingCopyDeltas(
		"Unexpected delta",
		"mod.one[*]: {CATEGORIES}"
	);
}
public void testTerminalDeprecation1() throws CoreException {
	try {
		createJava9Project("P1");
		createFolder("/P1/src/p");
		createFile("/P1/src/p/X1.java", 
				"package p;\n" +
				"@Deprecated(forRemoval=true)\n" +
				"public class X1 {}");
		createFile("/P1/src/p/X2.java", 
				"package p;\n" +
				"public class X2 {\n" +
				"   @Deprecated(forRemoval=true)\n" +
				"	public Object field;\n" +
				"   @Deprecated(forRemoval=true)\n" +
				"	public void m() {}\n" +
				"}\n");

		setUpWorkingCopy("/P1/src/Y.java",
				"public class Y extends p.X1 {\n" +
				"	Object foo(p.X2 x2) {\n" +
				"		x2.m();\n" +
				"		return x2.field;\n" +
				"	}\n" +
				"}\n");
		assertProblems(
			"Unexpected problems",
			"----------\n" + 
			"1. WARNING in /P1/src/Y.java (at line 1)\n" + 
			"	public class Y extends p.X1 {\n" + 
			"	                         ^^\n" + 
			"The type X1 has been deprecated and marked for removal\n" + 
			"----------\n" + 
			"2. WARNING in /P1/src/Y.java (at line 3)\n" + 
			"	x2.m();\n" + 
			"	   ^^^\n" + 
			"The method m() from the type X2 has been deprecated and marked for removal\n" + 
			"----------\n" + 
			"3. WARNING in /P1/src/Y.java (at line 4)\n" + 
			"	return x2.field;\n" + 
			"	          ^^^^^\n" + 
			"The field X2.field has been deprecated and marked for removal\n" + 
			"----------\n"
		);
	} finally {
		deleteProject("P1");
	}
}
public void testTerminalDeprecation2() throws CoreException, IOException {
	try {
		IJavaProject p1 = createJava9Project("P1");
		String x1Source = "package p;\n" +
				"@Deprecated(forRemoval=true)\n" +
				"public class X1 {}";
		String x2Source = "package p;\n" +
			"public class X2 {\n" +
			"   @Deprecated(forRemoval=true)\n" +
			"	public Object field;\n" +
			"   @Deprecated(forRemoval=true)\n" +
			"	public void m() {}\n" +
			"	@Deprecated public void m2() {}\n" +
			"}\n";
		String[] allJarSources = (isJRE9)
				? new String[] {
					"p/X1.java",
					x1Source,
					"/P1/src/p/X2.java",
					x2Source }
				: new String[] {
					"java/lang/Deprecated.java",
					"package java.lang;\n" +
					"public @interface Deprecated {\n" +
					"	boolean forRemoval() default false;" +
					"}\n",
					"p/X1.java",
					x1Source,
					"/P1/src/p/X2.java",
					x2Source };
		createJar(
			allJarSources,
			p1.getProject().getLocation().append("lib.jar").toOSString(),
			null,
			"9");
		p1.getProject().refreshLocal(2, null);
		addLibraryEntry(p1, "/P1/lib.jar", false);

		setUpWorkingCopy("/P1/src/Y.java",
				"public class Y extends p.X1 {\n" +
				"	Object foo(p.X2 x2) {\n" +
				"		x2.m();\n" +
				"		x2.m2();\n" +
				"		return x2.field;\n" +
				"	}\n" +
				"}\n");
		assertProblems(
			"Unexpected problems",
			"----------\n" + 
			"1. WARNING in /P1/src/Y.java (at line 1)\n" + 
			"	public class Y extends p.X1 {\n" + 
			"	                         ^^\n" + 
			deprecatedForRemoval("The type X1") +
			"----------\n" + 
			"2. WARNING in /P1/src/Y.java (at line 3)\n" + 
			"	x2.m();\n" + 
			"	   ^^^\n" + 
			deprecatedForRemoval("The method m() from the type X2") + 
			"----------\n" + 
			"3. WARNING in /P1/src/Y.java (at line 4)\n" + 
			"	x2.m2();\n" + 
			"	   ^^^^\n" + 
			"The method m2() from the type X2 is deprecated\n" + 
			"----------\n" + 
			"4. WARNING in /P1/src/Y.java (at line 5)\n" + 
			"	return x2.field;\n" + 
			"	          ^^^^^\n" + 
			deprecatedForRemoval("The field X2.field") + 
			"----------\n");
	} finally {
		deleteProject("P1");
	}
}
public void testBug540541() throws CoreException, IOException {
	if (!isJRE9) return;
	IJavaProject project1 = null;
	IJavaProject project2 = null;
	Hashtable<String, String> options = JavaCore.getOptions();
	try {
		project1 = createJava9Project("java.base", "9");
		createFile("/java.base/src/module-info.java",
					"module java.base {\n" +
					"	exports java.lang;\n" +
					"}");
		createFolder("/java.base/src/java/lang");
		createFile("/java.base/src/java/lang/Object.java",
					"package java.lang;\n" +
					"public class Object {\n" +
					"}\n");
		
		project1.setRawClasspath(new IClasspathEntry[] {JavaCore.newSourceEntry(new Path("/java.base/src"))}, null);
		project1.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
		IMarker[] markers = project1.getProject().findMarkers(null, true, IResource.DEPTH_INFINITE);
		assertMarkers("Unexpected markers on java.base", "", markers);
		
		project2 = createJava9Project("client", "9");
		IClasspathAttribute[] attributes = new IClasspathAttribute[] { JavaCore.newClasspathAttribute("module", "true") };
		IClasspathEntry projectEntry = JavaCore.newProjectEntry(project1.getPath(), null, false, attributes, false);
		project2.setRawClasspath(new IClasspathEntry[] {projectEntry, JavaCore.newSourceEntry(new Path("/client/src"))}, null);
		createFolder("/client/src/p");
		createFile("/client/src/p/X.java",
					"package p;\n" +
					"public class X {\n" +
					"}\n");
		this.workingCopy = getCompilationUnit("client/src/p/X.java").getWorkingCopy(this.wcOwner, null);
		this.problemRequestor.initialize(this.workingCopy.getSource().toCharArray());
		this.workingCopy.reconcile(AST_INTERNAL_JLS11, true, this.wcOwner, null);
		assertProblems("Expecting no problems",
						"----------\n" + 
						"----------\n",
						this.problemRequestor);
		
		markers = project2.getProject().findMarkers(null, true, IResource.DEPTH_INFINITE);
		assertMarkers("Unexpected markers on client", "", markers);
	} finally {
		if (project1 != null)
			deleteProject(project1);
		if (project2 != null)
			deleteProject(project2);
		JavaCore.setOptions(options);
	}
}
public void testBug543092() throws Exception {
	if (!isJRE9) {
		System.err.println("Test "+getName()+" requires a JRE 9");
		return;
	}
	IJavaProject p = null;
	try {
		// ---- module log:
		//      - has log4j on the module path
		p = createJava9ProjectWithJREAttributes("p", new String[] {"src"},
				new IClasspathAttribute[] {JavaCore.newClasspathAttribute(IClasspathAttribute.MODULE, "true")});
		String jarAbsPath = p.getProject().getLocation()+"/lib-modular.jar";
		createJar(new String[] {
				"module-info.java",
				"module lib {\n" +
				"	exports lib.lab;\n" +
				"}\n",
				"lib/lab/Lib.java",
				"package lib.lab;\n" +
				"public class Lib {}\n"
			},
			jarAbsPath,
			null,
			"9");
		addLibraryEntry(p, new Path(jarAbsPath), false);

		String jarAbsPath2 = p.getProject().getLocation()+"/lib-nonmodular.jar";
		createJar(new String[] {
				"lib/lab/Lib.java",
				"package lib.lab;\n" +
				"public class Lib {}\n",
			},
			jarAbsPath2);
		addLibraryEntry(p, new Path(jarAbsPath2), false);

		createFolder("p/src/test");
		createFile("p/src/test/Test.java",
				"package test;\n" +
				"public class Test {\n" +
				"	lib.lab.Lib lob;\n" +
				"}\n");
		p.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
		IMarker[] markers = p.getProject().findMarkers(null, true, IResource.DEPTH_INFINITE);
		assertMarkers("markers in p",
				"",
				markers);

		this.workingCopy = getCompilationUnit("p/src/test/Test.java").getWorkingCopy(this.wcOwner, null);
		this.problemRequestor.initialize(this.workingCopy.getSource().toCharArray());
		this.workingCopy.reconcile(AST_INTERNAL_JLS11, true, this.wcOwner, null);
		assertProblems("Expecting no problems",
						"----------\n" + 
						"----------\n",
						this.problemRequestor);

	} finally {
		deleteProject(p);
	}
}
public void testBug543092b() throws Exception {
	if (!isJRE9) {
		System.err.println("Test "+getName()+" requires a JRE 9");
		return;
	}
	IJavaProject p = null;
	try {
		// ---- module log:
		//      - has log4j on the module path
		IClasspathAttribute[] moduleAttributes = new IClasspathAttribute[] {JavaCore.newClasspathAttribute(IClasspathAttribute.MODULE, "true")};
		p = createJava9ProjectWithJREAttributes("p", new String[] {"src"}, moduleAttributes);
		String jarAbsPath = p.getProject().getLocation()+"/lib-modular.jar";
		createJar(new String[] {
				"module-info.java",
				"module lib {\n" +
				"	exports lib.lab;\n" +
				"}\n",
				"lib/lab/Lib.java",
				"package lib.lab;\n" +
				"public class Lib {}\n"
			},
			jarAbsPath,
			null,
			"9");
		addLibraryEntry(p, new Path(jarAbsPath), null, null, null, null, moduleAttributes, false);

		String jarAbsPath2 = p.getProject().getLocation()+"/lib-nonmodular.jar";
		createJar(new String[] {
				"lib/lab/Lib.java",
				"package lib.lab;\n" +
				"public class Lib {}\n",
			},
			jarAbsPath2);
		addLibraryEntry(p, new Path(jarAbsPath2), false);

		createFolder("p/src/test");
		createFile("p/src/test/Test.java",
				"package test;\n" +
				"public class Test {\n" +
				"	lib.lab.Lib lob;\n" +
				"}\n");
		p.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
		IMarker[] markers = p.getProject().findMarkers(null, true, IResource.DEPTH_INFINITE);
		assertMarkers("markers in p",
				"The package lib.lab is accessible from more than one module: <unnamed>, lib",
				markers);

		this.workingCopy = getCompilationUnit("p/src/test/Test.java").getWorkingCopy(this.wcOwner, null);
		this.problemRequestor.initialize(this.workingCopy.getSource().toCharArray());
		this.workingCopy.reconcile(AST_INTERNAL_JLS11, true, this.wcOwner, null);
		assertProblems("Expecting no problems",
						"----------\n" + 
						"1. ERROR in /p/src/test/Test.java (at line 3)\n" + 
						"	lib.lab.Lib lob;\n" + 
						"	^^^^^^^\n" + 
						"The package lib.lab is accessible from more than one module: <unnamed>, lib\n" + 
						"----------\n",
						this.problemRequestor);
	} finally {
		deleteProject(p);
	}
}
public void testBug544017() throws CoreException {
	if (!isJRE9) {
		System.err.println("Test "+getName()+" requires a JRE 9");
		return;
	}
	IJavaProject testa = createJava9Project("testa");
	IJavaProject testb = createJava9Project("testb");
	IJavaProject testmain = createJava9Project("testmain");
	try {
		createFolder("testb/src/com/example/sub/b");
		createFile("testb/src/com/example/sub/b/B.java",
				"package com.example.sub.b;\n" + 
				"public class B {\n" + 
				"	public static void main(String[] args) {\n" + 
				"		System.out.println(\"B\");\n" + 
				"	}\n" + 
				"}\n");
		createFile("testb/src/module-info.java",
				"open module com.example.sub.b {\n" + 
				"	exports com.example.sub.b;\n" + 
				"}\n");

		addModularProjectEntry(testa, testb);
		createFolder("testa/src/com/example/sub/a");
		createFile("testa/src/com/example/sub/a/A.java",
				"package com.example.sub.a;\n" + 
				"public class A {\n" + 
				"	public static void main(String[] args) {\n" + 
				"		System.out.println(\"A\");\n" + 
				"	}\n" + 
				"}\n");
		createFile("testa/src/module-info.java",
				"open module com.example.sub.a {\n" + 
				"	exports com.example.sub.a;\n" + 
				"	requires com.example.sub.b;\n" + 
				"}\n");

		addModularProjectEntry(testmain, testa);
		addModularProjectEntry(testmain, testb);
		createFolder("testmain/src/com/example");
		createFile("testmain/src/module-info.java",
				"open module com.example {\n" + 
				"    requires com.example.sub.a;\n" + 
				"    requires com.example.sub.b;\n" + 
				"}\n");
		String pathExample = "testmain/src/com/example/Example.java";
		String sourceExample =
				"package com.example;\n" + 
				"import com.example.sub.a.A;\n" + 
				"import com.example.sub.b.B;\n" + 
				"\n" + 
				"public class Example {\n" + 
				"    public static void main(String[] args) {\n" + 
				"    	A.main(null);\n" + 
				"    	B.main(null);\n" + 
				"    }\n" + 
				"}\n";
		createFile(pathExample, sourceExample);

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
		IMarker[] markers = testmain.getProject().findMarkers(null, true, IResource.DEPTH_INFINITE);
		assertMarkers("markers in testmain",
				"",
				markers);

		ICompilationUnit wc = getCompilationUnit(pathExample).getWorkingCopy(this.wcOwner, null);
		wc.getBuffer().append(" ");
		this.problemRequestor.initialize((sourceExample+" ").toCharArray());
		wc.reconcile(AST_INTERNAL_JLS11, true, this.wcOwner, null);
		assertProblems("Expecting no problems",
						"----------\n" + 
						"----------\n",
						this.problemRequestor);
	} finally {
		deleteProject(testa);
		deleteProject(testb);
		deleteProject(testmain);
	}
}
}
