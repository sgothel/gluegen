package com.jogamp.gluegen.cgram;

import antlr.ASTFactory;
import antlr.collections.AST;

/** This class extends ASTFactory to build instances
 of class TNode */
public class TNodeFactory extends ASTFactory {

  /** Create a new ampty AST node */
  @Override
  public AST create() {
    return new TNode();
  }

        /** Create a new AST node from type and text */
        @Override
        public AST create(final int ttype, final String text) {
                final AST ast = new TNode();
                ast.setType(ttype);
                ast.setText(text);
                return ast;
        }

        /** Create a new AST node from an existing AST node */
        @Override
        public AST create(final AST ast) {
                final AST newast = new TNode();
                newast.setType(ast.getType());
                newast.setText(ast.getText());
                return newast;
        }


}
