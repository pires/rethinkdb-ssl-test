# rethinkdb-ssl-test

### Requisites

We need to build RethinkDB Java driver with [PR #5284](https://github.com/rethinkdb/rethinkdb/pull/5284) code.

To do so, first you need to clone my repository:
```
git clone https://github.com/pires/rethinkdb
cd rethinkdb
git checkout -b java-ssl
git branch --track -u origin/java-ssl
git pull --rebase
cd drivers/java
```

Now, the current `build.gradle` won't build due to some missing configuration for publishing artifacts. To make it work, you need to change it to the following:
```
apply plugin: 'java'
apply plugin: 'maven'
apply plugin: 'maven-publish'
apply plugin: 'ivy-publish'

tasks.withType(JavaCompile) {
    options.compilerArgs << "-parameters"
}

version = '2.2-beta-2-SNAPSHOT'
ext.isReleaseVersion = !version.endsWith("-SNAPSHOT")
group = "com.rethinkdb"
archivesBaseName = "rethinkdb-driver"

sourceCompatibility = 1.8
targetCompatibility = 1.8

//create a single Jar with all dependencies baked in
task fatJar(type: Jar) {
    archiveName = "rethinkdb-driver-"+version+".jar"
    from { configurations.compile.collect { it.isDirectory() ? it : zipTree(it) } }
    with jar
}

task sourcesJar(type: Jar) {
    classifier = 'sources'
    from sourceSets.main.allSource
}

task javadocJar(type: Jar) {
    classifier = 'javadoc'
    from javadoc
}

repositories {
    mavenCentral()
}

artifacts {
    archives sourcesJar, fatJar, javadocJar
}

dependencies {
    testCompile "junit:junit:4.12"
    compile 'org.slf4j:slf4j-api:1.7.12'
    compile group: 'com.googlecode.json-simple', name: 'json-simple', version: '1.1.1'
}

test {
    testLogging {
        events 'started', 'passed'
    }
}

buildDir = '../../build/drivers/java/gradle'


repositories {
    ivy {
        url "${System.properties['user.home']}/.ivy2/local"
        layout 'pattern', {
            artifact "[organisation]/[module]/jars/[artifact](-[classifier])-[revision](.[ext])"
            ivy "[organisation]/[module]/[artifact](-[classifier])-[revision](.[ext])"
            }
    }
}
publishing {
    publications {
        ivyJava(IvyPublication) {
            from components.java
        }
    }
    repositories {
      add project.repositories.ivy
    }
}

```

and run:
```
gradle install
```

### Test

#### Unsecure

Let's say you have your RethinkDB server running on `localhost`:
```
mvn exec:java -Dargs "localhost 28015"
```

#### Secure

Let's say you're using a [Compose.io RethinkDB](https://www.compose.io/rethinkdb/) cluster.

First, make sure you have saved your cluster Public SSL Key to a file, e.g. `cacert`. Now, you need to create a keystore:
```
keytool -genkey -keystore keystore.jks -keyalg RSA
keytool -keystore keystore.jks -importcert -alias rethinkdb -file cacert
mkdir src/main/resources && mv keystore.ks src/main/resources/
```

When prompted for a passphrase type `password`. If you're going with your own passphrase, make sure you change the code in this repository accordingly.


Now, run:
```
mvn exec:java -Dargs "my-compose-io-proxy.dblayer.com 12345 my-auth-key-xxx keystore.jks"
```

Obviously, you need to adapt the previous command accordingly.
