package com.jogamp.gluegen.cgram;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;


public class CSymbolTable {

  /** holds list of scopes */
  private final Vector<String> scopeStack;

  /** table where all defined names are mapped to TNode tree nodes */
  private final Hashtable<String, TNode> symTable;

  public CSymbolTable()  {
    scopeStack = new Vector<String>(10);
    symTable = new Hashtable<String, TNode>(533);
  }


  /** push a new scope onto the scope stack.
    */
  public void pushScope(final String s) {
      //System.out.println("push scope:" + s);
    scopeStack.addElement(s);
  }

  /** pop the last scope off the scope stack.
    */
  public void popScope() {
      //System.out.println("pop scope");
    final int size = scopeStack.size();
    if(size > 0)
      scopeStack.removeElementAt(size - 1);
  }

  /** return the current scope as a string
   */
  public String currentScopeAsString() {
      final StringBuilder buf = new StringBuilder(100);
      boolean first = true;
      final Enumeration<String> e = scopeStack.elements();
      while(e.hasMoreElements()) {
        if(first)
          first = false;
        else
          buf.append("::");
        buf.append(e.nextElement().toString());
      }
      return buf.toString();
  }

  /** given a name for a type, append it with the
    current scope.
    */
  public String addCurrentScopeToName(final String name) {
    final String currScope = currentScopeAsString();
    return addScopeToName(currScope, name);
  }

  /** given a name for a type, append it with the
    given scope.  MBZ
    */
  public String addScopeToName(final String scope, final String name) {
    if(scope == null || scope.length() > 0)
      return scope + "::" + name;
    else
      return name;
  }

  /** remove one level of scope from name MBZ*/
  public String removeOneLevelScope(final String scopeName) {
    final int index = scopeName.lastIndexOf("::");
    if (index > 0) {
      return scopeName.substring(0,index);
    }
    if (scopeName.length() > 0) {
        return "";
    }
    return null;
  }

  /** add a node to the table with it's key as
    the current scope and the name */
  public TNode add(final String name, final TNode node) {
    return symTable.put(addCurrentScopeToName(name),node);
  }


  /** lookup a fully scoped name in the symbol table */
  public TNode lookupScopedName(final String scopedName) {
    return symTable.get(scopedName);
  }

  /** lookup an unscoped name in the table by prepending
    the current scope.
    MBZ -- if not found, pop scopes and look again
    */
  public TNode lookupNameInCurrentScope(final String name) {
    String scope = currentScopeAsString();
    String scopedName;
    TNode tnode = null;

    //System.out.println( "\n"+ this.toString() );

    while (tnode == null && scope != null) {
      scopedName = addScopeToName(scope, name);
      //System.out.println("lookup trying " + scopedName);
      tnode = symTable.get(scopedName);
      scope = removeOneLevelScope(scope);
    }
    return tnode;
  }

  /** convert this table to a string */
  @Override
  public String toString() {
    final StringBuilder buff = new StringBuilder(300);
    buff.append("CSymbolTable { \nCurrentScope: " + currentScopeAsString() +
                "\nDefinedSymbols:\n");
    final Enumeration<String> ke = symTable.keys();
    final Enumeration<TNode> ve = symTable.elements();
    while(ke.hasMoreElements()) {
      buff.append(ke.nextElement().toString());
      buff.append(" (").append(TNode.getNameForType(ve.nextElement().getType())).append(")\n");
    }
    buff.append("}\n");
    return buff.toString();
  }

}
