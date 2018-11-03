#!/usr/bin/env bash
# =========
#
# Orchestrator script for execute all necessary steps to
# convert FRAMENET (XML) data into FRAMESTER (RDF)
# Script valid under *nix systems 
#
# Prerequisites
# -------------
# Java
# Framenet data (https://framenet.icsi.berkeley.edu/fndrupal/framenet_request_data)
# Premonitor (https://github.com/dkmfbk/premon)
# Apache jena fuseki (https://jena.apache.org/)
# UKB (http://ixa2.si.ehu.es/ukb/)
# Frnet2RDF (https://github.com/stlu/Frnet2RDF)
# 
#
# Orchestration
# -------------
# The script frnetMake.sh execude 6 steps to complete all work 
#
# STEP1 - convert the Framenet dataset (XML) in RDF using Premonitor  
#         need PREMON_HOME, PREMON_CONF, PREMON_OUT
# STEP2 - load previous step's result into the Sparql Endpoint 
#         defined in SPARQL_DATASET_PREMON need  FRNET2RDF_HOME, FRNET2DRF_START
# STEP3 - refactorize RDF of previous step 
#         need FRNET2RDF_HOME, REFACT_CONF, REFACT_RULES_DIR, REFACT_OUT FRNET2DRF_START
# STEP4 - load previous step's result into the Sparql Endpoint 
#         defined in SPARQL_DATASET_FRNET need  FRNET2RDF_HOME, FRNET2DRF_START
# STEP5 - analize Framenet's annotations with UKB and make a Word 
#         Sense Disambiguation with Frnet2RDF
#         need  FRNET2RDF_HOME, FRNET2RDF_CONF, FRNET2RDF_OUT, UKB_WSD, WN30_GLOSS, WN30_DICT, UKB_TMP
# STEP6 - load previous step's result into the Sparql Endpoint
#         defined in SPARQL_DATASET_FRNET (configured with java -Xmx4G)
#         need FRNET2RDF_HOME, FRNET2DRF_START
#
# Configuration
# -------------
# Values are loaded from ./defaults, if it exists.
# The description below cover the default settings if ./defaults
# does not exist.
#
# JAVA
#   Command to invoke Java. If not set, java (from the PATH) will be used.
#   JAVA_HOME can also be used to set JAVA.
#
# FRNETMAKE_HOME
#   Directory where is frnetMake script.  If not set, the script will try
#   to guess it based on the script invokation path.
#
# FRNETMAKE_RUN
#   The root of the runtime area where the frnetMake.pid file should be stored. 
#   It defaults first available of /var/run, /usr/var/run, /tmp
#
# FRNETMAKE_PID
#   The FRNETMAKE PID file, defaults to $FRNETMAKE_RUN/frnetMake.pid
#
# FRNETMAKE_LOGS
#   Directory where logs will be generated. 
#   Fixed as $FRNETMAKE_HOME/log
#
# FRNETMAKE_LOGS_STDERROUT
#   Log file with stderr and stdout log output from frnetMake.sh
#   Defaults to $FRNETMAKE_LOGS/frnetMake.log
#
# PREMON_HOME
#   Directory where premonitor is installed.
#
# PREMON_CONF
#   The premonitor configuration file 
#
# PREMON_OUT
#   Directory where premonitor's output will be generated
#
# SPARQL_DATASET_PREMON
#   The http URL of the sparql end point to load premon's data 
#
# SPARQL_DATASET_FRNET
#   The http URL of the sparql end point to load frnet's data 
#
# FRNET2RDF_HOME
#   Where Frnet2RDF is installed.  
#
# FRNET2RDF_START
#   Path to the jar file. Defaults to $FRNET2RDF_HOME/target/Frnet2RDF-xxx.jar
#
# REFACT_CONF
#   The refact configuration file 
#
# REFACT_OUT
#   Directory where refact's output will be generated
#
# REFACT_RULES_DIR
#   Directory containing the rules to be used to refactorize data
#
# FRNET2RDF_CONF
#   The Frnet2RDF configuration file 
#
# FRNET2RDF_OUT
#   Directory where Frnet2RDF's output will be generated
#
#########
#

NAME=defaults
if [ -f ./$NAME ]; then
  . ./$NAME
fi

# logging functions
log_msg() {
    echo $1  
    echo $(date '+%D %X') $1  >>   $FRNETMAKE_LOGS_STDERROUT
}

usage()
{
  echo "Usage: ${0##*/} {step1|step2|step3|step4|step5|step6|runAll|status}"
  exit 1
}

[ $# -gt 0 ] || usage
CMD="$1"

# Utility functions

findDirectory()
{
  local L OP=$1
  shift
  for L in "$@"; do
    [ "$OP" "$L" ] || continue
    printf %s "$L"
    break
  done
}

running()
{
  if [ ! -e "$1" ]
  then
     return 1
  fi
  local PID=$(cat "$1" 2>/dev/null) || return 1
  ps -p "$PID" >/dev/null 2>&1
}


# Are we running in cygwin?
cygwin=false
case "`uname`" in
    CYGWIN*) cygwin=true;;
esac

# Set FRNETMAKE_HOME to the script invocation directory if it is not specified
# Set $1 to the script invocation directory if it is not specified
if [ -z "$FRNETMAKE_HOME" ]  # value of $1 
then
  SCRIPT="$0"
  # Catch common issue: script has been symlinked
  if [ -L "$SCRIPT" ]
  then
    SCRIPT="$(readlink "$0")"
    # If link is relative
    case "$SCRIPT" in
      /*) ;; # fine
      *) SCRIPT=$( dirname "$0" )/$SCRIPT;; # fix
    esac
  fi
  # Work out root from script location
  FRNETMAKE_HOME="$( cd "$( dirname "$SCRIPT" )" && pwd )" 
fi

# Check if FRNET2RDF_HOME exist
if [ ! -e "$FRNET2RDF_HOME" ]
then
  echo "FRNET2RDF_HOME '$FRNET2RDF_HOME' does not exist" 1>&2
  exit 1
fi

# Deal with Cygwin path issues
if [ "$cygwin" == "true" ]
then
  FRNETMAKE_HOME=`cygpath -w "$FRNETMAKE_HOME"`
  FRNET2RDF_HOME=`cygpath -w "$FRNET2RDF_HOME"`
fi

# Set FRNET2RDF_START
if [ -z "$FRNET2RDF_START" ]
then
  FRNET2RDF_START="$FRNET2RDF_HOME/target/Frnet2RDF-0.0.1.jar"
fi

# Set FRNETMAKE_RUN
if [ -z "$FRNETMAKE_RUN" ]
then
  FRNETMAKE_RUN=$(findDirectory -w /var/run /usr/var/run /tmp)
fi

# Get PID file name
if [ -z "$FRNETMAKE_PID" ]
then
  FRNETMAKE_PID="$FRNETMAKE_RUN/frnetMake.pid"
fi

# Log directory
if [ -n "$FRNETMAKE_LOGS" ]
then
    echo "FRNETMAKE_LOGS can not be set externally - ignored" 1>&2
fi
FRNETMAKE_LOGS="$FRNETMAKE_HOME/log"

# Stderr and stdout log
if [ -z "$FRNETMAKE_LOGS_STDERROUT" ]
then
  FRNETMAKE_LOGS_STDERROUT="$FRNETMAKE_LOGS/frnetMake.log"
fi

###
# Set up JAVA if not set
if [ -z "$JAVA" ]
then
    if [ -z "$JAVA_HOME" ]
    then
       JAVA=$(which java)
    else
       JAVA=$JAVA_HOME/bin/java
    fi
fi

if [ -z "$JAVA" ]
then
    (
	echo "Cannot find a Java JDK."
	echo "Please set either set JAVA or JAVA_HOME and put java (>=1.8) in your PATH."
    ) 1>&2
    exit 1
fi

###
# Get SPARQL_DATASET_PREMON
if [ -z "$SPARQL_DATASET_PREMON" ]
then
    (
	echo "Cannot find a Sparl Endpoint. Please create one and configure in your default parameters"
    ) 1>&2
    exit 1
fi

###
# Get SPARQL_DATASET_FRNET
if [ -z "$SPARQL_DATASET_FRNET" ]
then
    (
	echo "Cannot find the Sparl Endpoint. Please create one and configure in your default parameters"
    ) 1>&2
    exit 1
fi

#
step1() {
  if ! running "$FRNETMAKE_PID"
  then
     echo $$ >  "$FRNETMAKE_PID"
  fi

  if [ -e "$PREMON_HOME" ]
  then
    ( echo "Cannot find PREMON_HOME") 1>&2
    exit 1
  fi

  # STEP1 - convert the Framenet dataset (XML) in RDF using Premonitor
  #         need PREMON_HOME, PREMON_CONF, PREMON_OUT
  JAVA_OPT_PREMON="-Xmx2400M -Xms1G -server"
  BASEDIR=`cd "$PREMON_HOME" >/dev/null; pwd`

  # Build classpath.
  _LIB=$BASEDIR/lib
  _CLASSPATH=$RDFPRO_CLASSPATH:$BASEDIR/etc
  for _JAR in `ls $_LIB/*.jar` ; do
        _CLASSPATH=$_CLASSPATH:$_JAR;
  done

  # Substitute env variables into PREMON_CONF
  sed "s|\$PREMON_HOME|$PREMON_HOME|"   $PREMON_CONF > /tmp/_premon.conf
  _OPTIONS="-b $PREMON_OUT -x -f ttl -p /tmp/_premon.conf"

  log_msg "** STEP1 start premonitor" 
  "$JAVA" $JAVA_OPT_PREMON -classpath $_CLASSPATH eu.fbk.dkm.premon.premonitor.Premonitor $_OPTIONS 2>&1 | tee -a $FRNETMAKE_LOGS_STDERROUT

  log_msg "** STEP1 end premonitor" 
}

step2() {
  if ! running "$FRNETMAKE_PID"
  then
     echo $$ >  "$FRNETMAKE_PID"
  fi

  `cd "$FRNET2RDF_HOME" >/dev/null; `
  JAVA_OPT_LOAD="-Xmx4800M -Xms1G"

  # STEP2 - load previous step's result into the Sparql Endpoint 
  #         defined in SPARQL_DATASET_PREMON
  log_msg "** STEP2 start" 
  for _TTL in `ls $PREMON_OUT*.ttl` ; do
    _OPTIONS=" -f $_TTL -s $SPARQL_DATASET_PREMON "
    echo "... load $_TTL into $SPARQL_DATASET_PREMON "
    "$JAVA" $JAVA_OPT_LOAD -cp $FRNET2RDF_START it.unibo.cs.Frnet2RDF.utils.LoadIntoSparq $_OPTIONS 2>&1 | tee -a $FRNETMAKE_LOGS_STDERROUT

    log_msg "   Load into $SPARQL_DATASET_PREMON file $_TTL" 
  done

  log_msg "** STEP2 end" 
}

step3() {
  if ! running "$FRNETMAKE_PID"
  then
     echo $$ >  "$FRNETMAKE_PID"
  fi

  `cd "$FRNET2RDF_HOME" >/dev/null; `
  JAVA_OPT_LOAD="-Xmx2G -Xms1G"

# STEP3 - refactorize RDF of previous step with Refact 
#         need FRNET2RDF_HOME, REFACT_CONF, REFACT_RULES_DIR, REFACT_OUT

  # Substitute env variables into REFACT_CONF
  _OPTIONS="$FRNETMAKE_HOME/tmp/_refact.properties"
  sed -e "s|\$REFACT_RULES_DIR|$REFACT_RULES_DIR|" -e "s|\$REFACT_OUT|$REFACT_OUT|"  -e "s|\$SPARQL_DATASET_PREMON|$SPARQL_DATASET_PREMON|"  $REFACT_CONF > $_OPTIONS

  log_msg "** STEP3 start" 
  "$JAVA" $JAVA_OPT_LOAD -cp $FRNET2RDF_START it.unibo.cs.Frnet2RDF.refact.Refact $_OPTIONS 2>&1 | tee -a $FRNETMAKE_LOGS_STDERROUT
  log_msg "** STEP3 end" 
}

step4() {
  if ! running "$FRNETMAKE_PID"
  then
     echo $$ >  "$FRNETMAKE_PID"
  fi

  `cd "$FRNET2RDF_HOME" >/dev/null; `
  JAVA_OPT_LOAD="-Xmx4800M -Xms1G"

  # STEP4 - load previous step's result into the Sparql Endpoint 
  #         defined in SPARQL_DATASET_FRNET
  log_msg "** STEP4 start" 

  _OPTIONS=" -f $REFACT_OUT -s $SPARQL_DATASET_FRNET "
  echo "... load $REFACT_OUT into $SPARQL_DATASET_PREMON "
  "$JAVA" $JAVA_OPT_LOAD -cp $FRNET2RDF_START it.unibo.cs.Frnet2RDF.utils.LoadIntoSparq $_OPTIONS 2>&1 | tee -a $FRNETMAKE_LOGS_STDERROUT

  log_msg "   Load into $SPARQL_DATASET_FRNET file $REFACT_OUT" 
  log_msg "** STEP4 end" 
}

step5() {
  if ! running "$FRNETMAKE_PID"
  then
     echo $$ >  "$FRNETMAKE_PID"
  fi

  `cd "$FRNET2RDF_HOME" >/dev/null; `
  JAVA_OPT_LOAD="-Xmx6G -Xms1G"

# STEP5 - analize Framenet's annotations with UKB and make a Word 
#         Sense Disambiguation with Frnet2RDF
#         need  FRNET2RDF_HOME, FRNET2RDF_CONF, FRNET2RDF_OUT, UKB_WSD, WN30_GLOSS, WN30_DICT, UKB_TMP

  # Substitute env variables into FRNET2RDF_CONF
  _OPTIONS="$FRNETMAKE_HOME/tmp/_frnet2rdf.properties"
  sed -e "s|\$FRNET2RDF_OUT|$FRNET2RDF_OUT|" -e "s|\$FRNET2RDF_CONF|$FRNET2RDF_CONF|"  -e "s|\$SPARQL_DATASET_PREMON|$SPARQL_DATASET_PREMON|" -e "s|\$UKB_WSD|$UKB_WSD|" -e "s|\$WN30_GLOSS|$WN30_GLOSS|" -e "s|\$WN30_DICT|$WN30_DICT|" -e "s|\$UKB_TMP|$UKB_TMP|" $FRNET2RDF_CONF > $_OPTIONS 

  log_msg "** STEP5 start" 
  "$JAVA" $JAVA_OPT_LOAD  -cp $FRNET2RDF_START it.unibo.cs.Frnet2RDF.UKB $_OPTIONS  2>&1 | tee -a $FRNETMAKE_LOGS_STDERROUT
  log_msg "** STEP5 end" 
}

step6() {
  if ! running "$FRNETMAKE_PID"
  then
     echo $$ >  "$FRNETMAKE_PID"
  fi

  `cd "$FRNET2RDF_HOME" >/dev/null; `
  JAVA_OPT_LOAD="-Xmx4800M -Xms1G"

# STEP6 - load previous step's result into the Sparql Endpoint
#         defined in SPARQL_DATASET_FRNET 
  log_msg "** STEP6 start" 

  _OPTIONS=" -f $FRNET2RDF_OUT -s $SPARQL_DATASET_FRNET "
  echo "... load $FRNET2RDF_OUT into $SPARQL_DATASET_FRNET "
  "$JAVA" $JAVA_OPT_LOAD -cp $FRNET2RDF_START it.unibo.cs.Frnet2RDF.utils.LoadIntoSparq $_OPTIONS 2>&1 | tee -a $FRNETMAKE_LOGS_STDERROUT
  log_msg "   Load into $SPARQL_DATASET_FRNET file $FRNET2RDF_OUT" 

  log_msg "** STEP6 end" 
}

case $CMD in
  step1)
    step1
  ;;
  step2)
    step2
  ;;
  step3)
    step3
  ;;
  step4)
    step4
  ;;
  step5)
    step5
  ;;
  step6)
    step6
  ;;
  runall)
    step1
    step2
    step3
    step4
    step5
    step6
  ;;
  status)
    if running $FRNETMAKE_PID
    then
      PID=`cat "$FRNETMAKE_PID"`
      echo "frnetMake is running with pid: $PID"
    else
      echo "frnetMake is not running"
    fi
  ;;
  *)
    usage
  ;;
esac

exit 0
