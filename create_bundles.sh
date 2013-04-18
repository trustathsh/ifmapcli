#!/bin/bash

function usage() {
	echo "usage:  $0 -v <version>"
	echo "                            -v : provide version as dotted decimal number, e.g. 0.1.2"
}

# check arguments
if [ $# == 0 ] ; then
	usage
	exit 1
fi

# get version from command line
VERSION="0.0.0"
while getopts v: o
do
	case "$o" in
		v)	VERSION="$OPTARG";;
		[?])	usage
			exit 1;;
	esac
done
#shift $OPTIND-1

# declare variables for later use
PROJECT=ifmapcli
DATE=`date +'%C%y%m%d-%H%M'`
FOLDER=${PROJECT}-${VERSION}
SRCFOLDER=${FOLDER}-src
CUR=`pwd`
RM="rm -rvf"
CP="cp -v"

if [ -d ${FOLDER} ] ; then
	echo "Directory for version $VERSION already exists, remove it manually..."
	exit 1
fi

mkdir ${FOLDER}
cd ${FOLDER}

mkdir ${SRCFOLDER}

# copy source tree
${CP} -R ${CUR}/ar-dev ${SRCFOLDER}
${CP} -R ${CUR}/ar-ip ${SRCFOLDER}
${CP} -R ${CUR}/ar-mac ${SRCFOLDER}
${CP} -R ${CUR}/auth-as ${SRCFOLDER}
${CP} -R ${CUR}/auth-by ${SRCFOLDER}
${CP} -R ${CUR}/cap ${SRCFOLDER}
${CP} -R ${CUR}/common ${SRCFOLDER}
${CP} -R ${CUR}/dev-attr ${SRCFOLDER}
${CP} -R ${CUR}/dev-char ${SRCFOLDER}
${CP} -R ${CUR}/dev-ip ${SRCFOLDER}
${CP} -R ${CUR}/event ${SRCFOLDER}
${CP} -R ${CUR}/feature ${SRCFOLDER}
${CP} -R ${CUR}/feature2 ${SRCFOLDER}
${CP} -R ${CUR}/featureSingle ${SRCFOLDER}
${CP} -R ${CUR}/ifmapcli-distribution ${SRCFOLDER}
${CP} -R ${CUR}/ip-disc-by ${SRCFOLDER}
${CP} -R ${CUR}/ip-mac ${SRCFOLDER}
${CP} -R ${CUR}/layer2-info ${SRCFOLDER}
${CP} -R ${CUR}/mac-disc-by ${SRCFOLDER}
${CP} -R ${CUR}/pdp ${SRCFOLDER}
${CP} -R ${CUR}/perf1 ${SRCFOLDER}
${CP} -R ${CUR}/purge ${SRCFOLDER}
${CP} -R ${CUR}/role ${SRCFOLDER}
${CP} -R ${CUR}/search ${SRCFOLDER}
${CP} -R ${CUR}/src ${SRCFOLDER}
${CP} -R ${CUR}/subscribe ${SRCFOLDER}

${CP} ${CUR}/LICENSE.txt ${SRCFOLDER}
${CP} ${CUR}/README.txt ${SRCFOLDER}
${CP} ${CUR}/pom.xml ${SRCFOLDER}

# remove all .svn folders
find . | grep '\.svn' | xargs ${RM}

zip -r ${SRCFOLDER}.zip ${SRCFOLDER}
