#! /bin/sh

echo $0

if [ -e /opt-share/apache-ant ] ; then
    ANT_PATH=/opt-share/apache-ant
    PATH=$ANT_PATH/bin:$PATH
    export ANT_PATH
fi
if [ -z "$ANT_PATH" ] ; then
    if [ -e /usr/share/ant/bin/ant -a -e /usr/share/ant/lib/ant.jar ] ; then
        ANT_PATH=/usr/share/ant
        export ANT_PATH
        echo autosetting ANT_PATH to $ANT_PATH
    fi
fi
if [ -z "$ANT_PATH" ] ; then
    if [ -e /usr/share/ant/bin/ant -a -e /usr/share/ant/lib/ant.jar ] ; then
        ANT_PATH=/usr/share/ant
        export ANT_PATH
        echo autosetting ANT_PATH to $ANT_PATH
    fi
fi
if [ -z "$ANT_PATH" ] ; then
    echo ANT_PATH does not exist, set it
    exit
fi

if [ -e /opt-linux-x86_64/jre6 -a -e /opt-linux-x86_64/j2se6 ] ; then
    J2RE_HOME=/opt-linux-x86_64/jre6
    JAVA_HOME=/opt-linux-x86_64/j2se6
    PATH=$J2RE_HOME/bin:$JAVA_HOME/bin:$PATH
    export J2RE_HOME JAVA_HOME
fi

if [ -e /opt-solaris-x86_64/jre6 -a -e /opt-solaris-x86_64/j2se6 ] ; then
    # make a symbolic link: /opt-solaris-x86_64/jre6/bin/amd64/bin$ ln -s . bin
    # since ant looks for $JAVA_HOME/bin/java and we need to force the 64bit JVM
    J2RE_HOME=/opt-solaris-x86_64/jre6/bin/amd64
    JAVA_HOME=/opt-solaris-x86_64/j2se6/bin/amd64
    PATH=$J2RE_HOME:$JAVA_HOME:$PATH
    export J2RE_HOME JAVA_HOME
fi

export PATH


