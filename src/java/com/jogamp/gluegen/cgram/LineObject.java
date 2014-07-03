package com.jogamp.gluegen.cgram;

class LineObject {
  LineObject parent = null;
  String source = "";
  int line = 1;
  boolean enteringFile = false;
  boolean returningToFile = false;
  boolean systemHeader = false;
  boolean treatAsC = false;

  public LineObject()
  {
    super();
  }

  public LineObject( final LineObject lobj )
  {
    parent = lobj.getParent();
    source = lobj.getSource();
    line = lobj.getLine();
    enteringFile = lobj.getEnteringFile();
    returningToFile = lobj.getReturningToFile();
    systemHeader = lobj.getSystemHeader();
    treatAsC = lobj.getTreatAsC();
  }

  public LineObject( final String src)
  {
    source = src;
  }

  public void setSource(final String src)
  {
    source = src;
  }

  public String getSource()
  {
    return source;
  }

  public void setParent(final LineObject par)
  {
    parent = par;
  }

  public LineObject getParent()
  {
    return parent;
  }

  public void setLine(final int l)
  {
    line = l;
  }

  public int getLine()
  {
    return line;
  }

  public void newline()
  {
    line++;
  }

  public void setEnteringFile(final boolean v)
  {
    enteringFile = v;
  }

  public boolean getEnteringFile()
  {
    return enteringFile;
  }

  public void setReturningToFile(final boolean v)
  {
    returningToFile = v;
  }

  public boolean getReturningToFile()
  {
    return returningToFile;
  }

  public void setSystemHeader(final boolean v)
  {
    systemHeader = v;
  }

  public boolean getSystemHeader()
  {
    return systemHeader;
  }

  public void setTreatAsC(final boolean v)
  {
    treatAsC = v;
  }

  public boolean getTreatAsC()
  {
    return treatAsC;
  }

  @Override
  public String toString() {
    StringBuilder ret;
    ret = new StringBuilder("# " + line + " \"" + source + "\"");
    if (enteringFile) {
        ret.append(" 1");
    }
    if (returningToFile) {
        ret.append(" 2");
    }
    if (systemHeader) {
        ret.append(" 3");
    }
    if (treatAsC) {
        ret.append(" 4");
    }
    return ret.toString();
  }
}

