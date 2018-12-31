#!/usr/bin/env bash
#
# This script call every step to complete the conversion from premon to ukb 
#
# STEP1 premon parameters: 
#
#                          PREMON_HOME              PREMON_CONF                                                PREMON_OUT
echo premon step1
./frnetMakeNew.sh premon /xxx/premon/target/premon /xxx/Frnet2RDF/scripts/conf/premon_framenet17.properties /xxx/out/premon
#
# STEP2  loader  parameters:
#
#                          SPARQL_DATASET                DIR_FROM_WHERE_LOAD_TTL      FRNET2RDF_HOME
echo loader step2
./frnetMakeNew.sh loader  http://localhost:3030/premon  /xxx/premon-output/           /xxx/Frnet2RDF
#
# STEP3 refact parameters:
#
#                          REFACT_CONF                                          REFACT_RULES_DIR                         REFACT_OUT                             SPARQL_DATASET                  FRNET2RDF_HOME
echo refact  step3
./frnetMakeNew.sh refact  /xxx/Frnet2RDF/scripts/conf/refact-config.properties  /xxx/Frnet2RDF/scripts/conf/refact_rules /xxx/refactoringRulesOUT/step3_out.ttl http://localhost:3030/premon   /xxx/Frnet2RDF
#
# STEP4 loader parameters:
#                         SPARQL_DATASET               DIR_FROM_WHERE_LOAD_TTL   FRNET2RDF_HOME
echo loader step4
./frnetMakeNew.sh loader  http://localhost:3030/frnet /xxx/refactoringRulesOUT/  /xxx/Frnet2RDF
#
# STEP5 ukb parameters:
#
#                        FRNET2RDF_CONF                                         FRNET2RDF_OUT                   UKB_WSD              WN30_GLOSS                               WN30_DICT                               UKB_TMP             SPARQL_DATASET                FRNET2RDF_HOME
echo ukb step5
./frnetMakeNew.sh  ukb  /xxx/Frnet2RDF/scripts/conf/frnet2rdf-config.properties /xxx/out/step5-ukb-wsd_out.ttl /xxx/ukb/src/ukb_wsd /xxx/ukb/lkb_sources/30/wn30-gloss.bin   /xxx/ukb/lkb_sources/30/wnet30_dict.txt  /xxx/out/tempUkb.txt http://localhost:3030/premon /xxx/Frnet2RDF
#
# STEP6 loader parameters:
#                         SPARQL_DATASET               DIR_FROM_WHERE_LOAD_TTL   FRNET2RDF_HOME
echo loader step6
./frnetMakeNew.sh loader  http://localhost:3030/frnet /xxx/out/                /xxx/Frnet2RDF
