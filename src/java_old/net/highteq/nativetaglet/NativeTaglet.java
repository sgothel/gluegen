package net.highteq.nativetaglet;

import jdk.javadoc.doclet.Taglet;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import com.sun.source.util.SimpleDocTreeVisitor;

import javax.lang.model.element.Element;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.io.InputStream;
import java.net.URL;
import java.net.MalformedURLException;

public class NativeTaglet implements Taglet
{
  private Properties mapping= null;
  private static final String NAME = "native";

  /**
   * Return the name of this custom tag.
   */
  @Override
  public String getName()
  {
    return NAME;
  }

  /**
   * Will return true since this is an inline tag.
   *
   * @return true since this is an inline tag.
   */

  @Override
  public boolean isInlineTag()
  {
    return true;
  }

  /**
   * Register this Taglet.
   *
   * @param tagletMap the map to register this tag to.
   */
  public static void register(final Map tagletMap)
  {
    final NativeTaglet tag = new NativeTaglet();
    final Taglet t = (Taglet) tagletMap.get(tag.getName());
    if (t != null)
      {
        tagletMap.remove(tag.getName());
      }
    tagletMap.put(tag.getName(), tag);
  }

    /**
     * Utility function to visit the tree of tags and return the text.
     * @param tags the list of instances of this tag
     * @return the text of the tags.
     */
    static String getTagsText(DocTree tags) {
        return new SimpleDocTreeVisitor<String, Void>() {
            @Override
            public String visitText(TextTree node, Void unused) {
                return node.getBody();
            }

            @Override
            public String visitUnknownBlockTag(UnknownBlockTagTree node, Void unused) {
                for(DocTree doctree : node.getContent())
                    return doctree.accept(this, unused);

                return "";
            }

            @Override
            public String visitUnknownInlineTag(UnknownInlineTagTree node, Void unused) {
                for(DocTree doctree : node.getContent())
                    return doctree.accept(this, unused);

                return "";
            }

            @Override
            protected String defaultAction(DocTree node, Void unused) {
                return "";
            }

      }.visit(tags, null);
  }

  /**
   * Returns the string representation of a series of instances of
   * this tag to be included in the generated output.
   *
   * @param tags the list of instances of this tag
   * @param element the element to which the enclosing comment belongs
   * @return the string representation of the tags to be included in
   *  the generated output
   */
  @Override
  public String toString(List<? extends DocTree> tags, Element element) {
    String text= getTagsText(tags.get(0)).trim();
    if(mapping== null)
      {
        mapping= new Properties();
        InputStream in= null;
        try
          {
            URL url;
            try
              {
                url = new URL(System.getProperty("nativetaglet.mapping","file:native-taglet.properties"));
              }
            catch (final MalformedURLException e)
              {
                url = new URL("file:"+System.getProperty("nativetaglet.mapping","file:native-taglet.properties"));
              }
            in= url.openStream();
            mapping.load(in);
          }
        catch (final Exception e)
          {
            System.err.println("[NATIVE TAGLET] Could not read mapping file");
            System.err.println("-->");
            e.printStackTrace(System.err);
            System.err.println("<--");
            System.err.println("[NATIVE TAGLET] !!! NO LINKS WILL BE GENERATED !!!");
          }
        finally
          {
            if(in!=null) try{ in.close(); }catch(final Exception ignore){}
          }
      }

    if(mapping!=null)
      {
        // First check to see whether this key exists in the mapping
        String url = mapping.getProperty(text);
        if (url == null) {
          // Try iterating the keySet seeing if we can find a partial match
          // In the OpenGL spec this handles the case of glVertex -> glVertex3f
          for(final Iterator i= mapping.keySet().iterator(); i.hasNext();) {
            final String name= (String) i.next();
            if (hasOpenGLSuffix(text, name)) {
              url = mapping.getProperty(name);
              break;
            }
          }
        }
        if (url != null) {
          url = mapping.getProperty("nativetaglet.baseUrl", "") + url;
          text = "<a href=\"" + url + "\">" + text + "</a>";
        }
      }
    return text;
  }

  private static final String[] openGLSuffixes = {
    "b",
    "s",
    "i",
    "f",
    "d",
    "ub",
    "us",
    "ui",
    "bv",
    "sv",
    "iv",
    "fv",
    "dv",
    "ubv",
    "usv",
    "uiv"
  };
  private static boolean hasOpenGLSuffix(final String name,
                                         final String baseName) {
    if (!name.startsWith(baseName)) {
      return false;
    }
    for (int i = 0; i < openGLSuffixes.length; i++) {
      final String suffix = openGLSuffixes[i];
      if (name.endsWith(suffix)) {
        // First see whether it's a simple concatenation
        if (name.equals(baseName + suffix)) {
          return true;
        }
        // Now chop prefix and suffix off and see whether the
        // resulting is a number
        try {
          final String tmp = name.substring(baseName.length(),
                                      name.length() - suffix.length());
          if (tmp.length() == 1 &&
              Character.isDigit(tmp.charAt(0))) {
            return true;
          }
        } catch (final IndexOutOfBoundsException e) {
        }
      }
    }
    return false;
  }

    @Override
    public Set<Location> getAllowedLocations() {
        Set<Location> locations = new HashSet<Location>();
        locations.add(Location.FIELD);
        locations.add(Location.CONSTRUCTOR);
        locations.add(Location.METHOD);
        locations.add(Location.OVERVIEW);
        locations.add(Location.PACKAGE);
        locations.add(Location.TYPE);
        return locations;
    }
}
