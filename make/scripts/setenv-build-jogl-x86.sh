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
    echo ANT_PATH does not exist, set it
    exit
fi

if [ ! -z "$J2RE_HOME" -a ! -z "$JAVA_HOME" ] ; then
    if [ -e $J2RE_HOME -a -e $JAVA_HOME ] ; then
        PATH=$J2RE_HOME/bin:$JAVA_HOME/bin:$PATH
        export J2RE_HOME JAVA_HOME
        FOUND_JAVA=1
    fi 
fi 

if [ -z "$FOUND_JAVA" ] ; then
    if [ -e /opt-linux-x86/jre8 -a -e /opt-linux-x86/j2se8 ] ; then
        J2RE_HOME=/opt-linux-x86/jre8
        JAVA_HOME=/opt-linux-x86/j2se8
        PATH=$J2RE_HOME/bin:$JAVA_HOME/bin:$PATH
        export J2RE_HOME JAVA_HOME
        FOUND_JAVA=1
    fi 
fi 

if [ -z "$FOUND_JAVA" ] ; then
    if [ -e /opt-linux-x86/jre7 -a -e /opt-linux-x86/j2se7 ] ; then
        J2RE_HOME=/opt-linux-x86/jre7
        JAVA_HOME=/opt-linux-x86/j2se7
        PATH=$J2RE_HOME/bin:$JAVA_HOME/bin:$PATH
        export J2RE_HOME JAVA_HOME
        FOUND_JAVA=1
    fi 
fi 

if [ -z "$FOUND_JAVA" ] ; then
    if [ -e /opt-linux-x86/jre6 -a -e /opt-linux-x86/j2se6 ] ; then
        J2RE_HOME=/opt-linux-x86/jre6
        JAVA_HOME=/opt-linux-x86/j2se6
        PATH=$J2RE_HOME/bin:$JAVA_HOME/bin:$PATH
        export J2RE_HOME JAVA_HOME
        FOUND_JAVA=1
    fi 
fi 

if [ -z "$FOUND_JAVA" ] ; then
    if [ -e /usr/java/jre/bin -a -e /usr/java/bin ] ; then
        # make a symbolic link: /usr/java/bin/amd64/bin$ ln -s . bin
        # since ant looks for $JAVA_HOME/bin/java and we need to force the 64bit JVM
        J2RE_HOME=/usr/java/jre
        JAVA_HOME=/usr/java
        PATH=$J2RE_HOME/bin:$JAVA_HOME/bin:$PATH
        export J2RE_HOME JAVA_HOME
        FOUND_JAVA=1
    fi
fi
if [ -z "$FOUND_JAVA" ] ; then
    if [ -e /opt-solaris-x86/jre7 -a -e /opt-solaris-x86/j2se7 ] ; then
        J2RE_HOME=/opt-solaris-x86/jre7
        JAVA_HOME=/opt-solaris-x86/j2se7
        PATH=$J2RE_HOME/bin:$JAVA_HOME/bin:$PATH
        export J2RE_HOME JAVA_HOME
        FOUND_JAVA=1
    fi
fi

if [ -z "$FOUND_JAVA" ] ; then
    if [ -e /opt-solaris-x86/jre6 -a -e /opt-solaris-x86/j2se6 ] ; then
        J2RE_HOME=/opt-solaris-x86/jre6
        JAVA_HOME=/opt-solaris-x86/j2se6
        PATH=$J2RE_HOME/bin:$JAVA_HOME/bin:$PATH
        export J2RE_HOME JAVA_HOME
        FOUND_JAVA=1
    fi
fi

if [ -z "$FOUND_JAVA" ] ; then
    # make a symbolic link, e.g. OpenJDK:
    # /usr/lib/jvm/java-7-openjdk-i386 -> /usr/lib/jvm/java-i386
    if [ -e /usr/lib/jvm/java-i386 ] ; then
        J2RE_HOME=/usr/lib/jvm/java-i386/jre
        JAVA_HOME=/usr/lib/jvm/java-i386
        PATH=$J2RE_HOME/bin:$JAVA_HOME/bin:$PATH
        export J2RE_HOME JAVA_HOME
        FOUND_JAVA=1
    fi 
fi 

export PATH

echo FOUND_JAVA $FOUND_JAVA
echo J2RE_HOME $J2RE_HOME
echo JAVA_HOME $JAVA_HOME
echo PATH $PATH
which java
java -version

