package gipsy.GIPC.intensional.SIPL.ForensicLucid;

import gipsy.GIPC.ISemanticAnalyzer;
import gipsy.GIPC.intensional.SimpleNode;
import gipsy.GIPC.util.Node;
import gipsy.lang.GIPSYIdentifier;
import gipsy.lang.GIPSYInteger;
import gipsy.lang.GIPSYObject;
import gipsy.lang.GIPSYString;
import gipsy.lang.GIPSYType;
import gipsy.lang.context.OrderedFiniteNonPeriodicTagSet;
import gipsy.lang.context.OrderedFinitePeriodicTagSet;
import gipsy.lang.context.OrderedInfiniteNonPeriodicTagSet;
import gipsy.lang.context.OrderedInfinitePeriodicTagSet;
import gipsy.lang.context.TagSet;
import gipsy.lang.context.UnorderedFiniteNonPeriodicTagSet;
import gipsy.lang.context.UnorderedFinitePeriodicTagSet;
import gipsy.lang.context.UnorderedInfinitePeriodicTagSet;
import gipsy.storage.Dictionary;
import gipsy.storage.DictionaryItem;
import gipsy.storage.FunctionItem;
import gipsy.tests.GIPC.intensional.SIPL.Lucx.SemanticTest.LucxSemanticAnalyzer;

import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import marf.util.FreeVector;


/**
 * Does static Forensic Lucid semantic analysis based on
 * the input abstract syntactic tree from the parser.
 *
 * @author Serguei Mokhov
 * @version $Id: ForensicLucidSemanticAnalyzer.java,v 1.2 2013/08/25 02:59:26 mokhov Exp $
 */
public class ForensicLucidSemanticAnalyzer
extends LucxSemanticAnalyzer
implements ISemanticAnalyzer, ForensicLucidParserConstants, ForensicLucidParserTreeConstants
{
	/**
	 * TODO: document
	 */
	private DictionaryItem item, current, previous;

	/**
	 * Dictionary of identifiers.
	 */
	private Dictionary oDictionary = new Dictionary();

	/**
	 * TODO: document
	 */
	private int iCount;

	/**
	 * Number of semantic errors found.
	 */
	private int iErrorCount = 0;

	/**
	 * Number of warnings produced.
	 */
	private int iWarningCount = 0;

	/**
	 * This is the stack for semantic checking
	 */
	private Stack<Node> oSemanticStack = new Stack<Node>();

	/**
	 * TODO: document
	 */
	private Stack<SimpleNode> oTempStack = new Stack<SimpleNode>();

	/**
	 * TODO: document
	 */
	private Stack<Node> oSecondStack = new Stack<Node>();

	//for check the type and rank of identifiers second time.

	/**
	 * TODO: document
	 */
	private String p_kind = "identifier";

	/**
	 * TODO: document
	 */
	private String p_name = "";

	/**
	 * TODO: document
	 */
	private SimpleNode p_entry;

	/**
	 * TODO: document
	 */
	private int p_type = 0;

	/**
	 * TODO: document
	 */
	private boolean again = false; //for the second semantic checking, some attribute is not available

	/**
	 * TODO: document
	 */
	private Hashtable<String, FunctionItem> FunTable = new Hashtable<String, FunctionItem>(); //for function elimination

	/**
	 * TODO: document
	 */
	private FunctionItem FunIP = new FunctionItem();

	/**
	 * TODO: document
	 */
	private int DN = 0, PN = 0; //for setting up function

	/**
	 * TODO: document, fix hardcoding
	 */
	private String Dim[] = new String[10];

	/**
	 * TODO: document, fix hardcoding
	 */
	private String Para[] = new String[10];

	/**
	 * TODO: document
	 */
	private Vector<SimpleNode> PL = new Vector<SimpleNode>();

	/**
	 * TODO: document
	 */
	private boolean HasFun = false;

	/**
	 * Class constructor.
	 */
	public ForensicLucidSemanticAnalyzer()
	{
	}

	/* (non-Javadoc)
	 * @see gipsy.GIPC.intensional.SIPL.ForensicLucid.ISemanticAnalyzer#getDictionary()
	 */
	@Override
	public Dictionary getDictionary()
	{
		return this.oDictionary;
	}

	/* (non-Javadoc)
	 * @see gipsy.GIPC.intensional.SIPL.ForensicLucid.ISemanticAnalyzer#setupDictionary(gipsy.GIPC.intensional.SimpleNode)
	 */
	@Override
	public void setupDictionary(SimpleNode address)
	{
		SimpleNode as, temp;
		previous = null;
		iCount = 0;
		address.ID = iCount;		
		current = new DictionaryItem(iCount, "Start", "", 2, "", address, null, true);		
		//'start' node
		oDictionary.addElement(current);
		current.getHashtable().put(current.getName(), current);
		iCount++;

		as = (SimpleNode)address.children[0];

		if(as.children != null)
		{
			as.dump(" ");
			setFunction(as, 0); //first deal with the function			
			if(HasFun)
			{
				eliminateFunction(as, 0);
				HasFun = false;
			}

			traverseTree(as); //set up the dictionary of identifier
			TS(as);
		}
		else
			System.err.println("There is no program should be complied!");

		int co = 0;

		while(co < oSemanticStack.size())
		{
			oSecondStack.addElement(oSemanticStack.elementAt(co));
			co++;
		}

		if(!oSemanticStack.empty())
			typeCheck();

		co = 0;

		while(co < oSecondStack.size())
		{
			oSemanticStack.addElement(oSecondStack.elementAt(co));
			co++;
		}

		if((again) && (!oSemanticStack.empty()))
		{
			System.out.println("Second Semantic Check.");
			typeCheck();
		}

		for(int k = 0; k < oDictionary.size(); k++)
		{
			item = (DictionaryItem)oDictionary.elementAt(k);

			System.out.println
			(
				item.getID()
				+ " __ "
				+ item.getName()
				+ " __ "
				+ item.getKind()
				+ " __ "
				+ item.getType()
				+ " __ "
				+ item.getRank()
				+ " __ "
				+ item.getEntry()
			);
		}
	}

	/* (non-Javadoc)
	 * @see gipsy.GIPC.intensional.SIPL.ForensicLucid.ISemanticAnalyzer#setFunction(gipsy.GIPC.intensional.SimpleNode, int)
	 */
	@Override
	public void setFunction(SimpleNode FunT, int Sfn)
	{
		SimpleNode ft1, ft2, ft3, ftp;
		int ChildNum = 0;
        //where
		if(FunT.toString().trim().equals("WHERE"))
		{
			ftp = (SimpleNode)FunT.parent;

			for(int m = 1; m < FunT.children.length; m++)
			{
				ft1 = (SimpleNode)FunT.children[m];//equal
				if(ft1.toString().trim().equals("ASSIGN"))
				{
					if(((SimpleNode)ft1.children[0]).toString().trim().equals("FUN"))
						//is a function
					{
						HasFun = true;
						ft2 = (SimpleNode)ft1.children[0];
						p_name = ft2.getImage();
						if(ft2.children != null)
						{
							ft3 = (SimpleNode)ft2.children[0];

							//is dimension
							if(ft3.toString().trim().equals("DIM"))
							{
								DN = ft3.children.length;
								for(int d1 = 0; d1 < DN; d1++)
									Dim[d1] = ((SimpleNode)ft3.children[d1]).getImage();
							}
							else
							{
								PN = ft3.children.length;
								for(int d2 = 0; d2 < PN; d2++)
									Para[d2] = ((SimpleNode)ft3.children[d2]).getImage();
							}
							
							if(ft2.children.length == 2)
							{
								ft3 = (SimpleNode)ft2.children[1];
								PN = ft3.children.length;

								for(int d3 = 0; d3 < PN; d3++)
									Para[d3] = ((SimpleNode)ft3.children[d3]).getImage();
							}
							
							p_entry = take((SimpleNode)ft1.children[1]);
						}
						else
							p_entry = (SimpleNode)ft1.children[1];

						FunIP = new FunctionItem(p_name, DN, PN, p_entry);
						FunTable.put(p_name, FunIP);

						if (!(((SimpleNode)ft1.children[1]).toString().trim().equals("WHERE"))
							&& (FunT.children.length == 2))
							FunT = (SimpleNode)FunT.children[0];
						else
						{
							ft1.id = ForensicLucidParserTreeConstants.JJTFUN;
							ft1.setImage("");
							ft1.children = null;
							ft1.parent = FunT;
							FunT.children[m] = ft1;
						}
					}
					else
						setFunction(ft1, m);
				}
			}
		}
		else
		{
			for(int i = 0; i < FunT.children.length; i++)
			{
				ft1 = (SimpleNode)FunT.children[i];
				
				if(ft1.children != null)
					setFunction(ft1, i);
			}
		}
	}

	/* (non-Javadoc)
	 * @see gipsy.GIPC.intensional.SIPL.ForensicLucid.ISemanticAnalyzer#take(gipsy.GIPC.intensional.SimpleNode)
	 */
	@Override
	public SimpleNode take(SimpleNode FunB)
	{
		if(FunB.children == null)
		{
			//is a identifier
			if(FunB.toString().substring(0,FunB.toString().indexOf(":")).trim().equals("ID"))
			{
				for(int fp = 0; fp < PN; fp++)
					if(FunB.getImage().equals(Para[fp]))
						FunB.setImage("~" + Integer.toString(fp));
			}
			else
				if(FunB.toString().trim().equals("DIMENSION"))
				{
					for(int fd = 0; fd < DN; fd++)
						if(FunB.getImage().equals(Dim[fd]))
							FunB.setImage("#" + Integer.toString(fd));
				}
		}
		else
		{
			for(int f = 0; f < FunB.children.length; f++)
				take((SimpleNode)FunB.children[f]);
		}

		return FunB;
	}

	/**
	 * Replaces function by its definition.
	 * The output of AST is a simple tree without function node.
	 *
	 * @param begin  Entry SimpleNode of a function.
	 * @param Fcn    integer number of children.
	 */
	public void eliminateFunction(SimpleNode begin, int Fcn)
	{
		FunctionItem FT = new FunctionItem();
		SimpleNode Fp, Fp1, Fparent, Ftemp1, Ftemp2;
		int FDnum = 0, FPnum = 0;
		int p1 = 0, p2 = 0;
		boolean IS = true;

		Dim = new String[10];
		Para = new String[10];

		//is a function
		if((begin.toString().trim().equals("FUN")) && (begin.getImage() != "")) 
		{
			if(FunTable.containsKey(begin.getImage()))
			{
				Fparent = (SimpleNode)begin.parent;

				if(begin.children != null)
					//judge has children, dimension or parameters
				{
					if(begin.children.length == 2)
					{
						Ftemp1 = (SimpleNode)begin.children[0];
						Ftemp2 = (SimpleNode)begin.children[1];
						FDnum = Ftemp1.children.length;
						FPnum = Ftemp2.children.length;
						for(int f1 = 0; f1 < FDnum; f1++)
							Dim[f1] =
								((SimpleNode)Ftemp1.children[f1]).getImage();
						for(int f2 = 0; f2 < FPnum; f2++)
							PL.addElement((SimpleNode)Ftemp2.children[f2]);
					}
					else
					{
						Ftemp1 = (SimpleNode)begin.children[0];
						if(Ftemp1.toString().trim().equals("DIM"))
						{
							FDnum = Ftemp1.children.length;
							for(int f1 = 0; f1 < FDnum; f1++)
								Dim[f1] =
									((SimpleNode)Ftemp1.children[f1])
										.getImage();
						}
						else
							if(Ftemp1.toString().trim().equals("PARAS"))
							{
								FPnum = Ftemp1.children.length;
								for(int f2 = 0; f2 < FPnum; f2++)
									PL.addElement(
										(SimpleNode)Ftemp1.children[f2]);
							}
					}
				}

				FT = (FunctionItem)FunTable.get(begin.getImage());
				Fp = FT.getFunctionEntry();

				if((FT.getDimensions() != FDnum)
					|| (FT.getParamCount() != FPnum))
				{
					System.err.println("Semantic error: Function parameters' number error.");
					iErrorCount++;
					return;
				}

				//duplicate a small tree
				Fp1 = new SimpleNode(Fp.id);
				Fp1.setImage(Fp.getImage());
				Fp1.parent = null;
				if(Fp.children != null)
					for(int dup = 0; dup < Fp.children.length; dup++)
						duplicate(Fp1, (SimpleNode)Fp.children[dup], dup);

				//replace the function real parameter
				System.out.println
				(
					PL.size()
						+ "*****"
						+ ((SimpleNode)PL.elementAt(0)).id
						+ "//"
						+ ((SimpleNode)PL.elementAt(1)).id
				);

				replace(Fp1, Dim, PL);

				//replace the function
				if(!(Fp1.toString().trim().equals("WHERE")))
				{ //isn't where
					Fp1.parent = begin.parent;
					((SimpleNode)begin.parent).children[Fcn] = Fp1;
				}
				else
				{
					((SimpleNode)Fp1.children[0]).parent = begin.parent;
					((SimpleNode)begin.parent).children[Fcn] =
						(SimpleNode)Fp1.children[0];

					while(IS)
					{
						if((Fparent.toString().trim().equals("ASSIGN"))
							|| (Fparent.toString().equals("WHERE")))
							IS = false;
						else
							if(Fparent.parent == null)
							{
								System.err.println(
									"Semantic error: Error AST structure.");
								iErrorCount++;
								return;
							}
							else
								Fparent = (SimpleNode)Fparent.parent;
					}

					if(Fparent.toString().trim().equals("ASSIGN"))
					{
						((SimpleNode)Fparent.children[1]).parent = Fp1;
						Fp1.children[0] = (SimpleNode)Fparent.children[1];

						Fp1.parent = Fparent;
						Fparent.children[1] = Fp1;
					}
					if(Fparent.toString().trim().equals("WHERE"))
					{
						p1 = Fparent.children.length;
						p2 = Fp1.children.length;
						for(int p3 = 1; p3 < p2; p3++)
						{
							((SimpleNode)Fp1.children[p3]).parent = Fparent;
							Fparent.jjtAddChild(
								(SimpleNode)Fp1.children[p3],
								p1 + p3 - 1);
						}
					}
				}
			}
			else
			{
				System.err.println("Semantic error: No such function.");
				iErrorCount++;
				return;
			}
		}
		else
		{
			if(begin.children != null)
				for(int r = 0; r < begin.children.length; r++)
				{
					Ftemp1 = (SimpleNode)begin.children[r];
					if(Ftemp1 != null)
						eliminateFunction(Ftemp1, r);
				}
		}
	}

	/**
	 * Duplicates a tree.
	 *
	 * @param root Entry SimpleNode of the new tree.
	 * @param Ori_tree  Entry SimpleNode of the old tree duplicated.
	 * @param Child_Num  integer type and the Child_Num-th child.
	 *
	 * TODO: move to AbstractSyntaxTree
	 */
	public static void duplicate
	(
		SimpleNode root,
		SimpleNode Ori_tree,
		int Child_Num
	)
	{
		SimpleNode Dup_tree, Dup_root;

		Dup_tree = new SimpleNode(Ori_tree.id); //= node
		Dup_tree.setImage(Ori_tree.getImage());
		Dup_tree.parent = root;
		root.jjtAddChild(Dup_tree, Child_Num);

		if(Ori_tree.children != null)
		{
			for(int r = 0; r < Ori_tree.children.length; r++)
				duplicate
				(
					(SimpleNode)root.children[Child_Num],
					(SimpleNode)Ori_tree.children[r],
					r
				);
		}

	}

	/* (non-Javadoc)
	 * @see gipsy.GIPC.intensional.SIPL.ForensicLucid.ISemanticAnalyzer#replace(gipsy.GIPC.intensional.SimpleNode, java.lang.String[], java.util.Vector)
	 */
	@Override
	public void replace(SimpleNode ReTree, String dim[], Vector<SimpleNode> pl)
	{
		SimpleNode Rep, temp_p;
		int loc = 0;

		if(ReTree.children == null)
		{
			if((ReTree.toString().trim().equals("DIMENSION"))
				&& (ReTree.getImage().charAt(0) == '#'))
			{
				loc = Integer.parseInt(ReTree.getImage().substring(1));
				ReTree.setImage(dim[loc]);
			}
			else
				if((ReTree.toString().substring(0,ReTree.toString().indexOf(":")).trim().equals("ID"))
					&& (ReTree.getImage().charAt(0) == '~'))
				{
					loc = Integer.parseInt(ReTree.getImage().substring(1));
					temp_p = (SimpleNode)pl.elementAt(loc);
					temp_p.parent = ReTree.parent;
					ReTree.id = temp_p.id;
					ReTree.setImage(temp_p.getImage());
					ReTree.children = temp_p.children;
					ReTree.type = temp_p.type;
					ReTree.ID = temp_p.ID;
					ReTree.setRank(temp_p.getRank());
				}
		}
		else
		{
			for(int r = 0; r < ReTree.children.length; r++)
				replace((SimpleNode)ReTree.children[r], Dim, PL);
		}
	}

	/* (non-Javadoc)
	 * @see gipsy.GIPC.intensional.SIPL.ForensicLucid.ISemanticAnalyzer#traverseTree(gipsy.GIPC.intensional.SimpleNode)
	 */
	@Override
	public void traverseTree(SimpleNode begin)
	{
		SimpleNode n, sub_begin, sub_node, nParent, temp_node2;
		DictionaryItem p_item;
		int beginChildNumber, g_count, nParentChildNumber;
		boolean Con = true;
		boolean fin_in_dim=false;

		beginChildNumber = begin.children.length;			
		
		//START OF TRAVERSING WHERE
		if (begin.toString().trim().equals("WHERE"))
		{
			n = (SimpleNode)begin.parent;
			
			//START OF TRAVERSING ASSIGN NODES, which is the parent of WHERE
			if(n.toString().trim().equals("ASSIGN"))
			{
				p_name = ((SimpleNode)n.children[0]).getImage();
				p_entry = (SimpleNode)begin.children[0];
				((SimpleNode)n.children[0]).ID = iCount;
				p_kind = "identifier";
				if(((SimpleNode)n.children[1]).children == null)
					p_type = ((SimpleNode)n.children[1]).type;
				else
					p_type = -1;

				p_item =
					new DictionaryItem
					(
						iCount,
						p_name,
						p_kind,
						p_type,
						"",
						p_entry,
						previous,
						false
					);
				oDictionary.addElement(p_item);
				
				nParent = (SimpleNode)n.parent;
				nParentChildNumber = nParent.children.length;
				sub_begin = (SimpleNode)nParent.children[0];
				
				//SubTree2 is called when the begin node is WHERE and the parent of begin is ASSIGN(it actually means that there is a sub-scope embedded inside)it writes the ID to the first child of where
				/**
				 * X 
				 *  where
				 *     X=Y+1
				 *       where
				 *         Y=3;
				 *       end;
				 *   end
				 */
				SubTree2(sub_begin, p_name, iCount);
				
				int tt1 = 1;

				while((Con) && (tt1 < nParentChildNumber))
				{
					temp_node2 = (SimpleNode)nParent.children[tt1];
					
					//START OF TRAVERSING ASSIGN NODE
					if(temp_node2.toString().trim().equals("ASSIGN"))
					{
						if(((SimpleNode)temp_node2.children[0]).ID == iCount)
								Con = false;
						else
						{
							temp_node2 = (SimpleNode)temp_node2.children[1];
							SubTree5(temp_node2, iCount);
							tt1++;
						}
					}//END OF TRAVERSING ASSIGN NODE
					
					else
						tt1++;
				}

				iCount++;
				previous = current; //switching the current scope
				current =
					new DictionaryItem
					(
						(iCount - 1),
						p_name,
						"",
						-1,
						"",
						null,
						previous,
						true
					);				
				current.getHashtable().put(current.getName(), current);
				sub_begin = (SimpleNode)begin.children[0];			
				//This is called to begin 
				SubTree1(sub_begin, current);
				g_count = 1;			
				
				//START TRAVERSING THE CHILDREN OF 'begin'
				n = (SimpleNode)begin.children[g_count];
				
				while((n.toString().trim().equals("DIMENSION"))&&(!fin_in_dim))
				{
					for(int gg = 0; gg < n.children.length; gg++)
					{
						sub_node = (SimpleNode)n.children[gg];						
						
						if(conflict(sub_node.getImage()))
						{
							System.err.println
							(
								"Semantic error: "
									+ sub_node.getImage()
									+ " conflict."
							);

							iErrorCount++;
						}

						p_kind = "dimension";
						p_name = sub_node.getImage();
						((SimpleNode)n.children[gg]).ID = iCount;
						//set the entry address(the reference to the tree) of a dimension variable, which is the dimension SimpleNode itself
						p_entry = n;

						p_item =
							new DictionaryItem
							(
								iCount,
								p_name,
								p_kind,
								2,
								"",
								p_entry,
								null,
								false
							);

						current.getHashtable().put(sub_node.getImage(), p_item);
						oDictionary.addElement(p_item);
						iCount++;
					}

					g_count++;					
					if (g_count<begin.children.length) n = (SimpleNode)begin.children[g_count];
					else fin_in_dim=true;						
				}
				//delete the current scope
				
				if (g_count<begin.children.length)
				{	
				  for(int g = g_count; g < begin.children.length; g++)
				  {
					n = (SimpleNode)begin.children[g];
					if (n.toString().trim().equals("ASSIGN"))
					{ //equal
					//Now n is assign, then n.children[1] could be the actual value assigned to the variable or another where clause, meaning that there's another expression in this assign expression(such as X=Y+m)
					//And if the n.children[1] is not where, then we can write the type information etc into the Dictionary.
						if(!(((SimpleNode)n.children[1]).toString().trim().equals("WHERE")))
						{
							p_name = ((SimpleNode)n.children[0]).getImage();
							p_kind = "identifier";
							((SimpleNode)n.children[0]).ID = iCount;
							if(((SimpleNode)n.children[1]).children == null)
								p_type = ((SimpleNode)n.children[1]).type;
							else
								p_type = -1;
							p_entry = (SimpleNode)n.children[1];
							p_item =
								new DictionaryItem(
									iCount,
									p_name,
									p_kind,
									p_type,
									"",
									p_entry,
									previous,
									false);
							oDictionary.addElement(p_item);
							SubTree2(sub_begin, p_name, iCount);
							for(int gg2 = 1; gg2 < g; gg2++)
							{
								nParent = (SimpleNode)begin.children[gg2];
								if(nParent.toString().trim().equals("ASSIGN"))
								{
									nParent =
										(SimpleNode)nParent.children[1];
									SubTree5(nParent, iCount);
								}
							}
							iCount++;
							SubTree4((SimpleNode)n.children[1], current);
							//write the ID to the right tree;
						}
						if((!(n.toString().trim().equals("FUN"))) && (n.children != null))
							traverseTree(n);
						else
							if (!(n.toString().trim().equals("FUN")))
							{
								n.setRank(""); //the rank of Const;
								oSemanticStack.push(n); //push the leave
							}
					}
					else
						if (!(n.toString().trim().equals("FUN")))
						//if(n.id != JJTFUN)
							System.err.println("Semantic Analyzer: erroneous structure of the AST.");
				  }
			  }

				current = current.getPrevious(); //delete the current scope;
			}//END TRAVERSING ASSIGN NODE, which is the parent of WHERE
			
			//(n=begin.parent)!="ASSIGN"
			else 
			{
				sub_begin = (SimpleNode)begin.children[0];
				
				SubTree1(sub_begin, current); //for 'where' under condition 1
				
				for(int m = 1; m < begin.children.length; m++)
				{
					n = (SimpleNode)begin.children[m];					
					//dimension
					if (n.toString().trim().equals("DIMENSION"))
					{ 
						for(int mm = 0; mm < n.children.length; mm++)
						{
							sub_node = (SimpleNode)n.children[mm];
							if(sub_node.toString().contains("ID"))
							{
								if(conflict(sub_node.getImage())) //Here the dimension children have changed. Before it was just bunch of IDs attached, now it has tag set nodes under, so the getImage() method has nothing returned.
								{
									System.err.println(
										"Semantic error: "
											+ sub_node.getImage()
											+ " conflict.");
									iErrorCount++;
								}
								
								p_kind = "dimension";
								p_name = sub_node.getImage();
								((SimpleNode)n.children[mm]).ID = iCount;
								//set the entry address(the reference to the tree) of a dimension variable, which is the dimension SimpleNode itself
								p_entry=n;
								p_item =
									new DictionaryItem(
										iCount,
										p_name,
										p_kind,
										2,
										"",
										p_entry,
										null,
										false);
								current.getHashtable().put(
									sub_node.getImage(),
									p_item);
								oDictionary.addElement(p_item);
								iCount++;
								
								
								oSemanticStack.push(n.children[mm]);
								
							}
							
						}
					}
					else
						if (n.toString().trim().equals("ASSIGN")) 
						{ //equal
							if(!(((SimpleNode)n.children[1]).toString().trim().equals("WHERE")))
								//not where
							{
								p_name = ((SimpleNode)n.children[0]).getImage();
								p_kind = "identifier";
								p_entry = (SimpleNode)n.children[1];
								((SimpleNode)n.children[0]).ID = iCount;
								if(((SimpleNode)n.children[1]).children
									== null)
									p_type = ((SimpleNode)n.children[1]).type;
								else
									p_type = -1;
								p_item =
									new DictionaryItem(
										iCount,
										p_name,
										p_kind,
										p_type,
										"",
										p_entry,
										previous,
										false);
								oDictionary.addElement(p_item);
								SubTree2(sub_begin, p_name, iCount);
								for(int mm2 = 1; mm2 < m; mm2++)
								{
									nParent = (SimpleNode)begin.children[mm2];
									if (nParent.toString().trim().equals("ASSIGN"))
									//if(temp_node.id == JJTASSIGN)
									{
										nParent =
											(SimpleNode)nParent.children[1];
										SubTree5(nParent, iCount);
									}
								}
								iCount++;
								
								SubTree4((SimpleNode)n.children[1], current);
								//write the ID to the right tree;
							}
							if((!(n.toString().trim().equals("FUN"))) && (n.children != null))						
								traverseTree(n);							
							else
								if(!(n.toString().trim().equals("FUN")))
								{
									n.setRank(""); //the rank of Const;
									oSemanticStack.push(n); //push the leave
								}
						}
						else
							if(!(n.toString().trim().equals("FUN")))
								System.err.println(
									"error structure of the Tree.");
				}
			 }
		}//END OF TRAVERSING WHERE NODE 
		//finish the first path(set up the dictionary)
		
		
		else
		{
			for(int i = 0; i < beginChildNumber; i++)
			{
				n = (SimpleNode)begin.children[i];
				if((!(n.toString().trim().equals("FUN"))) && (n.children != null))
					traverseTree(n);				
				
				//Indicating that it is a leave node
				else
					if(!(n.toString().trim().equals("FUN")))
					{
						n.setRank(""); //the rank of Const;
						oSemanticStack.push(n); //push the leave
					}
			}
		}
	}

	/**
	 *  Writes the ID to the first child of where node, does semantic checking on scope.
	 *
	 *  If the identifier is defined, checks the scope, writes the ID to the left branch.
	 *  If the identifier is new, generates the ID for the identifier, writes information
	 *  to the dictionary, writes the ID to the left branch.
	 *
	 *  @param tree1    entry SimpleNode of the left branch.
	 *  @param C_scope  the DictionaryItem type, used to do scope checking.
	 */
	public void SubTree1(SimpleNode tree1, DictionaryItem C_scope)
	{
		DictionaryItem T1_item, T2_item, P_scope;
		SimpleNode T1_node;
		boolean IsNew = true;

		
		if(tree1.children == null)
		{
			tree1.setRank(""); //the rank of Const;
			oSemanticStack.push(tree1);			//push the leave of the first child of where
			//is an identifier
			if(tree1.toString().substring(0,tree1.toString().indexOf(":")).trim().equals("ID"))
			{ 
				T1_node = (SimpleNode)tree1.parent;
				if((T1_node.toString().equals("HASH"))       //Here I added 'CONTEXT_ELEMENT'
					|| ((T1_node.toString().equals("AT")) && (T1_node.children[1] == tree1)) || (T1_node.toString().contains("CONTEXT_ELEMENT")))
						tree1.ID = lookfor(tree1.getImage());	
				
				else
				{
					//Indicating that this ID is not new
					if((C_scope.getHashtable().containsKey(tree1.getImage()))
						|| (C_scope.getName().equals(tree1.getImage())))
					{
						IsNew = false;
						T2_item =
							(DictionaryItem)C_scope.getHashtable().get(
								tree1.getImage());
						tree1.ID = T2_item.getID();
					}
					else
					{
						//Meaning that if the identifier has been defined in the father scope, it can be used in this scope
						P_scope = C_scope.getPrevious();
						while(P_scope != null)
						{
							if((P_scope
								.getHashtable()
								.containsKey(tree1.getImage()))
								|| (P_scope.getName().equals(tree1.getImage())))
							{
								IsNew = false;
								T2_item =
									(DictionaryItem)P_scope.getHashtable().get(
										tree1.getImage());
								tree1.ID = T2_item.getID();
								P_scope = null;
							}
							else
								P_scope = P_scope.getPrevious();
						}
					}
					if(IsNew)
					{
						T1_item =
							new DictionaryItem(
								-1,
								tree1.getImage(),
								"",
								-1,
								"",
								null,
								previous,
								false);
						current.getHashtable().put(tree1.getImage(), T1_item); //Here it's just put the item into the oHashTable under the current scope, not in the oDictionary
					}
				}
			}
		}//end of if, when there is no children for nodes
		
		
		else
		{	
			if(tree1.toString().trim().equals("DIFFERENTIATE"))
			{
				SimpleNode operand1=(SimpleNode)tree1.children[0];
				SimpleNode operand2=(SimpleNode)tree1.children[1];
				if(!operand1.toString().trim().equals(operand2.toString().trim()))
				{
					System.err.println("Semantic Error: The two operands are not the same context type");
					iErrorCount++;
				}
			}
			
			for(int j = 0; j < tree1.children.length; j++)
				SubTree1(((SimpleNode)tree1.children[j]), C_scope);
		}

	}

	/**
	 *  Writes the ID to the first child of where node.
	 *
	 *  This condition happens when the right branch of assign node is a where node.
	 *  After getting the ID of new identifier definition, the ID should be written back
	 *  to the left branch of the same assign node. Makes sure that each identifier on
	 *  the tree to have ID number and to be accessed by dictionary.
	 *
	 *  @param tree2  entry SimpleNode of the left branch.
	 *  @param pIDName      the identifier's name String which will be written ID.
	 *  @param pSemanticID    the ID Integer for the identifier.
	 /**
				 * X 
				 *  where
				 *     X=Y+1
				 *       where
				 *         Y=3;
				 *       end;
				 *   end
	*/
	 
	public void SubTree2(SimpleNode tree2, String pIDName, int pSemanticID)
	{
		DictionaryItem t2_item;

		if(tree2.children == null)
		{
			if(tree2.toString().substring(0,tree2.toString().indexOf(":")).trim().equals("ID"))
			{
				if(tree2.getImage().equals(pIDName))
				{
					if((tree2.ID != 0) && (tree2.ID != -1))
					{
						System.err.println(
							"Semantic warning: "
								+ tree2.getImage()
								+ " re-defined.");
						iWarningCount++;
					}
					tree2.ID = pSemanticID;
				}
				//!contain report error
				if(current.getHashtable().containsKey(pIDName))
				{					
					t2_item = (DictionaryItem)current.getHashtable().remove(pIDName);
					t2_item.setID(pSemanticID);
					current.getHashtable().put(pIDName, t2_item);
				}
			}
		}
		else
		{
			for(int j = 0; j < tree2.children.length; j++)
				SubTree2(((SimpleNode)tree2.children[j]), pIDName, pSemanticID);
		}

	}

	/**
	 *  Writes the ID to the right branch of eaquel node.
	 *
	 *  After getting the ID of new identifier definition, the ID should be written back
	 *  to the right branch of the eaquel node. Makes sure that each identifier on
	 *  the tree to have ID number and can be accessed by dictionary.
	 *
	 *  @param tree5  entry SimpleNode of the right branch.
	 *  @param Num5   the ID Integer for the identifier.
	 */
	public void SubTree5(SimpleNode tree5, int Num5)
	{
		// write the ID to the before child of the branch
		DictionaryItem t5_item;

		if(tree5.children == null)
		{
			if(tree5.toString().substring(0,tree5.toString().indexOf(":")).trim().equals("ID"))
				if(tree5.ID == -1)
					tree5.ID = Num5;
		}
		else
		{
			for(int j = 0; j < tree5.children.length; j++)
				SubTree5(((SimpleNode)tree5.children[j]), Num5);
		}

	}

	/* (non-Javadoc)
	 * @see gipsy.GIPC.intensional.SIPL.ForensicLucid.ISemanticAnalyzer#TS(gipsy.GIPC.intensional.SimpleNode)
	 */
	@Override
	public void TS(SimpleNode T)
	{

		if(T.children == null)
		{}
		else
		{
			for(int j = 0; j < T.children.length; j++)
				TS(((SimpleNode)T.children[j]));
		}
	}

	/**
	 *  Writes the ID to the right child of where node, does semantic checking on scope.
	 *
	 *  If the identifier is defined, checks the scope, writes the ID to the right branch.
	 *  If the identifier is new, generates the ID for the identifier, writes information
	 *  to the dictionary, writes the ID to the right branch.
	 *
	 *  @param tree4          entry SimpleNode of the right branch.
	 *  @param current_scope  the DictionaryItem type, used to do scope checking.
	 */
	public void SubTree4(SimpleNode tree4, DictionaryItem current_scope)
	{
		DictionaryItem T4_item, parent_scope;
		SimpleNode T4_node;

		if(tree4.children == null)
		{

			if(tree4.toString().substring(0,tree4.toString().indexOf(":")).trim().equals("ID"))
			{ //is a identifier

				if((current_scope
					.getHashtable()
					.containsKey(tree4.getImage()))
					|| (current_scope.getName().equals(tree4.getImage())))
				{
					T4_item =
						(DictionaryItem)current_scope.getHashtable().get(
							tree4.getImage());
					tree4.ID = T4_item.getID();
				}
				else
				{
					parent_scope = current_scope.getPrevious();
					while(parent_scope != null)
					{
						if((parent_scope
							.getHashtable()
							.containsKey(tree4.getImage()))
							|| (parent_scope.getName().equals(tree4.getImage())))
						{
							T4_item =
								(DictionaryItem)parent_scope
									.getHashtable()
									.get(
									tree4.getImage());
							tree4.ID = T4_item.getID();
							parent_scope = null;
						}
						else
							parent_scope = parent_scope.getPrevious();
					}
				}
			}

		} //end of if(tree4.children==null)
		else
		{
			for(int j = 0; j < tree4.children.length; j++)
				SubTree4(((SimpleNode)tree4.children[j]), current_scope);
		}
	}

	/**
	 *  Checks if the identifier is defined.
	 *
	 *  @param tree4  identifier String.
	 *  @return  the integer position for the identifier on the dictionary.
	 */
	public int lookfor(String tree4)
	{
		int t4, t5 = 0;
		DictionaryItem temp4;

		for(t4 = 0; t4 < oDictionary.size(); t4++)
		{
			temp4 = (DictionaryItem)oDictionary.elementAt(t4);

			if(temp4.getName().equals(tree4))
			{
				t5 = t4;
				t4 = oDictionary.size();
			}
		}

		return t5;
	}

	/* (non-Javadoc)
	 * @see gipsy.GIPC.intensional.SIPL.ForensicLucid.ISemanticAnalyzer#conflict(java.lang.String)
	 */
	@Override
	public boolean conflict(String name)
	{
		boolean conflict = true;
		boolean warning = false;
		DictionaryItem parents;


		if(!current.getHashtable().containsKey(name))
			conflict = false;
		parents = current.getPrevious();
		if((!conflict) && (parents != null))
		{
			while(parents != null)
			{
				if(parents.getHashtable().containsKey(name))
					warning = true;

				if(parents.getName().equals(name))
					warning = true;

				parents = parents.getPrevious();
			}

			if(warning)
			{
				System.out.println(" Semantic Warning: " + name + " overridding.");
				iWarningCount++;
			}
		}

		return conflict;
	}

	/* (non-Javadoc)
	 * @see gipsy.GIPC.intensional.SIPL.ForensicLucid.ISemanticAnalyzer#typeCheck()
	 */
	@Override
	public void typeCheck()
	{
		SimpleNode Right, Result;
		boolean judge = false;

		while(!oSemanticStack.empty())
		{
			Right = (SimpleNode)oSemanticStack.pop(); //the rightest leaf
			Result = (SimpleNode)Right.parent;
			judge = check(Right, Result);			

			while(!judge)
			{
				Right = (SimpleNode)oSemanticStack.pop(); //the rightest leaf
				Result = (SimpleNode)Right.parent;
				judge = check(Right, Result);
			}
		}
	}

	/* (non-Javadoc)
	 * @see gipsy.GIPC.intensional.SIPL.ForensicLucid.ISemanticAnalyzer#check(gipsy.GIPC.intensional.SimpleNode, gipsy.GIPC.intensional.SimpleNode)
	 */
	@Override
	public boolean check(SimpleNode Right, SimpleNode Result)
	{
		SimpleNode Left, temp_node;
		boolean found = false, finish = false;
		DictionaryItem temp = new DictionaryItem();
		DictionaryItem temp2 = new DictionaryItem();
		String R_str, L_str = "";
		
		
		//parent is dimension or where
		if((Result.toString().trim().equals("WHERE")) || (Result.toString().trim().equals("DIMENSION")))
		{
			
			
			//Just add the situation for X is not defined in the where clause.
			/*
			 * X
                where
                   Y=43
                     where
                        X=12;
                     end;
                end
			 */
			
			
			if(Right.toString().contains("ID"))
			
			{
				if(!((DictionaryItem)oDictionary.elementAt(Right.ID)).getName().equals(Right.getImage())) //This is how to deal with both the identifier name and the scope in looking for an entry in Dictionary
				{
					System.err.println("Semantic Error: The Identifier "+Right.getImage()+" has not been defined!");	
				    iErrorCount++;
				}
			}
			
			finish = true;

		}
		
		/**
		 * Added for CONTEXT_ELEMENT
		 */
		if(Result.toString().trim().equals("CONTEXT_ELEMENT"))
		{
			//It is the identifier under context_element
			if(Right.toString().contains("ID"))
			{
				if(oDictionary.getItem(Right.getImage(), current)==null)
				{
					System.err.println("Semantic Error: The dimension Identifier "+Right.getImage()+" has not been defined!");  
				    iErrorCount++;
				}
				
			} //End of checking under context_element for ID.
			
			
			//This is the tag value under context_element
			else
			{
				checkTagValueInContext(Right);
			}
			
			finish=true;
		}
		
		
		
			
		else
			//parent is #
			if(Result.toString().trim().equals("HASH"))
			{
				Result.type = 0; //is integer
				int i = 1;
			  if (iCount==1)
			  {
				  found=false;
				  again=false;				  
			  }
			  else	
			  {
				while(i < iCount)
				{
					temp = (DictionaryItem)oDictionary.elementAt(i);
					if(temp.getName().equals(Right.getImage()))
					{
						if((!temp.getKind().equals("dimension")) && (!again))
						{
							System.out.println("Semantic error: " + temp.getName() + " is not a dimension.");
							iErrorCount++;
						}
						else
						{
							Right.ID = temp.getID();
							i = iCount + 1;
							found = true;
						}
					}
					else
						i++;
				}
			  }

				if((!found) && (!again)) //? how to deal with it better?
				{
					//for rank of #.d;
					Result.setRank(Right.ID + ",");
					System.err.println(
						" Semantic error: Dimension "
							+ Right.getImage()
							+ " is not defined.");
					iErrorCount++;
					Right = Result;
					Result = (SimpleNode)Right.parent;
					finish = check(Right, Result);
				}

				if(found)
				{
					//for rank of #.d;
					Result.setRank(Right.ID + ",");
					Right = Result;
					Result = (SimpleNode)Right.parent;
					finish = check(Right, Result);
				}
			}//finish if result is hash

			else
				 //+,-,*,/,mod
				if((Result.toString().trim().equals("MIN"))||(Result.toString().trim().equals("ADD"))||
						(Result.toString().trim().equals("MOD"))||(Result.toString().trim().equals("TIMES"))
						||(Result.toString().trim().equals("DIV")))
				{
					if((SimpleNode)Result.children[0] == Right) //condition x+......
					{
						while(!oTempStack.empty())
						{
							Left = Right;
							Right = (SimpleNode)oTempStack.pop();
							Result = (SimpleNode)Left.parent;
							
							if (!(Left.toString().indexOf(":")==-1))
							{
							if((Left.toString().substring(0,Left.toString().indexOf(":")).trim().equals("ID"))
								&& (Left.ID < oDictionary.size())
								&& (Left.ID > 0))
							{
								temp =
									(DictionaryItem)oDictionary.elementAt(
										Left.ID);
								if (temp.getType()==null) Left.type=-1;
								else Left.type = temp.getTypeEnumeration();
								if(temp.getRank() == null)
									Left.setRank("");
								else
									Left.setRank(temp.getRank());
							}
							else
								if((Left.toString().substring(0,Left.toString().indexOf(":")).trim().equals("ID")) && (!again))
								{
									System.err.println(
										" Semantic error: Identifier "
											+ Left.getImage()
											+ " is not defined.");
									iErrorCount++;
								}
							}
							
							if (!(Right.toString().indexOf(":")==-1))
							{
							if((Right.toString().substring(0,Right.toString().indexOf(":")).trim().equals("ID"))
								&& (Right.ID < oDictionary.size())
								&& (Right.ID > 0))
							{
								temp =
									(DictionaryItem)oDictionary.elementAt(
										Right.ID);
								if (temp.getType()==null) Right.type=-1;
								else Right.type = temp.getTypeEnumeration();
								if(temp.getRank() == null)
									Right.setRank("");
								else
									Right.setRank(temp.getRank());
							}
							else
								if((Right.toString().substring(0,Right.toString().indexOf(":")).trim().equals("ID")) && (!again))
								{
									System.err.println(
										" Semantic error: Identifier "
											+ Right.getImage()
											+ " is not defined.");
									iErrorCount++;
								}
							}

							if(Result.toString().trim().equals("MOD"))
							{
								Result.type = 0;
								if((!again)
									&& ((Right.type != 0) || (Left.type != 0)))
								{
									System.err.println(
										" Semantic error: MOD operand: "
											+ Right.getImage()
											+ " and "
											+ Left.getImage()
											+ " must be integer.");
									iErrorCount++;
								}
							}
							else
							{
								if((Left.type == -1) && (Right.type == -1))
								{
									again = true;
									Result.type = -1;
								}
								else
									if(Left.type == -1)
									{
										again = true;
										Result.type = Right.type;
									}
									else
										if(Right.type == -1)
										{
											again = true;
											Result.type = Left.type;
										}
										else
										{
											if(Left.type == 2)
												Result.type = Right.type;
											else
											{
												if(Left.type == Right.type)
													//think about the type is -1?
												{
													Result.type = Right.type;
												}
												else
												{
													Result.type = 1;
													//if left!=right, then result is float;
												}
											}
										}
							}

							Result.setRank(Right.getRank() + Left.getRank());
							if(Result.getRank() != "")
								Result.setRank(Op_Str(Result.getRank()));

							Right = Result;
						}
						if(((SimpleNode)Result.parent).toString().trim().equals("ASSIGN"))
							finish = true; //parent is "=",then finish;
						else //parent is logical symbol, if, at, push the result into temp stack;
							{
							temp_node = (SimpleNode)Result.parent;
							if((SimpleNode)temp_node.children[0] == Result)
								finish = check(Result, temp_node);
							else
								oTempStack.push(Result);
						}
					}

					else
						if(((SimpleNode)Result.children[0]).children == null)
							//condition: x+a*b or a+b+c ;
						{
							oSemanticStack.pop();
							Left = (SimpleNode)Result.children[0];
							//the left child

							if (!(Left.toString().indexOf(":")==-1))
							{
							     if((Left.toString().substring(0,Left.toString().indexOf(":")).trim().equals("ID"))
								    && (Left.ID < oDictionary.size())
								    && (Left.ID > 0)) //This is how to find an item in Dictionary
							     {
								     temp = (DictionaryItem)oDictionary.elementAt(Left.ID);
								     
								       if (temp.getType()==null) 
								    	   Left.type=-1;
								       else 
								    	   Left.type = temp.getTypeEnumeration();
								
								       if(temp.getRank() == null)
									      Left.setRank("");
								       else
									      Left.setRank(temp.getRank());
							     }
							     else
								
								     if((Left.toString().substring(0,Left.toString().indexOf(":")).trim().equals("ID")) && (!again))
								     {
								    	 System.err.println(" Semantic error: Identifier "+ Left.getImage()+ " is not defined.");
									     iErrorCount++;
								}
							}
							if (!(Right.toString().indexOf(":")==-1))
							{
								if((Right.toString().substring(0,Right.toString().indexOf(":")).trim().equals("ID"))
								&& (Right.ID < oDictionary.size())
								&& (Right.ID > 0))
								{
									temp =(DictionaryItem)oDictionary.elementAt(Right.ID);
									if (temp.getType()==null) 
										Right.type=-1;
									else 
										Right.type = temp.getTypeEnumeration();
								    if(temp.getRank() == null)
								    	Right.setRank("");
								    else
								    	Right.setRank(temp.getRank());
							    }
								else
									if((Right.toString().substring(0,Right.toString().indexOf(":")).trim().equals("ID")) && (!again))
									{
										System.err.println("Semantic error: Identifier "+ Right.getImage()+ " is not defined."); 
										iErrorCount++;
									}
							}

							
							if(Result.toString().trim().equals("MOD"))
							{
								Result.type = 0;
								if((!again)
									&& ((Right.type != 0) || (Left.type != 0)))
								{
									System.err.println(
										" Semantic error: MOD operand: "
											+ Right.getImage()
											+ " and "
											+ Left.getImage()
											+ " must be integer.");
									iErrorCount++;
								}
							}
							else
							{
								if((Left.type == -1) && (Right.type == -1))
								{
									again = true;
									Result.type = -1;
								}
								else
									if(Left.type == -1)
									{
										again = true;
										Result.type = Right.type;
									}
									else
										if(Right.type == -1)
										{
											again = true;
											Result.type = Left.type;
										}
										else
										{
											if(Left.type == 2)
												Result.type = Right.type;
											else
											{
												if(Left.type == Right.type)
													//think about the type is -1?
												{
													Result.type = Right.type;
												}
												else
												{
													Result.type = 1;
													//if left!=right, then result is float;
												}
											}
										}
							}

							Result.setRank(Right.getRank() + Left.getRank());
							if(Result.getRank() != "")
								Result.setRank(Op_Str(Result.getRank()));

							
							if(((SimpleNode)Result.parent).toString().trim().equals("WHERE"))
								finish = true; //the expression before while
							else
							{
								if((((SimpleNode)Result.parent).children[0])== Result)
								{
									while(!oTempStack.empty())
									{
										Left = Result;
										Right = (SimpleNode)oTempStack.pop();
										Result = (SimpleNode)Left.parent;
										if (!(Left.toString().indexOf(":")==-1))
										{
										
										if((Left.toString().substring(0,Left.toString().indexOf(":")).trim().equals("ID"))
											&& (Left.ID < oDictionary.size())
											&& (Left.ID > 0))
										{
											temp =
												(
													DictionaryItem)oDictionary
														.elementAt(
													Left.ID);
											if (temp.getType()==null) 
												Left.type=-1;
											else 
												Left.type = temp.getTypeEnumeration();
											if(temp.getRank() == null)
												Left.setRank("");
											else
												Left.setRank(temp.getRank());
										}
										else
											
											if((Left.toString().substring(0,Left.toString().indexOf(":")).trim().equals("ID")) && (!again))
											{
												System.err.println(
													" Semantic error: Identifier "
														+ Left.getImage()
														+ " is not defined.");
												iErrorCount++;
											}
										}

										if (!(Right.toString().indexOf(":")==-1))
										{
											if((Right.toString().substring(0,Right.toString().indexOf(":")).trim().equals("ID"))
											&& (Right.ID < oDictionary.size())
											&& (Right.ID > 0))
											{
												temp =(DictionaryItem)oDictionary.elementAt(Right.ID);
												if (temp.getType()==null) 
													Right.type=-1;
												else 
													Right.type = temp.getTypeEnumeration();
											if(temp.getRank() == null)
												Right.setRank("");
											else
												Right.setRank(temp.getRank());
										}
										else
											
											if((Right.toString().substring(0,Right.toString().indexOf(":")).trim().equals("ID"))
												&& (!again))
											{
												System.err.println(" Semantic error: Identifier "+ Right.getImage()+ " is not defined.");
												iErrorCount++;
											}
										}

										
										if(Result.toString().trim().equals("MOD"))
										{
											Result.type = 0;
											if((!again)
												&& ((Right.type != 0)
													|| (Left.type != 0)))
											{
												System.err.println(
													"Semantic error: MOD operand: "
														+ Right.getImage()
														+ " and "
														+ Left.getImage()
														+ " must be integer.");
												iErrorCount++;
											}
										}
										else
										{
											if((Left.type == -1)
												&& (Right.type == -1))
											{
												again = true;
												Result.type = -1;
											}
											else
												if(Left.type == -1)
												{
													again = true;
													Result.type = Right.type;
												}
												else
													if(Right.type == -1)
													{
														again = true;
														Result.type = Left.type;
													}
													else
													{
														if(Left.type == 2)
															Result.type =
																Right.type;
														else
														{
															if(Left.type
																== Right.type)
																//think about the type is -1?
															{
																Result.type =
																	Right.type;
															}
															else
															{
																Result.type = 1;
																//if left!=right, then result is float;
															}
														}
													}
										}

										Result.setRank(
											Right.getRank() + Left.getRank());
										if(Result.getRank() != "")
											Result.setRank(
												Op_Str(Result.getRank()));
										//for rank[ A + - * / mod B ];
									}

									
									if(((SimpleNode)Result.parent).toString().trim().equals("ASSIGN"))
										finish = true;
									//parent is "=",then finish;
									else //parent is logical symbol, if, at, push the result into temp stack;
										{
										temp_node = (SimpleNode)Result.parent;
										if((SimpleNode)temp_node.children[0]
											== Result)
											finish = check(Result, temp_node);
										else
											oTempStack.push(Result);
									}
								}
								else
								{
									oTempStack.push(Result);
								}
							}
						}
						else
							oTempStack.push(Right);
				}

				else
					// operator is 'not';
					if(Result.toString().trim().equals("NOT"))
					{
						Result.type = 3;

						if(Right.getRank() == null)
							Result.setRank("");
						else
							Result.setRank(Right.getRank());
						//for rank [ not A ];

						
						if(((SimpleNode)Result.parent).toString().trim().equals("ASSIGN"))
							finish = true; //parent is "=",then finish;
						else //parent is logical symbol, if, at, push the result into temp stack;
							{
							temp_node = (SimpleNode)Result.parent;
							if((SimpleNode)temp_node.children[0] == Result)
								finish = check(Result, temp_node);
							else
								oTempStack.push(Result);
						}
					}

					else
						
							//logical operator: <,>,<=,>=,==,!=, and, or;
						if((Result.toString().trim().equals("OR"))||(Result.toString().trim().equals("LT"))||
						   (Result.toString().trim().equals("GT"))||(Result.toString().trim().equals("GE"))||
						   (Result.toString().trim().equals("LE"))||(Result.toString().trim().equals("EQ"))||
						   (Result.toString().trim().equals("NE"))||(Result.toString().trim().equals("AND")))
						{
							if((SimpleNode)Result.children[0] == Right)
								//condition x+......
							{
								Left = Right;
								Right = (SimpleNode)oTempStack.pop();
								Result = (SimpleNode)Left.parent;
								Result.type = 3;

								Result.setRank(
									Right.getRank() + Left.getRank());
								if(Result == null)
									Result.setRank("");
								if(Result.getRank() != "")
									Result.setRank(Op_Str(Result.getRank()));
								//for rank[ A == ... and B]

								if((Left.type != Right.type)
									&& (Right.type != -1)
									&& (!again))
								{
									System.err.println(
										" Semantic error: "
											+ Left.getImage()
											+ " and "
											+ Right.getImage()
											+ " type does not match.");
									iErrorCount++;
								}
								if(((SimpleNode)Result.parent).toString().trim().equals("ASSIGN"))
									finish = true; //parent is "=",then finish;
								else //parent is logical symbol, if, at, push the result into temp stack;
									{
									temp_node = (SimpleNode)Result.parent;
									if((SimpleNode)temp_node.children[0]
										== Result)
										finish = check(Result, temp_node);
									else
										oTempStack.push(Result);
								}
							}
							else
								if(((SimpleNode)Result.children[0]).children
									== null)
									//condition: x+a*b or a+b+c ;
								{
									oSemanticStack.pop();
									Left = (SimpleNode)Result.children[0];
									//the left child
									Result.type = 3;

									Result.setRank(
										Right.getRank() + Left.getRank());
									if(Result.getRank() == null)
										Result.setRank("");
									if(Result.getRank() != "")
										Result.setRank(
											Op_Str(Result.getRank()));
									//for rank [A == ... and B]

									if((Left.type != Right.type)
										&& (Right.type != -1)
										&& (!again))
									{
										System.err.println(
											" Semantic error: "
												+ Left.getImage()
												+ " and "
												+ Right.getImage()
												+ " type does not match.");
										iErrorCount++;
									}
									if(((SimpleNode)Result.parent).toString().trim().equals("ASSIGN"))
										finish = true;
									//parent is "=",then finish;
									else //parent is logical symbol, if, at, push the result into temp stack;
										{
										temp_node = (SimpleNode)Result.parent;
										if((SimpleNode)temp_node.children[0]
											== Result)
											finish = check(Result, temp_node);
										else
											oTempStack.push(Result);
									}
								}
								else
									oTempStack.push(Right);
						}

						else
							 //parent is 'if'
							if(Result.toString().trim().equals("IF"))
							{
								if((SimpleNode)Result.children[0] == Right) //
								{
									if((Right.type != 3) && (!again))
									{
										System.err.println(
											" Semantic error: "
												+ Right.getImage()
												+ " is not condition.");
										iErrorCount++;
									}
									temp_node = Right;
									if(oTempStack.size() < 2)
										finish = true;
									else
									{
										Left = (SimpleNode)oTempStack.pop();
										Right = (SimpleNode)oTempStack.pop();
										Result = (SimpleNode)Left.parent;

										if (!(Left.toString().indexOf(":")==-1))
										{
										
										if(Left.toString().substring(0,Left.toString().indexOf(":")).trim().equals("ID"))
										{
											temp =
												(
													DictionaryItem)oDictionary
														.elementAt(
													Left.ID);
											if (temp.getType()==null) Left.type=-1;
											else Left.type = temp.getTypeEnumeration();
											if(temp.getRank() == null)
												Left.setRank("");
											else
												Left.setRank(temp.getRank());
										}
										}

										if (!(Right.toString().indexOf(":")==-1))
										{
										
										if(Right.toString().substring(0,Right.toString().indexOf(":")).trim().equals("ID"))
										{
											temp =
												(
													DictionaryItem)oDictionary
														.elementAt(
													Right.ID);
											if (temp.getType()==null) Right.type=-1;
											else Right.type = temp.getTypeEnumeration();
											if(temp.getRank() == null)
												Right.setRank("");
											else
												Right.setRank(temp.getRank());
										}
										}

										Result.setRank(
											temp_node.getRank()
												+ Right.getRank()
												+ Left.getRank());
										//for rank[ if statement ]
										if(Result.getRank() == null)
											Result.setRank("");
										if(Result.getRank() != "")
											Result.setRank(
												Op_Str(Result.getRank()));

										if((Left.type != Right.type)
											&& (Right.type != -1))
										{
											Result.type = Left.type;
											//type don't match, give float type to if statement
											if(!again)
											{
												System.err.println(
													" Semantic error: If Statement's type do not match.");
												iErrorCount++;
											}
										}
										else
										{
											Result.type = Left.type;
										}
										//if(((SimpleNode)Result.parent).id== JJTASSIGN)
										if(((SimpleNode)Result.parent).toString().trim().equals("ASSIGN"))
											finish = true;
										//parent is "=",then finish;
										else //parent is logical symbol, if, at, push the result into temp stack;
											{
											temp_node =
												(SimpleNode)Result.parent;
											if((SimpleNode)temp_node
												.children[0]
												== Result)
												finish =
													check(Result, temp_node);
											else
												oTempStack.push(Result);
										}
									}
								}
								else
									oTempStack.push(Right);
							}

							else
								//parent is 'at'
								
								
								if(Result.toString().trim().equals("AT"))
								{
									//Added identifier defined checking in AT
									if(Right.toString().contains("ID"))
									{
										if(!oDictionary.isInDictionary(Right.getImage()))
										{
											System.err.println("Semantic Error: The Identifier "+Right.getImage()+" has not been defined!");  
										}
										finish=true;
									}
									if((SimpleNode)Result.children[0]
										== Right)
										//
									{
										// add for rank [A @ .d N]
										if(Right.getRank() == null)
											Right.setRank("");
										if(Right.getRank() == "")
											Result.setRank("");
										else
										{
											Left =
												(SimpleNode)Result.children[1];
											R_str = Right.getRank();
											//L_str=Left.getImage();
											L_str = Integer.toString(Left.ID);
											int E_val = R_str.indexOf(L_str);
											if(E_val != -1)
											{
												if((R_str.length()
													- L_str.length())
													> 1)
													R_str =
														R_str.substring(
															0,
															E_val)
															+ R_str.substring(
																E_val
																	+ L_str
																		.length()
																	+ 2);
											}
											Result.setRank(R_str);
										}
										if(oTempStack.empty())
											finish = true;
										else
										{
											Right = (SimpleNode)oTempStack.pop();
											Result.type = Right.type;
											//if(((SimpleNode)Result.parent).id== JJTASSIGN)
											if(((SimpleNode)Result.parent).toString().trim().equals("ASSIGN"))
												finish = true;
											//parent is "=",then finish;
											else //parent is logical symbol, if, at, push the result into temp stack;
												{
												temp_node =
													(SimpleNode)Result.parent;
												if((SimpleNode)temp_node
													.children[0]
													== Result)
													finish =
														check(
															Result,
															temp_node);
												else
													oTempStack.push(Result);
											}
										}
									}
									else
										if((SimpleNode)Result.children[1]
											== Right)
										{
											int i2 = 1;
										  if (iCount==1)
										  {
											  found = false;
											  again = false;											  
										  }
										  else
										  {
											while(i2 < iCount)
											{
												temp =
													(
														DictionaryItem)oDictionary
															.elementAt(
														i2);
												if(temp
													.getName()
													.equals(Right.getImage()))
												{
													if((!temp
														.getKind()
														.equals("dimension"))
														&& (!again))
													{
														System.err.println(
															" Semantic error: "
																+ temp.getName()
																+ " is not dimension.");
														iErrorCount++;
													}
													else
													{
														Right.ID = temp.getID();
														i2 = iCount + 1;
														found = true;
													}
												}
												else
													i2++;
											}
										  }
											if((!found) && (!again))
											{
												System.err.println(
													" Semantic error: Dimension "
														+ Right.getImage()
														+ " is not defined.");
												iErrorCount++;
											}
										}
										else
											if((SimpleNode)Result.children[2]
												== Right)
											{
												finish = true;
											}
								}

								else
									//if(Result.id == JJTASSIGN) //parent is "="
									if(Result.toString().trim().equals("ASSIGN"))
									{
										if(((SimpleNode)Result.children[1]) == Right)
											//the type x=3;
										{
											if(!oSemanticStack.empty())
												Left = (SimpleNode)oSemanticStack.pop();
											//the left child
											else
											{
												finish = true;
												return finish;
											}
										}
										else
										{
											Left = Right;

											if((Left.ID < oDictionary.size()) && (Left.ID > 0))
											{
												temp = (DictionaryItem)oDictionary.elementAt(Left.ID);
												Right = temp.getEntry();
											}
											else
											{
												finish = true;
												return finish;
											}
										}
                                 
										((SimpleNode)Result.children[0]).type = Right.type; //This is to assign the type to the Identifier

										if((Left.ID < oDictionary.size()) && (Left.ID > 0))
											temp = (DictionaryItem)oDictionary.elementAt(Left.ID); //Is this the way to look up sth in a Dictionary?
										else
										{
											finish = true;
											return finish;
										}

										if (!(Right.toString().indexOf(":")==-1))
										{
										if(Right.toString().substring(0,Right.toString().indexOf(":")).trim().equals("ID"))
										{
											if((Right.ID > 0) && (Right.ID < oDictionary.size()))
												temp2 = (DictionaryItem)oDictionary.elementAt(Right.ID);
											else
											{
												finish = true;
												return finish;
											}

											Right.type = temp2.getTypeEnumeration();
											Right.setRank(temp2.getRank());
										}
										}

										temp.setType(Right.type);
										temp.setRank(Right.getRank());
										//for rank [X=...]

										if(temp.getRank() == null)
											temp.setRank("");

										if(temp.getRank() != "")
											temp.setRank(Op_Str(temp.getRank()));

										oDictionary.setElementAt(temp, Left.ID);
										finish = true;
									}//finish if "assign"
									else if (Result.toString().trim().equals("START"))
									//this is a special case for if reaching "start" some IPL code doesn't have declaration.
									{
										finish = true;
										again = false;
									}
										

		return finish;

	}

	/* (non-Javadoc)
	 * @see gipsy.GIPC.intensional.SIPL.ForensicLucid.ISemanticAnalyzer#Op_Str(java.lang.String)
	 */
	@Override
	public String Op_Str(String str)
	{
		String temp_str, First_str, Last_str = "";
		int len = 0;

		while(str.length() != 0)
		{
			if(str.indexOf(',') != -1)
			{
				temp_str = str.substring(0, str.indexOf(','));
				len = temp_str.length() + 1;
				str = str.substring(len);

				if(str.length() == 0)
					Last_str = Last_str + temp_str + ",";
				else
					if(str.indexOf(temp_str) == -1)
						Last_str = Last_str + temp_str + ",";
			}
			else
			{
				Last_str = Last_str + "";
				str = "";
			}
		}

		return Last_str;
	}

	/* (non-Javadoc)
	 * @see gipsy.GIPC.intensional.SIPL.ForensicLucid.ISemanticAnalyzer#rankAnalyze(gipsy.GIPC.intensional.SimpleNode)
	 */
	@Override
	public void rankAnalyze(SimpleNode poRoot)
	{
		traverseTree(poRoot);
	}

	/* (non-Javadoc)
	 * @see gipsy.GIPC.intensional.SIPL.ForensicLucid.ISemanticAnalyzer#getErrorCount()
	 */
	@Override
	public int getErrorCount()
	{
		return this.iErrorCount;
	}

	/* (non-Javadoc)
	 * @see gipsy.GIPC.intensional.SIPL.ForensicLucid.ISemanticAnalyzer#getWarningCount()
	 */
	@Override
	public int getWarningCount()
	{
		return this.iWarningCount;
	}
	
	/**
	 * This is the type checking method designed for checking valid tags inside a context expression.
	 * For the possible type for tag values, now we only consider GIPSYInteger, GIPSYString and GIPSYIdentifier
	 * @param pTagValue
	 */
	public void checkTagValueInContext(SimpleNode pTagValue)
	{

		String dimensionID=((SimpleNode)pTagValue.parent.jjtGetChild(0)).getImage(); //The first child of CONTEXT_ELEMENT should be the dimension identifier
	    DictionaryItem dimensionEntry=oDictionary.getItem(dimensionID, current); //looking up in the Dictionary and returning the DictionaryItem under dimensionID
	    SimpleNode tagSetEntry=(SimpleNode)dimensionEntry.getEntry().children[dimensionEntry.getEntry().children.length-1]; //Tag set node is always the last element under dimension node
	    TagSet oTagSet;
	    GIPSYType oTagValue=new GIPSYInteger(); //By default it is GIPSYInteger
	    
	    if(pTagValue.toString().trim().contains("INTEGER")|| pTagValue.toString().trim().contains("ID")||pTagValue.toString().trim().contains("STRING"))
	    {
	    	if(pTagValue.toString().trim().contains("INTEGER"))
	    	{
		    	oTagValue=new GIPSYInteger(Integer.parseInt(pTagValue.getImage()));	    		
	    	}
	    	if(pTagValue.toString().trim().contains("ID"))
	    	{
	    		oTagValue=new GIPSYIdentifier(pTagValue.getImage());
	    	}
	    	if(pTagValue.toString().trim().contains("STRING"))
	    	{
	    		oTagValue=new GIPSYString(pTagValue.getImage());
	    	}
	    }
	    else
	    {
	    	oTagValue=new GIPSYObject();
	    	System.out.println("Warning: Now we don't have tag types other than GIPSYInteger, GIPSYString and GIPSYIndentifier!");
	        iWarningCount++;
	    }
	    
	    if(tagSetEntry.toString().trim().equals("ORDEREDFINITEPERIODICTAGSET"))
	    {
	    	SimpleNode periodEntry=(SimpleNode)tagSetEntry.children[0];//This is the period under ORDEREDFINITEPERIODICTAGSET node
	    	SimpleNode timesEntry=(SimpleNode)tagSetEntry.children[1];
	    	FreeVector<GIPSYType> period=new FreeVector<GIPSYType>();
	    	for(int i=0; i<periodEntry.children.length; i++)
	    	{
	    		if(periodEntry.children[i].toString().contains("INTEGER") && oTagValue instanceof GIPSYInteger||
	    		   periodEntry.children[i].toString().contains("ID") && oTagValue instanceof GIPSYIdentifier||
	    		   periodEntry.children[i].toString().contains("STRING") && oTagValue instanceof GIPSYString) //denoting that the tag values inside the period node is of the three types that we currently have, and the tag value inside the context should be the same as in the period
	    		{
	    			if(oTagValue instanceof GIPSYInteger)
	    			{
	    				GIPSYInteger tag=new GIPSYInteger(Integer.parseInt(((SimpleNode)periodEntry.children[i]).getImage()));
		    			period.addElement(tag);
		    		}
	    			if(oTagValue instanceof GIPSYString)
	    			{
	    				GIPSYString tag=new GIPSYString(((SimpleNode)periodEntry.children[i]).getImage());
		    			period.addElement(tag);
		    		}
	    			if(oTagValue instanceof GIPSYIdentifier)
	    			{
	    				GIPSYIdentifier tag=new GIPSYIdentifier(((SimpleNode)periodEntry.children[i]).getImage());
		    			period.addElement(tag);
		    		}
	    			
	    		}
	    		else
	    		{
	    			System.err.println("Semantic Error: The tag value is not inside the tag set1");
	    			iErrorCount++;
	    		}
	    	}
	    	int times=Integer.parseInt(((SimpleNode)timesEntry.children[0]).getImage());
	    	oTagSet =new OrderedFinitePeriodicTagSet(period, times);
	    	
	    	
	    	if(!oTagSet.isInTagSet(oTagValue))
	    	{
	    		System.err.println("Semantic Error: The tag value is not inside the tag set2");
	    		iErrorCount++;
	    	}
	    }
	    
	    
	    
	    
	    if(tagSetEntry.toString().trim().equals("ORDEREDFINITENONPERIODICTAGSET_ENUMERATED"))
	    {
	    	//SimpleNode periodEntry=(SimpleNode)tagSetEntry.children[0];//This is the period under ORDEREDFINITEPERIODICTAGSET node
	    	FreeVector<GIPSYType> enumeratedElements=new FreeVector<GIPSYType>();
	    	
	    	for(int i=0; i<tagSetEntry.children.length; i++)
	    	{
	    		if(tagSetEntry.children[i].toString().contains("INTEGER") && oTagValue instanceof GIPSYInteger||
	    		   tagSetEntry.children[i].toString().contains("ID") && oTagValue instanceof GIPSYIdentifier||
	    		   tagSetEntry.children[i].toString().contains("STRING") && oTagValue instanceof GIPSYString) //denoting that the tag values inside the period node is of the three types that we currently have, and the tag value inside the context should be the same as in the period
	    		{
	    			if(oTagValue instanceof GIPSYInteger)
	    			{
	    				GIPSYInteger tag=new GIPSYInteger(Integer.parseInt(((SimpleNode)tagSetEntry.children[i]).getImage()));
	    				enumeratedElements.addElement(tag);
		    		}
	    			if(oTagValue instanceof GIPSYString)
	    			{
	    				GIPSYString tag=new GIPSYString(((SimpleNode)tagSetEntry.children[i]).getImage());
	    				enumeratedElements.addElement(tag);
		    		}
	    			if(oTagValue instanceof GIPSYIdentifier)
	    			{
	    				GIPSYIdentifier tag=new GIPSYIdentifier(((SimpleNode)tagSetEntry.children[i]).getImage());
	    				enumeratedElements.addElement(tag);
		    		}
	    			
	    		}
	    		else
	    		{
	    			System.err.println("Semantic Error: The tag value is not inside the tag set1");
	    			iErrorCount++;
	    		}
	    	}
	    	oTagSet =new OrderedFiniteNonPeriodicTagSet(enumeratedElements);
	    	
	    	
	    	if(!oTagSet.isInTagSet(oTagValue))
	    	{
	    		System.err.println("Semantic Error: The tag value is not inside the tag set2");
	    		iErrorCount++;
	    	}
	    }
	    
	    
	    if(tagSetEntry.toString().trim().equals("ORDEREDFINITENONPERIODICTAGSET_LOWERUPPER"))
	    {
	    	SimpleNode NodeLower=(SimpleNode)tagSetEntry.children[0];
	    	SimpleNode NodeUpper=(SimpleNode)tagSetEntry.children[1];
	    	SimpleNode NodeStep=(SimpleNode)tagSetEntry.children[2];
	    	
	    	if(NodeLower.children[0].toString().contains("INTEGER") && NodeUpper.children[0].toString().contains("INTEGER") && NodeStep.children[0].toString().contains("INTEGER")&& oTagValue instanceof GIPSYInteger)
	    	{
	    		GIPSYInteger lower=new GIPSYInteger(Integer.parseInt(((SimpleNode)NodeLower.children[0]).getImage()));
	    		GIPSYInteger upper=new GIPSYInteger(Integer.parseInt(((SimpleNode)NodeUpper.children[0]).getImage()));
	    		int step=Integer.parseInt(((SimpleNode)NodeStep.children[0]).getImage());
	    		oTagSet =new OrderedFiniteNonPeriodicTagSet(lower, upper, step);
		    	
		    	
		    	if(!oTagSet.isInTagSet(oTagValue))
		    	{
		    		System.err.println("Semantic Error: The tag value is not inside the tag set2");
		    		iErrorCount++;
		    	}
		    	
	    	}
	    
	    	else
	    		{
	    			System.err.println("Semantic Error: The tag value is not inside the tag set1");
	    			iErrorCount++;
	    		}
	    }
	    
	    
	    if(tagSetEntry.toString().trim().equals("ORDEREDINFINITEPERIODICTAGSET"))
	    {
	    	SimpleNode periodEntry=(SimpleNode)tagSetEntry.children[0];//This is the period under ORDEREDFINITEPERIODICTAGSET node
	    	FreeVector<GIPSYType> period=new FreeVector<GIPSYType>();
	    	for(int i=0; i<periodEntry.children.length; i++)
	    	{
	    		if(periodEntry.children[i].toString().contains("INTEGER") && oTagValue instanceof GIPSYInteger||
	    		   periodEntry.children[i].toString().contains("ID") && oTagValue instanceof GIPSYIdentifier||
	    		   periodEntry.children[i].toString().contains("STRING") && oTagValue instanceof GIPSYString) //denoting that the tag values inside the period node is of the three types that we currently have, and the tag value inside the context should be the same as in the period
	    		{
	    			if(oTagValue instanceof GIPSYInteger)
	    			{
	    				GIPSYInteger tag=new GIPSYInteger(Integer.parseInt(((SimpleNode)periodEntry.children[i]).getImage()));
		    			period.addElement(tag);
		    		}
	    			if(oTagValue instanceof GIPSYString)
	    			{
	    				GIPSYString tag=new GIPSYString(((SimpleNode)periodEntry.children[i]).getImage());
		    			period.addElement(tag);
		    		}
	    			if(oTagValue instanceof GIPSYIdentifier)
	    			{
	    				GIPSYIdentifier tag=new GIPSYIdentifier(((SimpleNode)periodEntry.children[i]).getImage());
		    			period.addElement(tag);
		    		}
	    			
	    		}
	    		else
	    		{
	    			System.err.println("Semantic Error: The tag value is not inside the tag set1");
	    			iErrorCount++;
	    		}
	    	}
	    	//int times=Integer.parseInt(((SimpleNode)periodEntry.children[1]).getImage());
	    	oTagSet =new OrderedInfinitePeriodicTagSet(period);
	    	if(!oTagSet.isInTagSet(oTagValue))
	    	{
	    		System.err.println("Semantic Error: The tag value is not inside the tag set2");
	    		iErrorCount++;
	    	}
	    }
	    
	    if(tagSetEntry.toString().trim().equals("ORDEREDINFINITENONPERIODICTAGSET_LOWER"))
	    {
	    	SimpleNode NodeLower=(SimpleNode)tagSetEntry.children[0];
	    	
	    	SimpleNode NodeStep=(SimpleNode)tagSetEntry.children[1];
	    	
	    	if(NodeLower.children[0].toString().contains("INTEGER") && NodeStep.children[0].toString().contains("INTEGER")&& oTagValue instanceof GIPSYInteger)
	    	{
	    		GIPSYInteger lower=new GIPSYInteger(Integer.parseInt(((SimpleNode)NodeLower.children[0]).getImage()));
	    		//GIPSYInteger upper=new GIPSYInteger(Integer.parseInt(((SimpleNode)NodeUpper.children[0]).getImage()));
	    		int step=Integer.parseInt(((SimpleNode)NodeStep.children[0]).getImage());
	    		oTagSet =new OrderedInfiniteNonPeriodicTagSet(lower, step, OrderedInfiniteNonPeriodicTagSet.LOWER_STEP);
		    	
		    	
		    	if(!oTagSet.isInTagSet(oTagValue))
		    	{
		    		System.err.println("Semantic Error: The tag value is not inside the tag set2");
		    		iErrorCount++;
		    	}
		    	
	    	}
	    
	    	else
	    		{
	    			System.err.println("Semantic Error: The tag value is not inside the tag set1");
	    			iErrorCount++;
	    		}
	    }
	    
	    if(tagSetEntry.toString().trim().equals("ORDEREDINFINITENONPERIODICTAGSET_UPPER"))
	    {
	    	SimpleNode NodeUpper=(SimpleNode)tagSetEntry.children[0];
	    	
	    	SimpleNode NodeStep=(SimpleNode)tagSetEntry.children[1];
	    	
	    	if(NodeUpper.children[0].toString().contains("INTEGER") && NodeStep.children[0].toString().contains("INTEGER")&& oTagValue instanceof GIPSYInteger)
	    	{
	    		GIPSYInteger lower=new GIPSYInteger(Integer.parseInt(((SimpleNode)NodeUpper.children[0]).getImage()));
	    		//GIPSYInteger upper=new GIPSYInteger(Integer.parseInt(((SimpleNode)NodeUpper.children[0]).getImage()));
	    		int step=Integer.parseInt(((SimpleNode)NodeStep.children[0]).getImage());
	    		oTagSet =new OrderedInfiniteNonPeriodicTagSet(lower, step, OrderedInfiniteNonPeriodicTagSet.LOWER_STEP);
		    	
		    	
		    	if(!oTagSet.isInTagSet(oTagValue))
		    	{
		    		System.err.println("Semantic Error: The tag value is not inside the tag set2");
		    		iErrorCount++;
		    	}
		    	
	    	}
	    
	    	else
	    		{
	    			System.err.println("Semantic Error: The tag value is not inside the tag set1");
	    			iErrorCount++;
	    		}
	    }
	    
	    if(tagSetEntry.toString().trim().equals("ORDEREDINFINITENONPERIODICTAGSET_INFINITE") || dimensionEntry.getEntry().children.length==1) //the second condition is to denote that it is the default expression 'dimension d' which there's no children under dimension node
	    {
	    	
	    		oTagSet =new OrderedInfiniteNonPeriodicTagSet(OrderedInfiniteNonPeriodicTagSet.INFINITY);
		    	if(!oTagSet.isInTagSet(oTagValue))
		    	{
		    		System.err.println("Semantic Error: The tag value is not inside the tag set2");
		    		iErrorCount++;
		    	}
	    }
	    
	    if(tagSetEntry.toString().trim().equals("UNORDEREDFINITEPERIODICTAGSET"))
	    {
	    	SimpleNode periodEntry=(SimpleNode)tagSetEntry.children[0];//This is the period under ORDEREDFINITEPERIODICTAGSET node
	    	SimpleNode timesEntry=(SimpleNode)tagSetEntry.children[1];
	    	FreeVector<GIPSYType> period=new FreeVector<GIPSYType>();
	    	for(int i=0; i<periodEntry.children.length; i++)
	    	{
	    		if(periodEntry.children[i].toString().contains("INTEGER") && oTagValue instanceof GIPSYInteger||
	    		   periodEntry.children[i].toString().contains("ID") && oTagValue instanceof GIPSYIdentifier||
	    		   periodEntry.children[i].toString().contains("STRING") && oTagValue instanceof GIPSYString) //denoting that the tag values inside the period node is of the three types that we currently have, and the tag value inside the context should be the same as in the period
	    		{
	    			if(oTagValue instanceof GIPSYInteger)
	    			{
	    				GIPSYInteger tag=new GIPSYInteger(Integer.parseInt(((SimpleNode)periodEntry.children[i]).getImage()));
		    			period.addElement(tag);
		    		}
	    			if(oTagValue instanceof GIPSYString)
	    			{
	    				GIPSYString tag=new GIPSYString(((SimpleNode)periodEntry.children[i]).getImage());
		    			period.addElement(tag);
		    		}
	    			if(oTagValue instanceof GIPSYIdentifier)
	    			{
	    				GIPSYIdentifier tag=new GIPSYIdentifier(((SimpleNode)periodEntry.children[i]).getImage());
		    			period.addElement(tag);
		    		}
	    			
	    		}
	    		else
	    		{
	    			System.err.println("Semantic Error: The tag value is not inside the tag set1");
	    			iErrorCount++;
	    		}
	    	}
	    	int times=Integer.parseInt(((SimpleNode)timesEntry.children[0]).getImage());
	    	oTagSet =new UnorderedFinitePeriodicTagSet(period, times);
	    	
	    	
	    	if(!oTagSet.isInTagSet(oTagValue))
	    	{
	    		System.err.println("Semantic Error: The tag value is not inside the tag set2");
	    		iErrorCount++;
	    	}
	    }
	    
	    if(tagSetEntry.toString().trim().equals("UNORDEREDFINITENONPERIODICTAGSET"))
	    {
	    	//SimpleNode periodEntry=(SimpleNode)tagSetEntry.children[0];//This is the period under ORDEREDFINITEPERIODICTAGSET node
	    	FreeVector<GIPSYType> enumeratedElements=new FreeVector<GIPSYType>();
	    	
	    	for(int i=0; i<tagSetEntry.children.length; i++)
	    	{
	    		if(tagSetEntry.children[i].toString().contains("INTEGER") && oTagValue instanceof GIPSYInteger||
	    		   tagSetEntry.children[i].toString().contains("ID") && oTagValue instanceof GIPSYIdentifier||
	    		   tagSetEntry.children[i].toString().contains("STRING") && oTagValue instanceof GIPSYString) //denoting that the tag values inside the period node is of the three types that we currently have, and the tag value inside the context should be the same as in the period
	    		{
	    			if(oTagValue instanceof GIPSYInteger)
	    			{
	    				GIPSYInteger tag=new GIPSYInteger(Integer.parseInt(((SimpleNode)tagSetEntry.children[i]).getImage()));
	    				enumeratedElements.addElement(tag);
		    		}
	    			if(oTagValue instanceof GIPSYString)
	    			{
	    				GIPSYString tag=new GIPSYString(((SimpleNode)tagSetEntry.children[i]).getImage());
	    				enumeratedElements.addElement(tag);
		    		}
	    			if(oTagValue instanceof GIPSYIdentifier)
	    			{
	    				GIPSYIdentifier tag=new GIPSYIdentifier(((SimpleNode)tagSetEntry.children[i]).getImage());
	    				enumeratedElements.addElement(tag);
		    		}
	    			
	    		}
	    		else
	    		{
	    			System.err.println("Semantic Error: The tag value is not inside the tag set1");
	    			iErrorCount++;
	    		}
	    	}
	    	oTagSet =new UnorderedFiniteNonPeriodicTagSet(enumeratedElements);
	    	
	    	
	    	if(!oTagSet.isInTagSet(oTagValue))
	    	{
	    		System.err.println("Semantic Error: The tag value is not inside the tag set2");
	    		iErrorCount++;
	    	}
	    }
	    
	    if(tagSetEntry.toString().trim().equals("UNORDEREDINFINITEPERIODICTAGSET"))
	    {
	    	SimpleNode periodEntry=(SimpleNode)tagSetEntry.children[0];//This is the period under ORDEREDFINITEPERIODICTAGSET node
	    	FreeVector<GIPSYType> period=new FreeVector<GIPSYType>();
	    	for(int i=0; i<periodEntry.children.length; i++)
	    	{
	    		if(periodEntry.children[i].toString().contains("INTEGER") && oTagValue instanceof GIPSYInteger||
	    		   periodEntry.children[i].toString().contains("ID") && oTagValue instanceof GIPSYIdentifier||
	    		   periodEntry.children[i].toString().contains("STRING") && oTagValue instanceof GIPSYString) //denoting that the tag values inside the period node is of the three types that we currently have, and the tag value inside the context should be the same as in the period
	    		{
	    			if(oTagValue instanceof GIPSYInteger)
	    			{
	    				GIPSYInteger tag=new GIPSYInteger(Integer.parseInt(((SimpleNode)periodEntry.children[i]).getImage()));
		    			period.addElement(tag);
		    		}
	    			if(oTagValue instanceof GIPSYString)
	    			{
	    				GIPSYString tag=new GIPSYString(((SimpleNode)periodEntry.children[i]).getImage());
		    			period.addElement(tag);
		    		}
	    			if(oTagValue instanceof GIPSYIdentifier)
	    			{
	    				GIPSYIdentifier tag=new GIPSYIdentifier(((SimpleNode)periodEntry.children[i]).getImage());
		    			period.addElement(tag);
		    		}
	    			
	    		}
	    		else
	    		{
	    			System.err.println("Semantic Error: The tag value is not inside the tag set1");
	    			iErrorCount++;
	    		}
	    	}
	    	oTagSet =new UnorderedInfinitePeriodicTagSet(period);
	    	
	    	
	    	if(!oTagSet.isInTagSet(oTagValue))
	    	{
	    		System.err.println("Semantic Error: The tag value is not inside the tag set2");
	    		iErrorCount++;
	    	}
	    
	    	
	    }
	}
	    
	}


