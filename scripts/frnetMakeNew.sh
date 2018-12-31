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
# The script frnetMakeNew.sh execude steps to complete all work 
#
# S_PREMON - convert the Framenet dataset (XML) in RDF using Premonitor  
#         need PREMON_HOME, PREMON_CONF, PREMON_OUT
# S_LOADER - load files *.ttl find in directory DIR_TTL into the Sparql Endpoint
#         defined in SPARQL_DATASET need  FRNET2RDF_HOME
# S_REFACT - refactorize RDF found into SPARQL_DATASET 
#         need FRNET2RDF_HOME, REFACT_CONF, REFACT_RULES_DIR, REFACT_OUT 
# S_UKB - analize Framenet's annotations with UKB and make a Word 
#         Sense Disambiguation with Frnet2RDF
#         need  FRNET2RDF_HOME, FRNET2RDF_CONF, FRNET2RDF_OUT, UKB_WSD, WN30_GLOSS, WN30_DICT, UKB_TMP
#
# Configuration Parameters (description)
# --------------------------------------
#
# JAVA
#   Command to invoke Java. If not set, java (from the PATH) will be used.
#   JAVA_HOME can also be used to set JAVA.
#
# FRNETMAKE_HOME
#   Directory where is frnetMakeNew script.  If not set, the script will try
#   to guess it based on the script invokation path.
#
# FRNETMAKE_RUN
#   The root of the runtime area where the frnetMakeNew.pid file should be stored. 
#   It defaults first available of /var/run, /usr/var/run, /tmp
#
# FRNETMAKE_PID
#   The FRNETMAKE PID file, defaults to $FRNETMAKE_RUN/frnetMakeNew.pid
#
# FRNETMAKE_LOGS
#   Directory where logs will be generated. 
#   Fixed as $FRNETMAKE_HOME/log
#
# FRNETMAKE_LOGS_STDERROUT
#   Log file with stderr and stdout log output from frnetMakeNew.sh
#   Defaults to $FRNETMAKE_LOGS/frnetMakeNew.log
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
# SPARQL_DATASET
#   The http URL of the sparql end point to load ttl data 
#
# FRNET2RDF_HOME
#   Where Frnet2RDF is installed.  
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


#Bad arguments, something has gone wrong with command.
if [ $? -ne 0 ];
then
  exit 1
fi

[ $# -gt 0 ] || usage
CMD="$1"

usage()
{
  echo "Usage: ${0##*/} {premon|S_LOADER|S_REFACT|S_UKB|status}"
  echo "Options:"
  echo "  -h, --help       Display this help message"
  echo "  premon               Execute Premon conversion"
  exit 1
}

if [ $# -lt 1 ]; then
  usage
  exit 1
fi
if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
  usage
  exit 0
fi


# logging functions
log_msg() {
    echo $1  
    echo $(date '+%D %X') $1  >>   $FRNETMAKE_LOGS_STDERROUT
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

setHome()
{
  f_H=$1

  # Check if f_H  exist
  if [ ! -e "$f_H" ]
  then
    echo "FRNET2RDF_HOME '$f_H' does not exist" 1>&2
    exit 1
  fi
  
  # Deal with Cygwin path issues
  if [ "$cygwin" == "true" ]
  then
    f_H=`cygpath -w "$f_H"`
  fi

  `cd "$f_H" >/dev/null; `

  # Set FRNET2RDF_START
  if [ -z "$FRNET2RDF_START" ]
  then
    FRNET2RDF_START="$f_H/target/Frnet2RDF-0.0.1.jar"
  fi
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

# Deal with Cygwin path issues
if [ "$cygwin" == "true" ]
then
  FRNETMAKE_HOME=`cygpath -w "$FRNETMAKE_HOME"`
fi

# Set FRNETMAKE_RUN
if [ -z "$FRNETMAKE_RUN" ]
then
  FRNETMAKE_RUN=$(findDirectory -w /var/run /usr/var/run /tmp)
fi

# Get PID file name
if [ -z "$FRNETMAKE_PID" ]
then
  FRNETMAKE_PID="$FRNETMAKE_RUN/frnetMakeNew.pid"
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
  FRNETMAKE_LOGS_STDERROUT="$FRNETMAKE_LOGS/frnetMakeNew.log"
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

#
function S_PREMON() {
  # S_PREMON - convert the Framenet dataset (XML) in RDF using Premonitor
  #         need PREMON_HOME, PREMON_CONF, PREMON_OUT
  p_HOME=$1 
  p_CONF=$2 
  p_OUT=$3 

  if ! running "$FRNETMAKE_PID"
  then
     echo $$ >  "$FRNETMAKE_PID"
  fi

  if [ ! -d "$p_HOME" ]
  then
    ( echo "Cannot find PREMON_HOME") 1>&2
    exit 1
  fi

  JAVA_OPT_PREMON="-Xmx2400M -Xms1G -server"
  BASEDIR=`cd "$p_HOME" >/dev/null; pwd`

  # Build classpath.
  _LIB=$BASEDIR/lib
  _CLASSPATH=$RDFPRO_CLASSPATH:$BASEDIR/etc
  for _JAR in `ls $_LIB/*.jar` ; do
        _CLASSPATH=$_CLASSPATH:$_JAR;
  done

  # Substitute env variables into PREMON_CONF
  sed "s|\$PREMON_HOME|$p_HOME|"   $p_CONF > /tmp/_premon.conf
  _OPTIONS="-b $p_OUT -x -f ttl -p /tmp/_premon.conf"

  log_msg "** S_PREMON start premonitor" 
  "$JAVA" $JAVA_OPT_PREMON -classpath $_CLASSPATH eu.fbk.dkm.premon.premonitor.Premonitor $_OPTIONS 2>&1 | tee -a $FRNETMAKE_LOGS_STDERROUT

  log_msg "** S_PREMON end premonitor" 
}

S_LOADER() {
  # S_LOADER - load files *.ttl find in directory DIR_TTL into the Sparql Endpoint
  #         defined in SPARQL_DATASET need FRNET2RDF_HOME
  SPARQL_DATASET=$1 
  DIR_TTL=$2 
  f_HOME=$3 

  if ! running "$FRNETMAKE_PID"
  then
     echo $$ > "$FRNETMAKE_PID"
  fi

  # Set f_HOME
  setHome $f_HOME
  JAVA_OPT_LOAD="-Xmx4800M -Xms1G"

  log_msg "** S_LOADER start"
  for _TTL in `ls $DIR_TTL*.ttl` ; do
    _OPTIONS=" -f $_TTL -s $SPARQL_DATASET "
    echo "... load $_TTL into $SPARQL_DATASET "
    "$JAVA" $JAVA_OPT_LOAD -cp $FRNET2RDF_START it.unibo.cs.Frnet2RDF.utils.LoadIntoSparq $_OPTIONS 2>&1 | tee -a $FRNETMAKE_LOGS_STDERROUT

    log_msg "   Load into $SPARQL_DATASET file $_TTL"
  done

  log_msg "** S_LOADER end"
}

S_REFACT() {
# S_REFACT - refactorize RDF with Refact RDF found into SPARQL_DATASET
#         need FRNET2RDF_HOME, REFACT_CONF, REFACT_RULES_DIR, REFACT_OUT, 
  REFACT_CONF=$1 
  REFACT_RULES_DIR=$2 
  REFACT_OUT=$3 
  SPARQL_DATASET=$4
  f_HOME=$5 

  if ! running "$FRNETMAKE_PID"
  then
     echo $$ >  "$FRNETMAKE_PID"
  fi

  # Set f_HOME
  setHome $f_HOME
  JAVA_OPT_LOAD="-Xmx2G -Xms1G"

  # Substitute env variables into REFACT_CONF
  _OPTIONS="$FRNETMAKE_HOME/tmp/_refact.properties"
  sed -e "s|\$REFACT_RULES_DIR|$REFACT_RULES_DIR|" -e "s|\$REFACT_OUT|$REFACT_OUT|"  -e "s|\$SPARQL_DATASET_PREMON|$SPARQL_DATASET|"  $REFACT_CONF > $_OPTIONS

  log_msg "** S_REFACT start" 
  "$JAVA" $JAVA_OPT_LOAD -cp $FRNET2RDF_START it.unibo.cs.Frnet2RDF.refact.Refact $_OPTIONS 2>&1 | tee -a $FRNETMAKE_LOGS_STDERROUT
  log_msg "** S_REFACT end" 
}

S_UKB() {
  # S_UKB - analize Framenet's annotations with UKB and make a Word 
  #         Sense Disambiguation with Frnet2RDF
  #         need  FRNET2RDF_HOME, FRNET2RDF_CONF, FRNET2RDF_OUT, UKB_WSD, WN30_GLOSS, WN30_DICT, UKB_TMP
  FRNET2RDF_CONF=$1 
  FRNET2RDF_OUT=$2 
  UKB_WSD=$3 
  WN30_GLOSS=$4 
  WN30_DICT=$5 
  UKB_TMP=$6 
  SPARQL_DATASET=$7 
  f_HOME=$8 

  if ! running "$FRNETMAKE_PID"
  then
     echo $$ >  "$FRNETMAKE_PID"
  fi

  # Set f_HOME
  setHome $f_HOME
  JAVA_OPT_LOAD="-Xmx8G -Xms1G"

  # Substitute env variables into FRNET2RDF_CONF
  _OPTIONS="$FRNETMAKE_HOME/tmp/_frnet2rdf.properties"
  sed -e "s|\$FRNET2RDF_OUT|$FRNET2RDF_OUT|" -e "s|\$FRNET2RDF_CONF|$FRNET2RDF_CONF|"  -e "s|\$SPARQL_DATASET_PREMON|$SPARQL_DATASET|" -e "s|\$UKB_WSD|$UKB_WSD|" -e "s|\$WN30_GLOSS|$WN30_GLOSS|" -e "s|\$WN30_DICT|$WN30_DICT|" -e "s|\$UKB_TMP|$UKB_TMP|" $FRNET2RDF_CONF > $_OPTIONS 

  log_msg "** S_UKB start" 
  "$JAVA" $JAVA_OPT_LOAD  -cp $FRNET2RDF_START it.unibo.cs.Frnet2RDF.UKB $_OPTIONS  2>&1 | tee -a $FRNETMAKE_LOGS_STDERROUT
  log_msg "** S_UKB end" 
}

#
# Check command
#
case $CMD in
  premon)
    S_PREMON $2 $3 $4
  ;;
  loader)
    S_LOADER $2 $3 $4 $4
  ;;
  refact)
    S_REFACT $2 $3 $4 $5 $6
  ;;
  ukb)
    S_UKB $2 $3 $4 $5 $6 $7 $8 $9
  ;;
  status)
    if running $FRNETMAKE_PID
    then
      PID=`cat "$FRNETMAKE_PID"`
      echo "frnetMakeNew is running with pid: $PID"
    else
      echo "frnetMakeNew is not running"
    fi
  ;;
  *)
    usage
  ;;
esac

exit 0
