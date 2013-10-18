package com.jogamp.gluegen.cgram;

import java.util.*;

public class PreprocessorInfoChannel
{
    Hashtable<Integer, Vector<Object>> lineLists = new Hashtable<Integer, Vector<Object>>(); // indexed by Token number
    int firstValidTokenNumber = 0;
    int maxTokenNumber = 0;

    public void addLineForTokenNumber( Object line, Integer toknum )
    {
        if ( lineLists.containsKey( toknum ) ) {
            Vector<Object> lines = lineLists.get( toknum );
            lines.addElement(line);
        }
        else {
            Vector<Object> lines = new Vector<Object>();
            lines.addElement(line);
            lineLists.put(toknum, lines);
            if ( maxTokenNumber < toknum.intValue() ) {
                maxTokenNumber = toknum.intValue();
            }
        }
    }

    public int getMaxTokenNumber()
    {
        return maxTokenNumber;
    }

    public Vector<Object> extractLinesPrecedingTokenNumber( Integer toknum )
    {
        Vector<Object> lines = new Vector<Object>();
        if (toknum == null) return lines;
        for (int i = firstValidTokenNumber; i < toknum.intValue(); i++){
            Integer inti = new Integer(i);
            if ( lineLists.containsKey( inti ) ) {
                Vector<Object> tokenLineVector = lineLists.get( inti );
                if ( tokenLineVector != null) {
                    Enumeration<Object> tokenLines = tokenLineVector.elements();
                    while ( tokenLines.hasMoreElements() ) {
                        lines.addElement( tokenLines.nextElement() );
                    }
                    lineLists.remove(inti);
                }
            }
        }
        firstValidTokenNumber = toknum.intValue();
        return lines;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("PreprocessorInfoChannel:\n");
        for (int i = 0; i <= maxTokenNumber + 1; i++){
            Integer inti = new Integer(i);
            if ( lineLists.containsKey( inti ) ) {
                Vector<Object> tokenLineVector = lineLists.get( inti );
                if ( tokenLineVector != null) {
                    Enumeration<Object> tokenLines = tokenLineVector.elements();
                    while ( tokenLines.hasMoreElements() ) {
                        sb.append(inti + ":" + tokenLines.nextElement() + '\n');
                    }
                }
            }
        }
        return sb.toString();
    }
}



