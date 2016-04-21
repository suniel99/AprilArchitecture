package gipsy.GIPC.imperative.Java;

import gipsy.GIPC.GIPCException;
import gipsy.GIPC.imperative.FormatTag;
import gipsy.GIPC.imperative.ImperativeCompiler;
import gipsy.GIPC.imperative.ImperativeCompilerException;
import gipsy.interfaces.AbstractSyntaxTree;

import java.io.FileWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

import marf.util.Debug;


/**
 * <p>Main Java Compiler.</p>
 *
 * $Id: JavaCompiler.java,v 1.19 2008/10/30 00:28:41 mokhov Exp $
 *
 * @author Serguei Mokhov
 * @version $Revision: 1.19 $
 * @since 1.0.0
 */
public class JavaCompiler
extends ImperativeCompiler
{
	protected String strWrappedCode = "";
	protected String strClassName = "";
	
	public JavaCompiler()
	{
	}

	/**
	 * Instantiates JavaSequentialThreadGenerator.
	 * @see gipsy.GIPC.ICompiler#init()
	 * @see JavaSequentialThreadGenerator
	 */
	public void init()
	throws GIPCException
	{
		try
		{
			//this.oSTGenerator = new JavaSequentialThreadGenerator("FooBarBaz");
			if(false)
				throw new ClassNotFoundException();
			
			//this.oCPGenerator = new JavaCommunicationProcedureGenerator();
			this.oFormatTag = FormatTag.JAVA;
		}
		catch(ClassNotFoundException e)
		{
			throw new GIPCException(e);
		}
	}

	public AbstractSyntaxTree parse()
	throws GIPCException
	{
		wrap();
		return compileExternally();
	}

	protected void wrap()
	throws ImperativeCompilerException
	{
		generateHead();
		generateBody();
		generateTail();
	}

	protected void generateHead()
	throws ImperativeCompilerException
	{
		this.strClassName = generateClassName(getClass().getName());
		this.strWrappedCode =
			  "// Generated by " + getClass().getName() + " $Revision: 1.19 $ / GIPSY\n"
			+ "import gipsy.interfaces.*;\n"
			+ "public class " + this.strClassName + "\n"
			+ "implements ISequentialThread {\n"
			+ "";
	}

	protected void generateBody()
	throws ImperativeCompilerException
	{
		try
		{
			int iChar;
			String strSTCode = "";
			
			while((iChar = this.oSourceCodeStream.read()) != -1)
			{
				strSTCode += (char)iChar;
			}
	
			this.strWrappedCode +=
				  "  public WorkResult oWorkResult = null;\n"
				+ "  // Constructor\n"
				+ "  public " + this.strClassName + "() {\n"
				+ "  }\n\n"

				+ "  // Implements ISequentialThread\n"
				+ "  public WorkResult work() { return this.oWorkResult = new WorkResult(); }\n\n"
				+ "  public WorkResult getWorkResult() {return this.oWorkResult;}\n\n"
				+ "  public void setMethod(java.lang.reflect.Method poMethod) {}\n\n"

				+ "  // Implements Runnable\n"
				+ "  public void run() {this.oWorkResult = work();}\n\n"

				// The wrapped #JAVA code code
				+ "//#JAVA BEGIN\n" + strSTCode + "//#JAVA END\n" 
				+ "";
		}
		catch(Exception e)
		{
			throw new ImperativeCompilerException(e);
		}
	}

	protected void generateTail()
	throws ImperativeCompilerException
	{
		this.strWrappedCode += "}\n// EOF\n";
	}
	
	public static final String generateClassName(String pstrFilename)
	{
		String strHostName = null;
		
		try
		{
			strHostName = InetAddress.getLocalHost().getHostName();
		}
		catch(UnknownHostException e)
		{
			strHostName = "localhost";
		}
		
		String strClassName =
		(
			pstrFilename + "_"
			+ strHostName + "_"
			//+ System.getProperty("user.name") + "_"
//			+ new Date()
		).replaceAll("[ :\\.]", "_");
		
		return strClassName;
	}
	
	private AbstractSyntaxTree compileExternally()
	throws GIPCException
	{
		try
		{
			FileWriter oFileWriter  = new FileWriter(this.strClassName + ".java");
			oFileWriter.write(this.strWrappedCode);
			oFileWriter.close();

			String strCompilerCommand =
				"javac "
				//+ "-g -source 1.4 -sourcepath src "
				+ "-g -source 1.5 -sourcepath src "
				+ "-classpath bin:bin/gipsy:.:src:gipc.jar:gipsy.jar:src/gipsy:/Library/Tomcat/webapps/WebEditor/WEB-INF/lib/gipsy.jar "
				+ "-d bin "
				+ this.strClassName + ".java";
			
			Debug.debug("JavaCompiler: spawning external compiler");
			Process oExternalCompiler = Runtime.getRuntime().exec(strCompilerCommand);
			
			Debug.debug("JavaCompiler: waiting for external compiler to terminate");
			int iRetVal = oExternalCompiler.waitFor();

			Debug.debug("JavaCompiler: external compiler terminated");
		
		
			int iChar;
			String strStdError = "";
			String strStdOut = "(o)";

			Debug.debug("JavaCompiler: reading in STDERR and STDOUT streams of the external compiler");

			while((iChar = oExternalCompiler.getErrorStream().read()) != -1)
			{
				strStdError += (char)iChar;
			}

			while((iChar = oExternalCompiler.getInputStream().read()) != -1)
			{
				strStdOut += (char)iChar;
			}
			
			System.out.println("STDOUT >>>: " + strStdOut + "<<<STDOUT");
			
			if(iRetVal != 0)
			{
				throw new GIPCException
				(
					"External compiler " + oExternalCompiler + " failed. Details:\n" +
					strStdError + "\nCommand was: " + strCompilerCommand
				);
			}
		
			Debug.debug("prior ST construction: " +  System.getProperties());
			Debug.debug("user.dir: " +  System.getProperty("user.dir"));
			Debug.debug("java.class.path: " +  System.getProperty("java.class.path"));
			//System.getProperties().list(System.out);
			//ISequentialThread oST = (ISequentialThread)Class.forName(this.strClassName).newInstance();
			System.setProperty
			(
				"java.class.path",
				System.getProperty("user.dir") + System.getProperty("path.separator") +
				System.getProperty("java.class.path") +
				System.getProperty("path.separator") + System.getProperty("user.dir")
			);
			
			Debug.debug("java.class.path2: " +  System.getProperty("java.class.path"));

			this.oSTGenerator = new JavaSequentialThreadGenerator(this.strClassName);
			Debug.debug("JavaCompiler: ST Generator Constructed...");
			//Debug.debug("ST tmp not-Constructed");
			
			//this.oSTGenerator.generate();
			
			return null;
		}
		catch(GIPCException e)
		{
			throw e;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new GIPCException(e);
		}
	}
	
	public String toString()
	{
		return this.strWrappedCode;
	}
}

// EOF