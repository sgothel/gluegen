package com.jogamp.gluegen.ant;

/*
 * Copyright (C) 2003 Rob Grzywinski (rgrzywinski@realityinteractive.com)
 * Copyright (c) 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.AbstractFileSet;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.DirSet;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * <p>An <a href="http://ant.apache.org">ANT</a> {@link org.apache.tools.ant.Task}
 * for using {@link com.jogamp.gluegen.GlueGen}.</p>
 *
 * <p>Usage:</p>
 * <pre>
    &lt;gluegen src="[source C file]"
                outputrootdir="[optional output root dir]"
                includes="[optional directory pattern of include files to include]"
                excludes="[optional directory pattern of include files to exclude]"
                includeRefid="[optional FileSet or DirSet for include files]"
                literalInclude="[optional comma separated list of literal include directories, avoiding limitations of FileSet / DirSet issues]"
                emitter="[emitter class name]"
                config="[configuration file]"
                dumpCPP="[optional boolean]"
                debug="[optional boolean]"
                logLevel="[optional string]" /&gt;
 * </pre>
 *
 * @author Rob Grzywinski <a href="mailto:rgrzywinski@realityinteractive.com">rgrzywinski@yahoo.com</a>
 */
// FIXME:  blow out javadoc
// NOTE:  this has not been exhaustively tested
public class GlueGenTask extends Task
{
    /**
     * <p>The {@link com.jogamp.gluegen.GlueGen} classname.</p>
     */
    private static final String GLUE_GEN = "com.jogamp.gluegen.GlueGen";

    // =========================================================================
    /**
     * <p>The {@link org.apache.tools.ant.types.CommandlineJava} that is used
     * to execute {@link com.jogamp.gluegen.GlueGen}.</p>
     */
    private final CommandlineJava gluegenCommandline;

    // =========================================================================
    /**
     * <p>The optional debug flag.</p>
     */
    private boolean debug=false;

    /**
     * <p>The optional logLevel.</p>
     */
    private String logLevel = null;

    /**
     * <p>The optional dumpCPP flag.</p>
     */
    private boolean dumpCPP=false;

    /**
     * <p>The optional output root dir.</p>
     */
    private String outputRootDir;

    /**
     * <p>The name of the emitter class.</p>
     */
    private String emitter;

    /**
     * <p>The configuration file name.</p>
     */
    private String configuration;

    /**
     * <p>The name of the source C file that is to be parsed.</p>
     */
    private String sourceFile;

    /**
     * <p>The {@link org.apache.tools.ant.types.FileSet} of includes.</p>
     */
    private final FileSet includeSet = new FileSet();

    /**
     * <p>Because a {@link org.apache.tools.ant.types.FileSet} will include
     * everything in its base directory if it is left untouched, the <code>includeSet</code>
     * must only be added to the set of includes if it has been <i>explicitly</i>
     * set.</p>
     */
    private boolean usedIncludeSet = false; // by default it is not used

    /**
     * <p>The set of include sets.  This allows includes to be added in multiple
     * fashions.</p>
     */
    // FIXME:  rename to listXXXX
    private final List<AbstractFileSet> setOfIncludeSets = new LinkedList<AbstractFileSet>();

    /**
     * <p>Comma separated list of literal directories to include.  This is to get around the
     * fact that neither {@link org.apache.tools.ant.types.FileSet} nor
     * {@link org.apache.tools.ant.types.DirSet} can handle multiple drives in
     * a sane manner or deal with relative path outside of the base-dir.
     * If <code>null</code> then it has not been specified.</p>
     */
    private String literalIncludes;

    // =========================================================================
    /**
     * <p>Create and add the VM and classname to {@link org.apache.tools.ant.types.CommandlineJava}.</p>
     */
    public GlueGenTask()
    {
        // create the CommandlineJava that will be used to call GlueGen
        gluegenCommandline = new CommandlineJava();

        // set the VM and classname in the commandline
        gluegenCommandline.setVm(JavaEnvUtils.getJreExecutable("java"));
        gluegenCommandline.setClassname(GLUE_GEN);
        // gluegenCommandline.createVmArgument().setValue("-verbose:class");
    }

    // =========================================================================
    // ANT getters and setters

    /**
     * <p>Set the debug flag (optional).  This is called by ANT.</p>
     */
    public void setDebug(final boolean debug)
    {
        log( ("Setting debug flag: " + debug), Project.MSG_VERBOSE);
        this.debug=debug;
    }

    /**
     * <p>Set the logLevel (optional).  This is called by ANT.</p>
     */
    public void setLogLevel(final String logLevel)
    {
        log( ("Setting logLevel: " + logLevel), Project.MSG_VERBOSE);
        this.logLevel=logLevel;
    }

    /**
     * <p>Set the dumpCPP flag (optional).  This is called by ANT.</p>
     */
    public void setDumpCPP(final boolean dumpCPP)
    {
        log( ("Setting dumpCPP flag: " + dumpCPP), Project.MSG_VERBOSE);
        this.dumpCPP=dumpCPP;
    }

    /**
     * <p>Set the output root dir (optional).  This is called by ANT.</p>
     *
     * @param  outputRootDir the optional output root dir
     */
    public void setOutputRootDir(final String outputRootDir)
    {
        log( ("Setting output root dir: " + outputRootDir), Project.MSG_VERBOSE);
        this.outputRootDir=outputRootDir;
    }

    /**
     * <p>Set the emitter class name.  This is called by ANT.</p>
     *
     * @param  emitter the name of the emitter class
     */
    public void setEmitter(final String emitter)
    {
        log( ("Setting emitter class name to: " + emitter), Project.MSG_VERBOSE);
        this.emitter = emitter;
    }

    /**
     * <p>Set the configuration file name.  This is called by ANT.</p>
     *
     * @param  configuration the name of the configuration file
     */
    public void setConfig(final String configuration)
    {
        log( ("Setting configuration file name to: " + configuration),
              Project.MSG_VERBOSE);
        this.configuration = configuration;
    }

    /**
     * <p>Set the source C file that is to be parsed.  This is called by ANT.</p>
     *
     * @param  sourceFile the name of the source file
     */
    public void setSrc(final String sourceFile)
    {
        log( ("Setting source file name to: " + sourceFile), Project.MSG_VERBOSE);
        this.sourceFile = sourceFile;
    }

    /**
     * <p>Set a literal include directories, separated with a comma.  See the <code>literalInclude</code>
     * javadoc for more information.</p>
     *
     * @param  commaSeparatedIncludes the comma separated directories to include
     */
    public void setLiteralInclude(final String commaSeparatedIncludes)
    {
        this.literalIncludes = commaSeparatedIncludes.trim();
    }

    /**
     * <p>Add an include file to the list.  This is called by ANT for a nested
     * element.</p>
     *
     * @return {@link org.apache.tools.ant.types.PatternSet.NameEntry}
     */
    public PatternSet.NameEntry createInclude()
    {
        usedIncludeSet = true;
        return includeSet.createInclude();
    }

    /**
     * <p>Add an include file to the list.  This is called by ANT for a nested
     * element.</p>
     *
     * @return {@link org.apache.tools.ant.types.PatternSet.NameEntry}
     */
    public PatternSet.NameEntry createIncludesFile()
    {
        usedIncludeSet = true;
        return includeSet.createIncludesFile();
    }

    /**
     * <p>Set the set of include patterns.  Patterns may be separated by a comma
     * or a space.  This is called by ANT.</p>
     *
     * @param  includes the string containing the include patterns
     */
    public void setIncludes(final String includes)
    {
        usedIncludeSet = true;
        includeSet.setIncludes(includes);
    }

    /**
     * <p>Add an include file to the list that is to be exluded.  This is called
     * by ANT for a nested element.</p>
     *
     * @return {@link org.apache.tools.ant.types.PatternSet.NameEntry}
     */
    public PatternSet.NameEntry createExclude()
    {
        usedIncludeSet = true;
        return includeSet.createExclude();
    }

    /**
     * <p>Add an exclude file to the list.  This is called by ANT for a nested
     * element.</p>
     *
     * @return {@link org.apache.tools.ant.types.PatternSet.NameEntry}
     */
    public PatternSet.NameEntry createExcludesFile()
    {
        usedIncludeSet = true;
        return includeSet.createExcludesFile();
    }

    /**
     * <p>Set the set of exclude patterns.  Patterns may be separated by a comma
     * or a space.  This is called by ANT.</p>
     *
     * @param  includes the string containing the exclude patterns
     */
    public void setExcludes(final String excludes)
    {
        usedIncludeSet = true;
        includeSet.setExcludes(excludes);
    }

    /**
     * <p>Set a {@link org.apache.tools.ant.types.Reference} to simplify adding
     * of complex sets of files to include.  This is called by ANT.</p>?
     *
     * @param  reference a <code>Reference</code> to a {@link org.apache.tools.ant.types.FileSet}
     *         or {@link org.apache.tools.ant.types.DirSet}
     * @throws BuildException if the specified <code>Reference</code> is not
     *         either a <code>FileSet</code> or <code>DirSet</code>
     */
public void setIncludeRefid(final Reference reference) {
	// ensure that the referenced object is either a FileSet or DirSet
	final Object referencedObject = reference.getReferencedObject(getProject());
	if (referencedObject instanceof FileSet) {
		setOfIncludeSets.add((FileSet)referencedObject);
		return;
	}
	if (referencedObject instanceof DirSet) {
		setOfIncludeSets.add((DirSet)referencedObject);
		return;
	}

	throw new BuildException("Only FileSets or DirSets are allowed as an include refid.");
}

    /**
     * <p>Add a nested {@link org.apache.tools.ant.types.DirSet} to specify
     * the files to include.  This is called by ANT.</p>
     *
     * @param  dirset the <code>DirSet</code> to be added
     */
    public void addDirset(final DirSet dirset)
    {
        setOfIncludeSets.add(dirset);
    }

    /**
     * <p>Add an optional classpath that defines the location of {@link com.jogamp.gluegen.GlueGen}
     * and <code>GlueGen</code>'s dependencies.</p>
     *
     * @returns {@link org.apache.tools.ant.types.Path}
     */
     public Path createClasspath()
     {
         return gluegenCommandline.createClasspath(project).createPath();
     }

    // =========================================================================
    /**
     * <p>Run the task.  This involves validating the set attributes, creating
     * the command line to be executed and finally executing the command.</p>
     *
     * @see  org.apache.tools.ant.Task#execute()
     */
    @Override
    public void execute()
        throws BuildException
    {
        // validate that all of the required attributes have been set
        validateAttributes();

        // TODO:  add logic to determine if the generated file needs to be
        //        regenerated

        // add the attributes to the CommandlineJava
        addAttributes();

        log(gluegenCommandline.describeCommand(), Project.MSG_VERBOSE);

        // execute the command and throw on error
        final int error = execute(gluegenCommandline.getCommandline());
        if(error == 1)
            throw new BuildException( ("GlueGen returned: " + error), location);
    }

    /**
     * <p>Ensure that the user specified all required arguments.</p>
     *
     * @throws BuildException if there are required arguments that are not
     *         present or not valid
     */
    private void validateAttributes()
        throws BuildException
    {
        // outputRootDir is optional ..

        // validate that the emitter class is set
        if(!isValid(emitter))
            throw new BuildException("Invalid emitter class name: " + emitter);

        // validate that the configuration file is set
        if(!isValid(configuration))
            throw new BuildException("Invalid configuration file name: " + configuration);

        // validate that the source file is set
        if(!isValid(sourceFile))
            throw new BuildException("Invalid source file name: " + sourceFile);

        // CHECK:  do there need to be includes to be valid?
    }

    /**
     * <p>Is the specified string valid?  A valid string is non-<code>null</code>
     * and has a non-zero length.</p>
     *
     * @param  string the string to be tested for validity
     * @return <code>true</code> if the string is valid.  <code>false</code>
     *         otherwise.
     */
    private boolean isValid(final String string)
    {
        // check for null
        if(string == null)
            return false;

        // ensure that the string has a non-zero length
        // NOTE:  must trim() to remove leading and trailing whitespace
        if(string.trim().length() < 1)
            return false;

        // the string is valid
        return true;
    }

    /**
     * <p>Add all of the attributes to the command line.  They have already
     * been validated.</p>
     */
    private void addAttributes()
        throws BuildException
    {
        // NOTE:  GlueGen uses concatenated flag / value rather than two
        //        separate arguments

        // add the debug flag if enabled
        if(debug) {
            gluegenCommandline.createArgument().setValue("--debug");
        }

        // add the logLevel if enabled
        if(null != logLevel) {
            gluegenCommandline.createArgument().setValue("--logLevel");
            gluegenCommandline.createArgument().setValue(logLevel);
        }

        // add the debug flag if enabled
        if(dumpCPP) {
            gluegenCommandline.createArgument().setValue("--dumpCPP");
        }

        // add the output root dir
        if(null!=outputRootDir && outputRootDir.trim().length()>0) {
            gluegenCommandline.createArgument().setValue("-O" + outputRootDir);
        }

        // add the emitter class name
        gluegenCommandline.createArgument().setValue("-E" + emitter);

        // add the configuration file name
        gluegenCommandline.createArgument().setValue("-C" + configuration);

        // add the includedSet to the setOfIncludeSets to simplify processing
        // all types of include sets ONLY if it has been set.
        // NOTE:  see the usedIncludeSet member javadoc for more info
        // NOTE:  references and nested DirSets have already been added to the
        //        set of include sets
        if(usedIncludeSet)
        {
            includeSet.setDir(getProject().getBaseDir()); // NOTE:  the base dir must be set
            setOfIncludeSets.add(includeSet);
        }

        // iterate over all include sets and add their directories to the
        // list of included directories.
        final List<String> includedDirectories = new LinkedList<String>();
        for (final Iterator<AbstractFileSet> includes = setOfIncludeSets.iterator(); includes.hasNext();)
        {
            // get the included set and based on its type add the directories
            // to includedDirectories
        	final AbstractFileSet include = includes.next();
        	final DirectoryScanner directoryScanner = include.getDirectoryScanner(getProject());
        	final String[] directoryDirs = directoryScanner.getIncludedDirectories();

            // add the directoryDirs to the includedDirectories
            // TODO:  exclude any directory that is already in the list
            for(int i=0; i<directoryDirs.length; i++)
            {
            	includedDirectories.add(directoryDirs[i]);
            }
        }

        // if literalInclude is valid then add it to the list of included
        // directories
        if( isValid( literalIncludes ) ) {
            final String[] includes = literalIncludes.split(",");
            for(int i=0; i<includes.length; i++) {
                final String include = includes[i].trim();
                if( include.length()>0 ) {
                    includedDirectories.add(include);
                }
            }
        }

        // add the included directories to the command
        for(final Iterator<String> includes=includedDirectories.iterator(); includes.hasNext(); )
        {
        	final String directory = includes.next();
            gluegenCommandline.createArgument().setValue("-I" + directory);
        }

        // finally, add the source file
        gluegenCommandline.createArgument().setValue(sourceFile);
    }

    /**
     * <p>Execute {@link com.jogamp.gluegen.GlueGen} in a forked JVM.</p>
     *
     * @throws BuildException
     */
    private int execute(final String[] command)
        throws BuildException
    {
        // create the object that will perform the command execution
        final Execute execute = new Execute(new LogStreamHandler(this, Project.MSG_INFO,
                                                           Project.MSG_WARN),
                                      null);

        // set the project and command line
        execute.setAntRun(project);
        execute.setCommandline(command);
        execute.setWorkingDirectory( project.getBaseDir() );

        // execute the command
        try
        {
            return execute.execute();
        } catch(final IOException ioe)
        {
            throw new BuildException(ioe, location);
        }
    }
}
