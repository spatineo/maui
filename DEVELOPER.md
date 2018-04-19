# Developer notes

Prerequisites:
 * You are registered at Sonatype OSS Repository hosting
 * Your maven settings.xml has a server with id "ossrh" with your XXX credentials
 * You have a GPG key that is registered with the main key servers
 * Your maven settings.xml has a profiled with id "ossrh" with your GPG key and passphrase
 * Use maven 3.2.1+

For more in-depth information, follow the guides: http://central.sonatype.org/pages/ossrh-guide.html and http://central.sonatype.org/pages/apache-maven.html

## Deploy snapshot to Sonatype

```shell
mvn clean deploy
```

## Deploy a release

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


