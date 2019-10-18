# Developer notes

Prerequisites:
 * You are registered at Sonatype OSS Repository hosting
 * Your maven settings.xml has a server with id "ossrh" with your XXX credentials
 * You have a GPG key that is registered with the main key servers
 * Your maven settings.xml has a profiled with id "ossrh" with your GPG key and passphrase
 * Use maven 3.2.1+

For more in-depth information, follow the guides: http://central.sonatype.org/pages/ossrh-guide.html and http://central.sonatype.org/pages/apache-maven.html

## Testing and voikko

This version of Maui uses the [voikko](https://voikko.puimula.org) library for stemming Finnish. The actual stemmer is a binary library that needs to be installed in the OS for the FinnishStemmer to work. 

```shell
sudo apt install libvoikko1
```

If you need to compile this module without the voikko library, you will run into problems with tests not completing. You can run maven using the no-voikko profile to complete the build without running the tests that use the FinnishStemmer,

```shell
mvn clean install -Pno-voikko
```  

## Deploy snapshot to Sonatype

```shell
mvn clean deploy
```

## Deploy a release

This has been tested to work with Maven 3.5.2

```shell
mvn clean
mvn release:prepare
mvn release:perform
```

If the release:perform phase fails for some reason, run the following commands before attempting the next release. Note, these instructions are untested and it is possible that release:rollback removes the tags.

```shell
mvn release:rollback
git tag -d maui-[version]
git push origin :maui-[version]
```


