#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import os
import xml.etree.ElementTree as et


NS = "http://maven.apache.org/POM/4.0.0"
POM_NS = "{http://maven.apache.org/POM/4.0.0}"


def getModuleNames(mainPom):
    pom = et.parse(mainPom)
    modules = pom.findall("./{ns}modules/{ns}module".format(ns=POM_NS))
    return map(lambda element: element.text, modules)


def updateVersionInModule(module, newVersion):
    pomPath = os.path.join(module, "pom.xml")
    modulePom = et.parse(pomPath)
    parentVersion = modulePom.find("./{ns}parent/{ns}version".format(ns=POM_NS))
    parentVersion.text = newVersion
    modulePom.write(pomPath, xml_declaration=False, encoding="utf-8", method="xml")


if __name__ == '__main__':
    et.register_namespace('', NS)

    parser = argparse.ArgumentParser(description='Update parent version in all submodules.')
    parser.add_argument('version', help='the new parent version')
    args = parser.parse_args()

    allModules = getModuleNames("pom.xml")
    for module in allModules:
        updateVersionInModule(module, args.version)