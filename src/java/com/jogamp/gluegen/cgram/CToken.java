package com.jogamp.gluegen.cgram;

public class CToken extends antlr.CommonToken {
  String source = "";
  int tokenNumber;

  public String getSource()
  {
    return source;
  }

  public void setSource(final String src)
  {
    source = src;
  }

  public int getTokenNumber()
  {
    return tokenNumber;
  }

  public void setTokenNumber(final int i)
  {
    tokenNumber = i;
  }

    @Override
    public String toString() {
        return "CToken:" +"(" + hashCode() + ")" + "[" + getType() + "] "+ getText() + " line:" + getLine() + " source:" + source ;
    }
}
