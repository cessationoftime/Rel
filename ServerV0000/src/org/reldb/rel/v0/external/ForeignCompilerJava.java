package org.reldb.rel.v0.external;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.reldb.rel.exceptions.*;
import org.reldb.rel.v0.generator.*;
import org.reldb.rel.v0.interpreter.ClassPathHack;
import org.reldb.rel.v0.storage.RelDatabase;
import org.reldb.rel.v0.types.*;
import org.reldb.rel.v0.types.builtin.TypeBoolean;
import org.reldb.rel.v0.types.builtin.TypeCharacter;
import org.reldb.rel.v0.types.builtin.TypeInteger;
import org.reldb.rel.v0.types.builtin.TypeRational;
import org.reldb.rel.v0.version.Version;

/**
 * @author Dave
 *
 */
public class ForeignCompilerJava {
	
	private Generator generator;
	private boolean verbose;
	
	public ForeignCompilerJava(Generator generator, boolean verbose) {
		this.generator = generator;
		this.verbose = verbose;
	}

	private static final String MANIFEST = "META-INF/MANIFEST.MF";
	
	private static String relCoreJar = null;
	
	// This bit of hackery is used to get a Rel core jar file so we can compile user Java code under a Web Start environment.
	// From https://weblogs.java.net/blog/2005/05/27/using-java-compiler-your-web-start-application
	private synchronized String getLocalWebStartRelJarName() {
		if (relCoreJar != null)
			return relCoreJar;
		try {
			for (Enumeration<?> e = getClass().getClassLoader().getResources(MANIFEST); e.hasMoreElements();) {
				URL url = (URL) e.nextElement();
				if (url.getFile().contains(Version.getCoreJarFilename())) {
					String relCoreJarName = getLocalJarFilename(url);
					if (relCoreJarName != null) {
						relCoreJar = relCoreJarName;
						return relCoreJar;
					}
				}
			}
		} catch (IOException exc) {
			exc.printStackTrace();
		}
		return null;
	}
	
	private static String getLocalJarFilename(URL remoteManifestFileName) {
		// remove trailing
		String urlStrManifest = remoteManifestFileName.getFile();
		String urlStrJar = urlStrManifest.substring(0, urlStrManifest.length() - MANIFEST.length() - 2);
		InputStream inputStreamJar = null;
		File tempJar;
		FileOutputStream fosJar = null;
		try {
			URL urlJar = new URL(urlStrJar);
			inputStreamJar = urlJar.openStream();
			String strippedName = urlStrJar;
			int dotIndex = strippedName.lastIndexOf('.');
			if (dotIndex >= 0) {
				strippedName = strippedName.substring(0, dotIndex);
				strippedName = strippedName.replace("/", File.separator);
				strippedName = strippedName.replace("\\", File.separator);
				int slashIndex = strippedName.lastIndexOf(File.separator);
				if (slashIndex >= 0) {
					strippedName = strippedName.substring(slashIndex + 1);
				}
			}
			tempJar = File.createTempFile(strippedName, ".jar");
			tempJar.deleteOnExit();
			fosJar = new FileOutputStream(tempJar);
			byte[] ba = new byte[1024];
			while (true) {
				int bytesRead = inputStreamJar.read(ba);
				if (bytesRead < 0) {
					break;
				}
				fosJar.write(ba, 0, bytesRead);
			}
			return tempJar.getAbsolutePath();
		} catch (Exception ioe) {
			System.out.println(ioe.getMessage());
			ioe.printStackTrace();
		} finally {
			try {
				if (inputStreamJar != null) {
					inputStreamJar.close();
				}
			} catch (IOException ioe) {}
			try {
				if (fosJar != null) {
					fosJar.close();
				}
			} catch (IOException ioe) {}
		}
		return null;
	}
	
	/** Return the package to which this entire Rel core belongs. */
	private String getPackagePrefix() {
		String thisPackageName = getClass().getPackage().getName();
		String relPackageName = thisPackageName.replace(".external", "");
		return relPackageName;
	}
	
	/** Return classpath to the Rel core. */
    private static String getLocalClasspath(RelDatabase database) {
        return System.getProperty("user.dir") + java.io.File.pathSeparatorChar + Version.getCoreJarFilename() + 
        	   java.io.File.pathSeparatorChar + database.getJavaUserSourcePath() +
        	   java.io.File.pathSeparatorChar + database.getHomeDir();
    }
    
    /** Given a Type, return the name of the equivalent Java type. */
    private final static String getJavaTypeForType(Generator generator, Type t) {
        if (t == null)
            return "void";
        else 
            return t.getValueClassname(generator);
    }

    /** Given a Type, return a Java expression to pop a Value from the operand
     * stack and convert it to an equivalent Java type. */
    private final static String getJavaPopForType(Generator generator, Type t) {
        if (t == null)
            throw new ExceptionFatal("RS0292: Got null type in getJavaPopForType()");
        return "(" + getJavaTypeForType(generator, t) + ")context.pop()";
    }

    /** Obtain a Java compiler invocation string.  The file name of the
     * Java source file to be compiled will be appended to this string 
     * to form a compilation command for external execution. */
    private String getJavacInvocation(RelDatabase database) {
    	String sysclasspathRaw = System.getProperty("java.class.path");
        System.out.println("ForeignCompilerJava: sysclasspathRaw is " + sysclasspathRaw);
        String sysclasspath = cleanClassPath(sysclasspathRaw);
        String devclasspathRaw = getLocalClasspath(database);
        System.out.println("ForeignCompilerJava: devclasspathRaw is " + devclasspathRaw);
        String devclasspath = cleanClassPath(devclasspathRaw);
        System.out.println("ForeignCompilerJava: sysclasspath is " + sysclasspath);
        System.out.println("ForeignCompilerJava: devclasspath is " + devclasspath);
        String webclasspath = getLocalWebStartRelJarName();
        System.out.println("ForeignCompilerJava: webclasspath is " + webclasspath);
        String cmd = "javac -classpath " + sysclasspath + java.io.File.pathSeparatorChar + devclasspath;
        if (webclasspath != null)
        	cmd += java.io.File.pathSeparatorChar + webclasspath;
        System.out.println("ForeignCompilerJava: cmd is " + cmd);
        return cmd;
    }
    
    /** Given an operator signature, return a Java method parameter definition. */
    private final static String getJavaMethodParmsForParameters(Generator generator, OperatorSignature os) {
        String s = "Context context";
        for (int i=0; i<os.getParmCount(); i++)
            s += ", " + getJavaTypeForType(generator, os.getParameterType(i)) + " " + os.getParameterName(i);
        return s;
    }
    
    /** Return a classpath cleaned of non-existent files.  Classpath
     * elements with spaces are converted to quote-delimited strings. */
    private final static String cleanClassPath(String s) {
    	if (java.io.File.separatorChar == '/')
    		s = s.replace('\\', '/');
    	else
    		s = s.replace('/', '\\');
        String outstr = "";
        java.util.StringTokenizer st = new java.util.StringTokenizer(s, java.io.File.pathSeparator);
        while (st.hasMoreElements()) {
            String element = (String)st.nextElement();
            java.io.File f = new java.io.File(element);
            if (f.exists() && !element.contains("deploy.jar")) {
            	String fname = f.toString();
            	if (fname.indexOf(' ')>=0)
            		fname = '"' + fname + '"';
                outstr += ((outstr.length()>0) ? java.io.File.pathSeparator : "") + fname;
            }
        }
        return outstr;
    }
	private static String getToolsJarPath(String javaHome) {
        File toolsJar = new File(javaHome, "../lib/tools.jar"); 
        if (toolsJar.exists())
        	return toolsJar.toString();
        toolsJar = new File(javaHome, "lib/tools.jar");
        if (toolsJar.exists())
        	return toolsJar.toString();
        toolsJar = new File(javaHome, "tools.jar");
        if (toolsJar.exists())
        	return toolsJar.toString();
        return null;
	}
	
	private static String getToolsJarPath() {
		String javaHome;
		javaHome = System.getProperty("java.home"); 
		if (javaHome != null) {
			 String toolsDir = getToolsJarPath(javaHome);
			 if (toolsDir != null)
				 return toolsDir;
		}
        javaHome = System.getenv("JAVA_HOME");
		if (javaHome != null) {
			 String toolsDir = getToolsJarPath(javaHome);
			 if (toolsDir != null)
				 return toolsDir;
		}
        javaHome = System.getenv("JDK_HOME");
		if (javaHome != null) {
			 String toolsDir = getToolsJarPath(javaHome);
			 if (toolsDir != null)
				 return toolsDir;
		}
		return null;
	}
	
	private JavaCompiler getSystemJavaCompiler() {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler != null)
			return compiler;
		String toolsJarPath = getToolsJarPath();
		if (toolsJarPath != null) {
			notify("ForeignCompilerJava: Found tools.jar at " + toolsJarPath.toString());
			try {
				ClassPathHack.addFile(toolsJarPath.toString());
			} catch (IOException e) {
				notify("ForeignCompilerJava: Unable to update classpath to include Tools.jar due to I/O error.");
				return null;
			}
			notify("ForeignCompilerJava: Attempting to load compiler via ToolProvider.");
			compiler = ToolProvider.getSystemJavaCompiler();
			if (compiler != null)
				return compiler;
			try {
				notify("ForeignCompilerJava: Attempting to load compiler class.");
				@SuppressWarnings("unchecked")
				Class<JavaCompiler> compilerClass = (Class<JavaCompiler>) Class.forName("com.sun.tools.javac.api.JavacTool");
				notify("ForeignCompilerJava: Tools.jar loaded.");
				if (compilerClass != null) {
					notify("ForeignCompilerJava: compiler class loaded.");
					try {
						return compilerClass.newInstance();
					} catch (Exception e) {
						notify("ForeignCompilerJava: Unable to instantiate internal Java compiler: " + e.toString());
					}
				} else {
					notify("ForeignCompilerJava: compiler class load failed.");
				}
			} catch (ClassNotFoundException e1) {
				notify("ForeignCompilerJava: Tools.jar load failed: JavaCompiler class not found.");
			}
		} else
			notify("ForeignCompilerJava: tools.jar not found.");
		return null;
	}
	
	private static boolean initialised = false;
	private static JavaCompiler compiler = null;
	
    /** Compilation of foreign source code. */
    public void compileForeignCode(RelDatabase database, PrintStream printstream, String className, String src) {
        // If resource directory doesn't exist, create it.
        File resourceDir = new File(database.getJavaUserSourcePath()); 
        if (!(resourceDir.exists()))
            resourceDir.mkdirs();
        
        File sourcef;
        try {
	        // Write source to a Java source file
	        sourcef = new File(database.getJavaUserSourcePath() + java.io.File.separator + getStrippedClassname(className) + ".java");
	        PrintStream sourcePS = new PrintStream(new FileOutputStream(sourcef));
	        sourcePS.print(src);
	        sourcePS.close();
        } catch (IOException ioe) {
        	throw new ExceptionFatal("RS0293: Unable to save Java source: " + ioe.toString());
        }
        
        if (!initialised) {
       		compiler = getSystemJavaCompiler();
        	initialised = true;
        }
        if (compiler != null) {
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
            List<String> optionList = new ArrayList<String>();
            String classpath = 
            	System.getProperty("java.class.path") + 
            	java.io.File.pathSeparatorChar + 
            	cleanClassPath(getLocalClasspath(database));
            String webclasspath = getLocalWebStartRelJarName();
            if (webclasspath != null)
            	classpath += File.pathSeparatorChar + webclasspath;
            optionList.addAll(Arrays.asList("-classpath", classpath)); 
            compiler.getTask(null, fileManager, diagnostics, optionList, null, fileManager.getJavaFileObjects(sourcef)).call();
            String msg = "";
            for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
            	msg += "Java error: " + diagnostic.getMessage(null) + ":\n" +
            			" in line " + diagnostic.getLineNumber() + " of:\n" +
                                ((diagnostic.getSource() != null) ? diagnostic.getSource().toString() : "<unknown>") + "\n";
            }
        	if (msg.length() > 0)
        		throw new ExceptionSemantic("RS0004: " + msg);
        	return;
        }
        
    	System.out.println("NOTE: A 'tools.jar' or internal Java compiler can't be found.");
    	System.out.println("      Make sure JAVA_HOME or JDK_HOME point to a JDK installation.");
    	System.out.println("      Trying to find an external javac compiler as an alternative.");
    	
    	try {
            // Compile source
            int retval = -1;
            String sourcefile = sourcef.getAbsolutePath();
            if (sourcefile.indexOf(' ')>=0)
            	sourcefile = '"' + sourcefile + '"';
           	String command = getJavacInvocation(database) + " " + sourcefile;
           	System.out.println("ForeignCompilerJava: command is " + command);
           	retval = ExternalExecutor.run(printstream, command);
            if (retval != 0) {
            	notify("? Compile failed.  Attempted with: " + command);
            	throw new ExceptionSemantic("RS0005: Java compilation failed due to errors.");
            }
        } catch (Throwable t) {
            throw new ExceptionSemantic("RS0289: " + t.toString());
        }
    }
    
    /** Get a stripped name.  Only return text after the final '.' */
    private static String getStrippedName(String name) {
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0)
            return name.substring(lastDot + 1);
        else
            return name;
    }
    
    /** Get stripped Java Class name. */
    private static String getStrippedClassname(String name) {
    	return getStrippedName(name);
    }
        
    private Type getTypeForClassName(Generator generator, String className) {
        if (className.equals(getPackagePrefix() + ".values.ValueBoolean"))
            return TypeBoolean.getInstance();
        else if (className.equals(getPackagePrefix() + ".values.ValueCharacter"))
            return TypeCharacter.getInstance();
        else if (className.equals(getPackagePrefix() + ".values.ValueInteger"))
            return TypeInteger.getInstance();
        else if (className.equals(getPackagePrefix() + ".values.ValueRational"))
            return TypeRational.getInstance();
        else if (className.equals(getPackagePrefix() + ".values.ValueObject"))
            return TypeInteger.getInstance();
        else if (className.equals(getPackagePrefix() + ".values.ValueOperator"))
            return TypeOperator.getInstance();
        else 
            return generator.locateType(getStrippedClassname(className));
    }
    
    private void notify(String s) {
    	if (verbose)
    		System.out.println(s);
    }
    
    /** Given a Java class, return the Type for which it is a Value, if there is one.
     * Return null if there isn't one. */
	private Type getTypeForClass(Class<?> c) {
    	return getTypeForClassName(generator, c.getName());
    }
    
    /** Given a class, and one of its constructors, define a selector operator. */
	private void addSelectorForClass(Class<?> c, java.lang.reflect.Constructor<?> con) {
    	String name = getTypeForClass(c).getValueClassname(generator);
    	OperatorSignature sig = new OperatorSignature(name);
        Class<?>[] rawParameters = con.getParameterTypes();
        if (rawParameters.length == 0)
            return;
        sig.setReturnType(getTypeForClass(c));
        if (sig.getReturnType() == null)
            throw new ExceptionSemantic("RS0006:  Unable to create selector for TYPE " + c.getName());
        String arguments = "context.getGenerator()";
        if (!rawParameters[0].toString().equals("class " + Generator.class.getCanonicalName())) {
    		notify("x Constructor " + con + " of " + c + " not implemented as OPERATOR due to missing first parameter of type " + Generator.class.getCanonicalName());
    		return;
    	}
        for (int i=1; i<rawParameters.length; i++) {
            if (rawParameters[i].isPrimitive()) {
                notify("x Constructor " + con + " of " + c + " not implemented as selector due to primitive parameter type.");
                return;
            }
            Type t = getTypeForClass(rawParameters[i]);
            if (t == null) {
                notify("x Constructor " + con + " of " + c + " not implemented as selector due to unrecognised parameter type " + rawParameters[i].toString());
                return;     // type not found -- not implementable
            }
            String parmName = "p" + i;
            sig.addParameter("p" + i, t);
            arguments += ((arguments.length() > 0) ? ", " : "") + parmName;
        }
        String language = "Java";
        String typeName = getStrippedClassname(c.getName());
        String src = "return new " + typeName + "(" + arguments + ");";  
        notify("* Constructor " + con + " of " + c + " implemented as selector OPERATOR " + sig);
        OperatorDefinition newOp = compileForeignOperator(sig, language, src);
        newOp.setCreatedByType(typeName);
        newOp.getReferences().addReferenceToType(typeName);
        generator.addOperator(newOp);
    }
    
    /** Given a class, and one of its methods, define an operator. */
	private void addOperatorForClass(Class<?> c, java.lang.reflect.Method m) {
        String name;
    	if (Modifier.isStatic(m.getModifiers()))
    		name = m.getName();
    	else
        	name = "THE_" + m.getName();
    	OperatorSignature sig = new OperatorSignature(name);
        Class<?>[] rawParameters = m.getParameterTypes();
        Class<?> rawReturnType = m.getReturnType();
        if (rawReturnType.isPrimitive()) {
            notify("x Method " + m + " of " + c + " not implemented as OPERATOR due to primitive return type.");
            return;
        }
        Type returnType = getTypeForClass(rawReturnType);
        if (returnType == null) {
            notify("x Method " + m + " of " + c + " not implemented as OPERATOR due to unrecognised return type.");
            return;
        }
        sig.setReturnType(returnType);
        Type instanceType = getTypeForClass(c);
        if (instanceType == null)
            throw new ExceptionSemantic("RS0007: Java type " + c + " not recognised.");
        String arguments = "context.getGenerator()";
    	if (!Modifier.isStatic(m.getModifiers()))
    		sig.addParameter("px", getTypeForClass(c));
    	else if (!rawParameters[0].toString().equals("class " + Generator.class.getCanonicalName())) {
    		notify("x Method " + m + " of " + c + " not implemented as OPERATOR due to missing first parameter of type " + Generator.class.getCanonicalName());
    		return;
    	}
        for (int i=1; i<rawParameters.length; i++) {
            String parmName = "p" + i;
            if (rawParameters[i].isPrimitive()) {
                notify("x Method " + m + " of " + c + " not implemented as OPERATOR due to primitive parameter type.");
                return;
            }
            Type t = getTypeForClass(rawParameters[i]);
            if (t == null) {
                notify("x Method " + m + " of " + c + " not implemented as OPERATOR due to unrecognised parameter type " + rawParameters[i].toString());
                return;     // type not found -- not implementable
            }
            sig.addParameter(parmName, t);
            arguments += ((arguments.length() > 0) ? ", " : "") + parmName;
        }
        String language = "Java";
        String src;
        String typeName = getStrippedClassname(c.getName());
        String returnStr = (returnType == null) ? "" : "return ";
        if (Modifier.isStatic(m.getModifiers()))
            src = returnStr + typeName + "." + m.getName() + "(" + arguments + ");";
    	else
            src = returnStr + "px." + m.getName() + "(" + arguments + ");";
        notify("* Method " + m + " of " + c + " implemented as OPERATOR " + sig);
        OperatorDefinition newOp = compileForeignOperator(sig, language, src);
        newOp.setCreatedByType(typeName);
        newOp.getReferences().addReferenceToType(typeName);
        generator.addOperator(newOp);
    }
    
    /** Given a Java Class, generate Rel Operators to access it. */
	private void addOperatorsForClass(Class<?> c) {
        try {
            for (java.lang.reflect.Constructor<?> constructor: c.getDeclaredConstructors())
                addSelectorForClass(c, constructor);
            for (java.lang.reflect.Method method: c.getDeclaredMethods())
       			addOperatorForClass(c, method);
        } catch (Throwable t) {
            throw new ExceptionSemantic("RS0008: Unable to add operators for " + c.getName() + ": " + t.toString());
        }
    }
    
    /** Compile a user-defined Java-based type. */
	public void compileForeignType(String name, String language, String src) {
        if (!language.equals("Java"))
            throw new ExceptionSemantic("RS0009: " + language + " is not recognised as a foreign language.");
        String body = src.replace("\n", "\n\t");
        src = "package " + RelDatabase.getRelUserCodePackage() + ";\n\n" +
        	  "import " + getPackagePrefix() + ".vm.*;\n" +
              "import " + getPackagePrefix() + ".types.*;\n" + 
              "import " + getPackagePrefix() + ".types.builtin.*;\n" + 
              "import " + getPackagePrefix() + ".types.userdefined.*;\n" + 
			  "import " + getPackagePrefix() + ".values.*;\n" +
			  "import " + getPackagePrefix() + ".generator.Generator;\n\n" +
              "public class " + name + " extends ValueTypeJava {\n" + 
                  body +
              "\n}";
        compileForeignCode(generator.getDatabase(), generator.getPrintStream(), name, src);
        try {
        	Class<?> typeClass = generator.getDatabase().loadClass(name);
            if (typeClass == null)
                throw new ExceptionSemantic("RS0010: Despite having been compiled, " + name + " could not be loaded.");
            notify("> Java class " + typeClass.getName() + " loaded to implement type " + name);
            Constructor<?> ctor = typeClass.getConstructor(new Class[] {Generator.class});
            if (ctor == null)
            	throw new ExceptionSemantic("RS0011: Unable to find a constructor of the form " + name + "(Generator generator)");
            generator.addTypeInProgress(name, (Type)ctor.newInstance(generator));
            addOperatorsForClass(typeClass);
        } catch (Throwable thrown) {
            throw new ExceptionSemantic("RS0012: Creation of type " + name + " failed: " + thrown.toString());
        }
    }

    /** Compilation of foreign operator. */
    public OperatorDefinition compileForeignOperator(OperatorSignature signature, String language, String src) {
        if (!language.equals("Java"))
            throw new ExceptionSemantic("RS0013: " + language + " is not recognised as a foreign language.");
        // Modify src here to include class definition and appropriate methods
        // One of these must be 'public static void execute(Session s)'
        String comp = 
        	  "package " + RelDatabase.getRelUserCodePackage() + ";\n\n" +
			  "import " + getPackagePrefix() + ".types.*;\n" +
              "import " + getPackagePrefix() + ".types.builtin.*;\n" + 
              "import " + getPackagePrefix() + ".types.userdefined.*;\n" + 
			  "import " + getPackagePrefix() + ".values.*;\n" +
			  "import " + getPackagePrefix() + ".vm.Context;\n\n" +
              "public class " + getStrippedClassname(signature.getClassSignature()) + " {\n" +
              "\tprivate static final " + getJavaTypeForType(generator, signature.getReturnType()) + " " +
              "do_" + getStrippedName(signature.getName()) + "(" + getJavaMethodParmsForParameters(generator, signature) + ") {\n" + 
              "\t\t" + src.replace("\n", "\n\t\t") + "\n\t}\n\n" +
              "\tpublic static final void execute(Context context) {\n";
        String args = "context";
        Type[] parmTypes = signature.getParameterTypes();
        for (int i=parmTypes.length - 1; i >= 0; i--) {
            comp += "\t\t" + getJavaTypeForType(generator, parmTypes[i]) + " " + "p" + i + " = " + getJavaPopForType(generator, parmTypes[i]) + ";\n";
            args += ", p" + (parmTypes.length - i - 1);
        }
        if (signature.getReturnType() != null)
            comp += "\t\tcontext.push(" + "do_" + getStrippedName(signature.getName()) + "(" + args + "));\n";
        else
            comp += "\t" + "do_" + getStrippedName(signature.getName()) + "(" + args + ");\n";
        comp += "\t}\n}";
        // compile source
        compileForeignCode(generator.getDatabase(), generator.getPrintStream(), signature.getClassSignature(), comp);
        notify("> Java class " + signature.getClassSignature() + " compiled to implement OPERATOR " + signature);
        // construct operator definition
        OperatorDefinitionNative o;
        Method method = generator.getDatabase().getMethod(signature);
        if (signature.getReturnType() != null)
        	o = new OperatorDefinitionNativeFunctionExternal(signature, method);
        else
        	o = new OperatorDefinitionNativeProcedureExternal(signature, method);
        o.setSourceCode(signature.getOperatorDeclaration() + " " + language + " FOREIGN " + src + "\nEND OPERATOR;");
        return o;
    }
    
}
