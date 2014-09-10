/**
 * Copyright 2013 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.common.net;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Helper class to process URI's query, handled as properties.
 * <p>
 * The order of the URI segments (any properties) are <i>not</i> preserved.
 * </p>
 * <pre>
 *  URI: [scheme:][//authority][path][?query][#fragment]
 *  w/ authority: [user-info@]host[:port]
 *  Note: 'path' starts w/ fwd slash
 * </pre>
 * <p>
 * Since 2.3.0 renamed from {@code URIQueryProps} to {@code UriQueryProps},
 * and using {@link Uri} instead of {@link java.net.URI}.
 * </p>
 */
public class UriQueryProps {
   private static final String QMARK = "?";
   private static final char ASSIG = '=';
   private static final String EMPTY = "";
   private final String query_separator;

   private final HashMap<String, String> properties = new HashMap<String, String>();

   private UriQueryProps(final char querySeparator) {
       query_separator = String.valueOf(querySeparator);
   }

   public final Map<String, String> getProperties() { return properties; }
   public final char getQuerySeparator() { return query_separator.charAt(0); }

   public final Uri.Encoded appendQuery(Uri.Encoded baseQuery) {
       boolean needsSep = false;
       final StringBuilder sb = new StringBuilder();
       if ( null != baseQuery ) {
           if( baseQuery.startsWith(QMARK) ) {
               baseQuery = baseQuery.substring(1); // cut off '?'
           }
           sb.append(baseQuery.get());
           if( !baseQuery.endsWith(query_separator) ) {
               needsSep = true;
           }
       }
       final Iterator<Entry<String, String>> entries = properties.entrySet().iterator();
       while(entries.hasNext()) {
           if(needsSep) {
               sb.append(query_separator);
           }
           final Entry<String, String> entry = entries.next();
           sb.append(entry.getKey());
           if( EMPTY != entry.getValue() ) {
               sb.append(ASSIG).append(entry.getValue());
           }
           needsSep = true;
       }
       return new Uri.Encoded(sb.toString(), Uri.QUERY_LEGAL);
   }

   public final Uri appendQuery(final Uri base) throws URISyntaxException {
       return base.getNewQuery( appendQuery( base.query ) );
   }

   /**
    *
    * @param uri
    * @param querySeparator should be either <i>;</i> or <i>&</i>, <i>;</i> is encouraged due to troubles of escaping <i>&</i>.
    * @return
    * @throws IllegalArgumentException if <code>querySeparator</code> is illegal, i.e. neither <i>;</i> nor <i>&</i>
    */
   public static final UriQueryProps create(final Uri uri, final char querySeparator) throws IllegalArgumentException {
       if( ';' != querySeparator && '&' != querySeparator ) {
           throw new IllegalArgumentException("querySeparator is invalid: "+querySeparator);
       }
       final UriQueryProps data = new UriQueryProps(querySeparator);
       final String q = Uri.decode(uri.query);
       final int q_l = null != q ? q.length() : -1;
       int q_e = -1;
       while(q_e < q_l) {
           final int q_b = q_e + 1; // next term
           q_e = q.indexOf(querySeparator, q_b);
           if(0 == q_e) {
               // single separator
               continue;
           }
           if(0 > q_e) {
               // end
               q_e = q_l;
           }
           // n-part
           final String part = q.substring(q_b, q_e);
           final int assignment = part.indexOf(ASSIG);
           if(0 < assignment) {
               // assignment
               final String k = part.substring(0, assignment);
               final String v = part.substring(assignment+1);
               data.properties.put(k, v);
           } else {
               // property key only
               data.properties.put(part, EMPTY);
           }
       }
       return data;
   }
}
