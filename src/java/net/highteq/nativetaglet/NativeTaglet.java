package net.highteq.nativetaglet;

import com.sun.tools.doclets.Taglet;
import com.sun.javadoc.*;

import java.util.Map;
import java.util.Properties;
import java.util.Iterator;
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
   * @return true since this tag can be used in a field
   *         doc comment
   */
  @Override
  public boolean inField()
  {
    return true;
  }

  /**
   * @return true since this tag can be used in a constructor
   *         doc comment
   */
  @Override
  public boolean inConstructor()
  {
    return true;
  }

  /**
   * @return true since this tag can be used in a method
   *         doc comment
   */
  @Override
  public boolean inMethod()
  {
    return true;
  }

  /**
   * @return true since this tag can be used in an overview
   *         doc comment
   */
  @Override
  public boolean inOverview()
  {
    return true;
  }

  /**
   * @return true since this tag can be used in a package
   *         doc comment
   */
  @Override
  public boolean inPackage()
  {
    return true;
  }

  /**
   * @return true since this
   */
  @Override
  public boolean inType()
  {
    return true;
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
   * Given the <code>Tag</code> representation of this custom
   * tag, return its string representation.
   *
   * @param tag the <code>Tag</code> representation of this custom tag.
   */
  @Override
  public String toString(final Tag tag)
  {
    String text= tag.text().trim();
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

  /**
   * This method should not be called since arrays of inline tags do not
   * exist.  Method {@link #tostring(Tag)} should be used to convert this
   * inline tag to a string.
   *
   * @param tags the array of <code>Tag</code>s representing of this custom tag.
   */
  @Override
  public String toString(final Tag[] tags)
  {
    return null;
  }
}
