# aem-utils
Maven repo with some useful utilities for AEM
=======
```xml
<plugin>
    <groupId>com.github.kele23.aem-utils</groupId>
    <artifactId>aem-utils</artifactId>
    <extensions>true</extensions>
    <version>0.1</version>
    <configuration>
        <components>${basedir}/src/main/content/jcr_root/apps/ktest/components</components><!--components folder-->
        <i18n>${basedir}/src/main/content/jcr_root/apps/ktest/i18n</i18n><!--i18n folder-->
        <overwriteKey>false</overwriteKey><!--Overwrites key if is already defined-->
        <overwriteFile>false</overwriteFile><!--Overwrites current i18n files-->
        <withMessage>true</withMessage> <!--Sets Message for non default languages-->
        <languages>
            <param>en</param> <!--Default Language (code language)-->
            <param>it</param>
            <param>fr</param>
            <param>ch</param>
        </languages>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>generate-i18n</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
