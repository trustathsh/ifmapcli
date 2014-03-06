ifmapcl
=======
This package contains a set of *experimental* IF-MAP Command Line Interface
tools for Java. ifmapcli supports [IF-MAP 2.0][1]. Development was done by
[Hochschule Hannover (Hannover University of Applied Sciences and Arts)][2]
within the [ESUKOM][3] research project.


Documentation
=============
A help message is printed if you start one of the CLI commands with wrong or
missing parameters or add `--help` to the call.
The name of each CLI command indicates its purpose (for
example `ar-dev` for publishing `access-request-device` metadata).

ifmapcli comes with a keystore that works out-of-the-box with our irond IF-MAP
server. The password for the keystore is 'ifmapcli'.


Build
=====
Just execute

	$ mvn package

in the top directory of the project in order to create binary JAR files.
You can find all executable jar files in the
`ifmapcli-distribution/target/ifmapcli-distribution-x.x.x-bundle.zip` zip file.


Feedback
========
If you have any questions, problems or comments, please contact
	<trust@f4-i.fh-hannover.de>


LICENSE
=======
ifmapcli is licensed under the [Apache License, Version 2.0][4].



[1]: http://www.trustedcomputinggroup.org/resources/tnc_ifmap_binding_for_soap_specification
[2]: http://trust.f4.hs-hannover.de
[3]: http://www.esukom.de
[4]: http://www.apache.org/licenses/LICENSE-2.0.html
